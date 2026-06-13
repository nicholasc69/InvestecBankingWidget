package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.data.api.InvestecApiClient
import com.example.data.api.InvestecApiService
import com.example.data.local.BankAccountDao
import com.example.data.local.TransactionDao
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.ApiBeneficiary
import com.example.data.model.PaymentResponse
import com.example.data.model.PaymentRequest
import com.example.data.model.PaymentItem
import com.example.data.model.TransferResponse
import com.example.data.model.TransferRequest
import com.example.data.model.TransferItem
import com.example.data.model.ApiCard
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val accountDao: BankAccountDao,
    private val transactionDao: TransactionDao,
    private val dataStore: DataStore<Preferences>,
    private val encryptedPrefs: SharedPreferences
) {

    companion object {
        private const val TAG = "BankRepository"

        // Sandbox keys from Investec OpenAPI Documentation
        const val DEFAULT_SANDBOX_CLIENT_ID = "yAxzQRFX97vOcyQAwluEU6H6ePxMA5eY"
        const val DEFAULT_SANDBOX_SECRET = "4dY0PjEYqoBrZ99r"
        const val DEFAULT_SANDBOX_API_KEY =
            "eUF4elFSRlg5N3ZPY3lRQXdsdUVVNkg2ZVB4TUE1ZVk6YVc1MlpYTjBaV010ZW1FdGNHSXRZV05qYjNWdWRITXRjMkZ1WkdKdmVBPT0="

        const val BASE_URL_SANDBOX = "https://openapisandbox.investec.com"
        const val BASE_URL_PRODUCTION = "https://openapi.investec.com"

        private val USE_SANDBOX = booleanPreferencesKey("use_sandbox")
    }

    // ==========================================
    // CONFIGURATION ACCESSORS
    // ==========================================

    fun useSandboxFlow(): Flow<Boolean> = dataStore.data.map { it[USE_SANDBOX] ?: true }

    suspend fun useSandbox(): Boolean = useSandboxFlow().first()

    suspend fun setUseSandbox(use: Boolean) {
        dataStore.edit { it[USE_SANDBOX] = use }
    }

    fun getClientId(): String = encryptedPrefs.getString("client_id", "") ?: ""

    fun setClientId(clientId: String) {
        encryptedPrefs.edit().putString("client_id", clientId).apply()
    }

    fun getClientSecret(): String = encryptedPrefs.getString("client_secret", "") ?: ""

    fun setClientSecret(secret: String) {
        encryptedPrefs.edit().putString("client_secret", secret).apply()
    }

    fun getApiKey(): String = encryptedPrefs.getString("api_key", "") ?: ""

    fun setApiKey(apiKey: String) {
        encryptedPrefs.edit().putString("api_key", apiKey).apply()
    }

    fun getSelectedProfileId(): String? = encryptedPrefs.getString("selected_profile_id", null)

    fun setSelectedProfileId(profileId: String?) {
        encryptedPrefs.edit().putString("selected_profile_id", profileId).apply()
    }

    // Resolves current active credentials based on configuration
    suspend fun getActiveCredentials(): Triple<String, String, String> {
        return if (useSandbox()) {
            val cid = getClientId().ifEmpty { DEFAULT_SANDBOX_CLIENT_ID }
            val sec = getClientSecret().ifEmpty { DEFAULT_SANDBOX_SECRET }
            val key = getApiKey().ifEmpty { DEFAULT_SANDBOX_API_KEY }
            Triple(cid, sec, key)
        } else {
            Triple(getClientId(), getClientSecret(), getApiKey())
        }
    }

    suspend fun getActiveBaseUrl(): String {
        return if (useSandbox()) BASE_URL_SANDBOX else BASE_URL_PRODUCTION
    }

    // ==========================================
    // DATABASE EXPOSURES
    // ==========================================

    fun getAccountsFlow(): Flow<List<BankAccountEntity>> = accountDao.getAccountsFlow()

    suspend fun getAccounts(): List<BankAccountEntity> = accountDao.getAccounts()

    suspend fun getLastFiveTransactions(accountId: String): List<TransactionEntity> =
        transactionDao.getLastFiveTransactions(accountId)

    suspend fun getLastFiveTransactionsForAccounts(accountIds: List<String>): List<TransactionEntity> =
        transactionDao.getLastFiveTransactionsForAccounts(accountIds)

    suspend fun getAllTransactions(accountId: String): List<TransactionEntity> =
        transactionDao.getAllTransactions(accountId)

    suspend fun getAllTransactionsForAccounts(accountIds: List<String>): List<TransactionEntity> =
        transactionDao.getAllTransactionsForAccounts(accountIds)

    fun getLastFiveTransactionsFlow(accountId: String): Flow<List<TransactionEntity>> =
        transactionDao.getLastFiveTransactionsFlow(accountId)

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
            val dynamicService = InvestecApiClient.getService(baseUrl)

            // Step 1: Exchange Oauth2 token
            Log.d(TAG, "Requesting token from codebase: $baseUrl")
            val authBytes = "$clientId:$secret".toByteArray(Charsets.UTF_8)
            val base64Auth = "Basic " + Base64.encodeToString(authBytes, Base64.NO_WRAP)

            val tokenResponse = dynamicService.getAccessToken(
                basicAuthHeader = base64Auth,
                apiKey = apiKey
            )
            val bearerToken = "Bearer ${tokenResponse.accessToken}"

            // Step 2: Fetch Accounts list
            Log.d(TAG, "Fetching cash accounts...")
            val accountsResult = dynamicService.getAccounts(bearerToken, apiKey)
            val apiAccounts = accountsResult.data.accounts

            // Step 3: Parallel Synchronization
            val cachedAccounts = apiAccounts.map { apiAcc ->
                async {
                    Log.d(TAG, "Syncing metadata for Account ID: ${apiAcc.accountId}")

                    // Fetch Balance
                    val balanceResult = try {
                        dynamicService.getAccountBalance(bearerToken, apiKey, apiAcc.accountId).data
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to get balance for ${apiAcc.accountId}: ${e.message}")
                        null
                    }

                    val currentBalance = balanceResult?.currentBalance ?: 0.0
                    val availableBalance = balanceResult?.availableBalance ?: 0.0
                    val currency = balanceResult?.currency ?: "ZAR"

                    // Fetch transactions in parallel (from 3 months ago to today)
                    try {
                        val toDateStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        val fromDateStr = java.time.LocalDate.now().minusMonths(3).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)

                        val txResult = dynamicService.getAccountTransactions(
                            bearerToken = bearerToken,
                            apiKey = apiKey,
                            accountId = apiAcc.accountId,
                            fromDate = fromDateStr,
                            toDate = toDateStr,
                            includePending = true
                        ).data
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
                        Log.e(
                            TAG,
                            "Error fetching transactions for ${apiAcc.accountId}: ${e.message}"
                        )
                    }

                    BankAccountEntity(
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
                }
            }.awaitAll()

            if (cachedAccounts.isNotEmpty()) {
                accountDao.replaceAccounts(cachedAccounts)
            }

            // Trigger widget update broadcast
            triggerWidgetUpdate(context)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync operation failed", e)
            Result.failure(e)
        }
    }

    private fun triggerWidgetUpdate(context: Context) {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val componentName =
            android.content.ComponentName(context, "com.example.receiver.BankWidgetProvider")
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isNotEmpty()) {
            val intent =
                android.content.Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .apply {
                        component = componentName
                        putExtra(
                            android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS,
                            appWidgetIds
                        )
                    }
            context.sendBroadcast(intent)
        }
    }

    private suspend fun getBearerTokenAndService(): Pair<String, InvestecApiService> {
        val (clientId, secret, apiKey) = getActiveCredentials()
        if (clientId.isBlank() || secret.isBlank() || apiKey.isBlank()) {
            throw Exception("Incomplete credentials. Please update settings.")
        }
        val baseUrl = getActiveBaseUrl()
        val service = InvestecApiClient.getService(baseUrl)

        val authBytes = "$clientId:$secret".toByteArray(Charsets.UTF_8)
        val base64Auth = "Basic " + Base64.encodeToString(authBytes, Base64.NO_WRAP)

        val tokenResponse = service.getAccessToken(
            basicAuthHeader = base64Auth,
            apiKey = apiKey
        )
        return Pair("Bearer ${tokenResponse.accessToken}", service)
    }

    suspend fun getBeneficiaries(): List<ApiBeneficiary> = withContext(Dispatchers.IO) {
        val (bearerToken, service) = getBearerTokenAndService()
        val (_, _, apiKey) = getActiveCredentials()
        val result = service.getBeneficiaries(bearerToken, apiKey)
        result.data.beneficiaries
    }

    suspend fun payBeneficiary(
        accountId: String,
        beneficiaryId: String,
        amount: Double,
        reference: String
    ): PaymentResponse = withContext(Dispatchers.IO) {
        val (bearerToken, service) = getBearerTokenAndService()
        val (_, _, apiKey) = getActiveCredentials()
        val request = PaymentRequest(
            paymentList = listOf(
                PaymentItem(
                    beneficiaryId = beneficiaryId,
                    amount = amount.toString(),
                    myReference = reference,
                    theirReference = reference
                )
            )
        )
        val result = service.payBeneficiary(bearerToken, apiKey, accountId, request)
        result.data
    }

    suspend fun transferFunds(
        sourceAccountId: String,
        destinationAccountId: String,
        amount: Double,
        reference: String
    ): TransferResponse = withContext(Dispatchers.IO) {
        val (bearerToken, service) = getBearerTokenAndService()
        val (_, _, apiKey) = getActiveCredentials()
        val request = TransferRequest(
            transferList = listOf(
                TransferItem(
                    beneficiaryAccountId = destinationAccountId,
                    amount = amount.toString(),
                    myReference = reference,
                    theirReference = reference
                )
            )
        )
        val result = service.transferFunds(bearerToken, apiKey, sourceAccountId, request)
        result.data
    }

    suspend fun getCards(): List<ApiCard> = withContext(Dispatchers.IO) {
        val (bearerToken, service) = getBearerTokenAndService()
        val (_, _, apiKey) = getActiveCredentials()
        val result = service.getCards(bearerToken, apiKey)
        result.data.cards
    }
}
