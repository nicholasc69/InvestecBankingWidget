package com.example.wear.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.BankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import com.example.data.repository.KeyValueSettings
import com.example.data.sync.WearSettingsSyncHelper
import dagger.hilt.android.qualifiers.ApplicationContext

enum class TransactionFilter {
    ALL, DEBIT, CREDIT, PENDING
}

data class WearTransactionsUiState(
    val accounts: List<BankAccountEntity> = emptyList(),
    val selectedAccountId: String = "ALL",
    val selectedAccount: BankAccountEntity? = null,
    val transactions: List<TransactionEntity> = emptyList(),
    val filter: TransactionFilter = TransactionFilter.ALL,
    val searchQuery: String = "",
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val useSandbox: Boolean = true,
    val hasCredentials: Boolean = false,
    val clientIdMasked: String = "Not Configured",
    val connectionStatusMessage: String? = null
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: BankRepository,
    private val settings: KeyValueSettings,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedAccountId = MutableStateFlow("ALL")
    val selectedAccountId: StateFlow<String> = _selectedAccountId.asStateFlow()

    private val _filter = MutableStateFlow(TransactionFilter.ALL)
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _connectionStatusMessage = MutableStateFlow<String?>(null)
    val connectionStatusMessage: StateFlow<String?> = _connectionStatusMessage.asStateFlow()

    private val _reloadTrigger = MutableStateFlow(0)

    val uiState: StateFlow<WearTransactionsUiState> = combine(
        combine(repository.getAccountsFlow(), _selectedAccountId, _filter, _searchQuery) { accounts, selectedId, activeFilter, query ->
            Quad(accounts, selectedId, activeFilter, query)
        },
        combine(_isSyncing, _errorMessage, _reloadTrigger, repository.useSandboxFlow(), _connectionStatusMessage) { syncing, errorMsg, _, sandbox, connMsg ->
            Quint(syncing, errorMsg, sandbox, connMsg, 0)
        },
        combine(repository.clientIdFlow(), repository.clientSecretFlow()) { clientId, clientSecret ->
            clientId to clientSecret
        }
    ) { (accounts, selectedId, activeFilter, query), (syncing, errorMsg, sandbox, connMsg, _), (clientId, clientSecret) ->
        val selectedAcc = accounts.firstOrNull { it.accountId == selectedId }
        val rawTxs = if (selectedId == "ALL") {
            val accIds = accounts.map { it.accountId }
            if (accIds.isNotEmpty()) repository.getAllTransactionsForAccounts(accIds) else emptyList()
        } else {
            repository.getAllTransactions(selectedId)
        }

        val filteredTxs = rawTxs.filter { tx ->
            val matchesFilter = when (activeFilter) {
                TransactionFilter.ALL -> true
                TransactionFilter.DEBIT -> tx.type.equals("DEBIT", ignoreCase = true)
                TransactionFilter.CREDIT -> tx.type.equals("CREDIT", ignoreCase = true)
                TransactionFilter.PENDING -> tx.status.equals("PENDING", ignoreCase = true)
            }
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                tx.description.contains(query, ignoreCase = true) ||
                        tx.transactionType.contains(query, ignoreCase = true) ||
                        tx.amount.toString().contains(query)
            }
            matchesFilter && matchesSearch
        }

        val income = filteredTxs
            .filter { it.type.equals("CREDIT", ignoreCase = true) }
            .sumOf { it.amount }
        val expenses = filteredTxs
            .filter { it.type.equals("DEBIT", ignoreCase = true) }
            .sumOf { it.amount }

        val hasCreds = clientId.isNotBlank() && clientSecret.isNotBlank()
        val maskedCid = getMaskedClientId(clientId)

        WearTransactionsUiState(
            accounts = accounts,
            selectedAccountId = selectedId,
            selectedAccount = selectedAcc,
            transactions = filteredTxs,
            filter = activeFilter,
            searchQuery = query,
            totalIncome = income,
            totalExpenses = expenses,
            isSyncing = syncing,
            errorMessage = errorMsg,
            useSandbox = sandbox,
            hasCredentials = hasCreds,
            clientIdMasked = maskedCid,
            connectionStatusMessage = connMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WearTransactionsUiState()
    )

    init {
        viewModelScope.launch {
            WearSettingsSyncHelper.fetchLatestSettingsFromDataLayer(context, settings) {
                syncData()
            }
            val accounts = repository.getAccounts()
            if (accounts.isEmpty()) {
                syncData()
            }
        }
    }

    fun selectAccount(accountId: String) {
        _selectedAccountId.value = accountId
        _reloadTrigger.value += 1
    }

    fun setFilter(filter: TransactionFilter) {
        _filter.value = filter
        _reloadTrigger.value += 1
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _reloadTrigger.value += 1
    }

    fun setUseSandbox(useSandbox: Boolean) {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.setUseSandbox(useSandbox)
            val modeName = if (useSandbox) "Sandbox Mode" else "Secure API Mode"
            _connectionStatusMessage.value = "Switched to $modeName"
            val result = repository.syncData()
            _isSyncing.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Sync failed for $modeName"
            }
            _reloadTrigger.value += 1
        }
    }

    fun fetchCredentialsFromPhone() {
        viewModelScope.launch {
            _isSyncing.value = true
            _connectionStatusMessage.value = "Requesting credentials from Android app..."
            
            // 1. Send message to phone asking for settings
            WearSettingsSyncHelper.requestSettingsFromPhone(context)
            
            // 2. Wait a bit for the DataLayer to propagate (usually fast, but let's give it a moment)
            kotlinx.coroutines.delay(1500)
            
            // 3. Force a fetch from the DataLayer in case the listener didn't catch it yet
            WearSettingsSyncHelper.fetchLatestSettingsFromDataLayer(context, settings) {
                viewModelScope.launch {
                    val result = repository.syncData()
                    _isSyncing.value = false
                    if (result.isSuccess) {
                        _connectionStatusMessage.value = "Credentials synced and verified!"
                    } else {
                        _connectionStatusMessage.value = "Settings received. Sync error: ${result.exceptionOrNull()?.message}"
                    }
                    _reloadTrigger.value += 1
                }
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            val result = repository.syncData()
            _isSyncing.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Sync failed"
            } else {
                _reloadTrigger.value += 1
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearConnectionStatusMessage() {
        _connectionStatusMessage.value = null
    }

    private fun getMaskedClientId(clientId: String): String {
        return if (clientId.isNotBlank()) {
            if (clientId.length > 8) {
                "${clientId.take(4)}...${clientId.takeLast(4)}"
            } else {
                "••••••••"
            }
        } else {
            "Not Configured"
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
}
