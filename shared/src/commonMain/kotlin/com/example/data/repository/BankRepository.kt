package com.example.data.repository

import com.example.data.api.InvestecApiClient
import com.example.data.api.InvestecApiService
import com.example.data.local.BankAccountDao
import com.example.data.local.TransactionDao
import com.example.data.model.*
import io.ktor.util.encodeBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BankRepository(
    private val accountDao: BankAccountDao,
    private val transactionDao: TransactionDao,
    private val settings: KeyValueSettings
) {

    var onSyncCompleted: (() -> Unit)? = null

    companion object {
        private const val TAG = "BankRepository"

        const val BASE_URL_SANDBOX = "https://openapisandbox.investec.com"
        const val BASE_URL_PRODUCTION = "https://openapi.investec.com"

        const val DEFAULT_SANDBOX_CLIENT_ID = ""
        const val DEFAULT_SANDBOX_CLIENT_SECRET = ""
        const val DEFAULT_SANDBOX_API_KEY = ""

        private const val USE_SANDBOX = "use_sandbox"
    }

    // ==========================================
    // CONFIGURATION ACCESSORS
    // ==========================================

    fun useSandboxFlow(): Flow<Boolean> = settings.getBooleanFlow(USE_SANDBOX, true)

    suspend fun useSandbox(): Boolean = useSandboxFlow().first()

    suspend fun setUseSandbox(use: Boolean) {
        settings.setBoolean(USE_SANDBOX, use)
    }

    fun getClientId(): String = settings.getString("client_id", "")

    fun clientIdFlow(): Flow<String> = settings.getStringFlow("client_id", "")

    fun setClientId(clientId: String) {
        settings.setString("client_id", clientId)
    }

    fun getClientSecret(): String = settings.getString("client_secret", "")

    fun clientSecretFlow(): Flow<String> = settings.getStringFlow("client_secret", "")

    fun setClientSecret(secret: String) {
        settings.setString("client_secret", secret)
    }

    fun getApiKey(): String = settings.getString("api_key", "")

    fun apiKeyFlow(): Flow<String> = settings.getStringFlow("api_key", "")

    fun setApiKey(apiKey: String) {
        settings.setString("api_key", apiKey)
    }

    fun getSelectedProfileId(): String? {
        val id = settings.getString("selected_profile_id", "")
        return if (id.isEmpty()) null else id
    }

    fun setSelectedProfileId(profileId: String?) {
        settings.setString("selected_profile_id", profileId ?: "")
    }

    // Resolves current active credentials based on configuration
    suspend fun getActiveCredentials(): Triple<String, String, String> {
        return if (useSandbox()) {
            val defaultCid = settings.getString("default_sandbox_client_id", "")
            val defaultSec = settings.getString("default_sandbox_client_secret", "")
            val defaultKey = settings.getString("default_sandbox_api_key", "")

            val cid = defaultCid.ifBlank { getClientId() }
            val sec = defaultSec.ifBlank { getClientSecret() }
            val key = defaultKey.ifBlank { getApiKey() }
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
            logDebug(TAG, "Requesting token from codebase: $baseUrl")
            val authString = "$clientId:$secret"
            val base64Auth = "Basic " + authString.encodeToByteArray().encodeBase64()

            val tokenResponse = dynamicService.getAccessToken(
                basicAuthHeader = base64Auth,
                apiKey = apiKey
            )
            val bearerToken = "Bearer ${tokenResponse.accessToken}"

            // Step 2: Fetch Accounts list
            logDebug(TAG, "Fetching cash accounts...")
            val accountsResult = dynamicService.getAccounts(bearerToken, apiKey)
            val apiAccounts = accountsResult.data.accounts

            // Step 3: Parallel Synchronization
            val cachedAccounts = apiAccounts.map { apiAcc ->
                async {
                    logDebug(TAG, "Syncing metadata for Account ID: ${apiAcc.accountId}")

                    // Fetch Balance
                    val balanceResult = try {
                        dynamicService.getAccountBalance(bearerToken, apiKey, apiAcc.accountId).data
                    } catch (e: Exception) {
                        logError(TAG, "Failed to get balance for ${apiAcc.accountId}: ${e.message}")
                        null
                    }

                    val currentBalance = balanceResult?.currentBalance ?: 0.0
                    val availableBalance = balanceResult?.availableBalance ?: 0.0
                    val currency = balanceResult?.currency ?: "ZAR"

                    // Fetch transactions in parallel (from 3 months ago to today)
                    try {
                        val (fromDateStr, toDateStr) = getSyncDateRange()

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
                        logError(
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
                        lastUpdated = getCurrentTimeMillis()
                    )
                }
            }.awaitAll()

            if (cachedAccounts.isNotEmpty()) {
                accountDao.replaceAccounts(cachedAccounts)
            } else {
                accountDao.clearAccounts()
            }

            // Trigger widget update callback
            onSyncCompleted?.invoke()

            Result.success(Unit)
        } catch (e: Exception) {
            logError(TAG, "Sync operation failed", e)
            Result.failure(e)
        }
    }

    private suspend fun getBearerTokenAndService(): Pair<String, InvestecApiService> {
        val (clientId, secret, apiKey) = getActiveCredentials()
        if (clientId.isBlank() || secret.isBlank() || apiKey.isBlank()) {
            throw Exception("Incomplete credentials. Please update settings.")
        }
        val baseUrl = getActiveBaseUrl()
        val service = InvestecApiClient.getService(baseUrl)

        val authString = "$clientId:$secret"
        val base64Auth = "Basic " + authString.encodeToByteArray().encodeBase64()

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
