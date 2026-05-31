package com.example.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.data.api.InvestecApiClient
import com.example.data.local.BankDatabase
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BankRepository(private val context: Context) {

    private val database = BankDatabase.getDatabase(context)
    private val accountDao = database.bankAccountDao()
    private val transactionDao = database.transactionDao()

    private val sharedPrefs = context.getSharedPreferences("InvestecPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "BankRepository"
        
        // Sandbox keys from Investec OpenAPI Documentation
        const val DEFAULT_SANDBOX_CLIENT_ID = "yAxzQRFX97vOcyQAwluEU6H6ePxMA5eY"
        const val DEFAULT_SANDBOX_SECRET = "4dY0PjEYqoBrZ99r"
        const val DEFAULT_SANDBOX_API_KEY = "eUF4elFSRlg5N3ZPY3lRQXdsdUVVNkg2ZVB4TUE1ZVk6YVc1MlpYTjBaV010ZW1FdGNHSXRZV05qYjNWdWRITXRjMkZ1WkdKdmVBPT0="
        
        const val BASE_URL_SANDBOX = "https://openapisandbox.investec.com"
        const val BASE_URL_PRODUCTION = "https://openapi.investec.com"
    }

    // ==========================================
    // CONFIGURATION ACCESSORS
    // ==========================================

    fun useSandbox(): Boolean {
        return sharedPrefs.getBoolean("use_sandbox", true)
    }

    fun setUseSandbox(use: Boolean) {
        sharedPrefs.edit().putBoolean("use_sandbox", use).apply()
    }

    fun getClientId(): String {
        return sharedPrefs.getString("client_id", "") ?: ""
    }

    fun setClientId(clientId: String) {
        sharedPrefs.edit().putString("client_id", clientId).apply()
    }

    fun getClientSecret(): String {
        return sharedPrefs.getString("client_secret", "") ?: ""
    }

    fun setClientSecret(secret: String) {
        sharedPrefs.edit().putString("client_secret", secret).apply()
    }

    fun getApiKey(): String {
        return sharedPrefs.getString("api_key", "") ?: ""
    }

    fun setApiKey(apiKey: String) {
        sharedPrefs.edit().putString("api_key", apiKey).apply()
    }

    // Resolves current active credentials based on configuration
    fun getActiveCredentials(): Triple<String, String, String> {
        return if (useSandbox()) {
            val cid = getClientId().ifEmpty { DEFAULT_SANDBOX_CLIENT_ID }
            val sec = getClientSecret().ifEmpty { DEFAULT_SANDBOX_SECRET }
            val key = getApiKey().ifEmpty { DEFAULT_SANDBOX_API_KEY }
            Triple(cid, sec, key)
        } else {
            Triple(getClientId(), getClientSecret(), getApiKey())
        }
    }

    fun getActiveBaseUrl(): String {
        return if (useSandbox()) BASE_URL_SANDBOX else BASE_URL_PRODUCTION
    }

    // ==========================================
    // DATABASE EXPOSURES
    // ==========================================

    fun getAccountsFlow(): Flow<List<BankAccountEntity>> {
        return accountDao.getAccountsFlow()
    }

    fun getAccountByIdFlow(accountId: String): Flow<BankAccountEntity?> {
        return accountDao.getAccountByIdFlow(accountId)
    }

    fun getLastFiveTransactionsFlow(accountId: String): Flow<List<TransactionEntity>> {
        return transactionDao.getLastFiveTransactionsFlow(accountId)
    }

    suspend fun getAccountById(accountId: String): BankAccountEntity? = withContext(Dispatchers.IO) {
        accountDao.getAccountById(accountId)
    }

    suspend fun getLastFiveTransactions(accountId: String): List<TransactionEntity> = withContext(Dispatchers.IO) {
        transactionDao.getLastFiveTransactions(accountId)
    }

    // ==========================================
    // SYNCHRONIZATION FROM RECONCILE ACTIONS
    // ==========================================

    suspend fun syncData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val (clientId, secret, apiKey) = getActiveCredentials()
            if (clientId.isBlank() || secret.isBlank() || apiKey.isBlank()) {
                return@withContext Result.failure(Exception("Incomplete credentials. Please update settings."))
            }

            val baseUrl = getActiveBaseUrl()
            val service = InvestecApiClient.getService(baseUrl)

            // Step 1: Exchange Oauth2 token
            Log.d(TAG, "Requesting token from codebase: $baseUrl")
            val authBytes = "$clientId:$secret".toByteArray(Charsets.UTF_8)
            val base64Auth = "Basic " + Base64.encodeToString(authBytes, Base64.NO_WRAP)
            
            val tokenResponse = service.getAccessToken(
                basicAuthHeader = base64Auth,
                apiKey = apiKey
            )
            val bearerToken = "Bearer ${tokenResponse.accessToken}"

            // Step 2: Fetch Accounts list
            Log.d(TAG, "Fetching cash accounts...")
            val accountsResult = service.getAccounts(bearerToken)
            val apiAccounts = accountsResult.data.accounts

            val cachedAccounts = mutableListOf<BankAccountEntity>()

            // Step 3: For each account, retrieve modern balance and transactions
            for (apiAcc in apiAccounts) {
                Log.d(TAG, "Syncing metadata for Account ID: ${apiAcc.accountId}")
                
                // Fetch Balance
                val balanceResult = try {
                    service.getAccountBalance(bearerToken, apiAcc.accountId).data
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get balance for ${apiAcc.accountId}: ${e.message}")
                    null
                }

                val currentBalance = balanceResult?.currentBalance ?: 0.0
                val availableBalance = balanceResult?.availableBalance ?: 0.0
                val currency = balanceResult?.currency ?: "ZAR"

                // Create cache entity
                val dbAccount = BankAccountEntity(
                    accountId = apiAcc.accountId,
                    accountNumber = apiAcc.accountNumber,
                    accountName = apiAcc.accountName,
                    referenceName = apiAcc.referenceName,
                    productName = apiAcc.productName,
                    kycCompliant = apiAcc.kycCompliant,
                    profileId = apiAcc.profileId,
                    profileName = apiAcc.profileName,
                    currentBalance = currentBalance,
                    availableBalance = availableBalance,
                    currency = currency,
                    lastUpdated = System.currentTimeMillis()
                )
                cachedAccounts.add(dbAccount)

                // Fetch & insert transactions
                Log.d(TAG, "Fetching list of transactions for associated profile id")
                try {
                    val txResult = service.getAccountTransactions(bearerToken, apiAcc.accountId, includePending = true).data
                    val apiTxs = txResult.transactions

                    val dbTxs = apiTxs.map { apiTx ->
                        TransactionEntity(
                            accountId = apiAcc.accountId,
                            type = apiTx.type,
                            transactionType = apiTx.transactionType ?: "Transfer",
                            status = apiTx.status,
                            description = apiTx.description,
                            amount = apiTx.amount,
                            runningBalance = apiTx.runningBalance ?: 0.0,
                            postingDate = apiTx.postingDate,
                            transactionDate = apiTx.transactionDate,
                            uuid = apiTx.uuid
                        )
                    }
                    transactionDao.replaceTransactionsForAccount(apiAcc.accountId, dbTxs)
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching transactions: ${e.message}")
                }
            }

            if (cachedAccounts.isNotEmpty()) {
                accountDao.replaceAccounts(cachedAccounts)
            }

            // Trigger widget update broadcast since local data changed
            triggerWidgetUpdate(context)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync operation failed", e)
            Result.failure(e)
        }
    }

    private fun triggerWidgetUpdate(context: Context) {
        val intent = android.content.Intent("android.appwidget.action.APPWIDGET_UPDATE")
        intent.component = android.content.ComponentName(
            context,
            "com.example.receiver.BankWidgetProvider"
        )
        context.sendBroadcast(intent)
    }
}
