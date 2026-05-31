package com.example.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.BankRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(
        val accounts: List<BankAccountEntity>,
        val selectedAccount: BankAccountEntity?,
        val transactions: List<TransactionEntity>
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BankRepository(application)

    // State properties for configuration settings
    private val _useSandbox = MutableStateFlow(repository.useSandbox())
    val useSandbox: StateFlow<Boolean> = _useSandbox.asStateFlow()

    private val _clientId = MutableStateFlow(repository.getClientId())
    val clientId: StateFlow<String> = _clientId.asStateFlow()

    private val _clientSecret = MutableStateFlow(repository.getClientSecret())
    val clientSecret: StateFlow<String> = _clientSecret.asStateFlow()

    private val _apiKey = MutableStateFlow(repository.getApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    // Currently selected account ID
    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId: StateFlow<String?> = _selectedAccountId.asStateFlow()

    // Sync activity indicators
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    // Consolidate Room flows into cohesive dashboard UI state
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAccountsFlow(),
        _selectedAccountId
    ) { accounts, selectId ->
        if (accounts.isEmpty()) {
            if (_isRefreshing.value) DashboardUiState.Loading else DashboardUiState.Success(emptyList(), null, emptyList())
        } else {
            // Pick selected account, falling back to first available cached account
            val activeAccount = accounts.find { it.accountId == selectId } ?: accounts.first()
            if (_selectedAccountId.value != activeAccount.accountId) {
                _selectedAccountId.value = activeAccount.accountId
            }
            // Combine with transaction observations
            val txs = repository.getLastFiveTransactions(activeAccount.accountId)
            DashboardUiState.Success(accounts, activeAccount, txs)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )

    // Observe transactions for selected account and automatically trigger updates inside Success state
    val selectedAccountTransactions: StateFlow<List<TransactionEntity>> = _selectedAccountId
        .filterNotNull()
        .flatMapLatest { accountId ->
            repository.getLastFiveTransactionsFlow(accountId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Automatically sync on first launch to populate demo sandbox data
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _syncMessage.value = "Synchronizing account details..."
            val result = repository.syncData()
            if (result.isSuccess) {
                _syncMessage.value = "Sync complete"
            } else {
                _syncMessage.value = "Sync failed: ${result.exceptionOrNull()?.message}"
            }
            _isRefreshing.value = false
        }
    }

    fun selectAccount(accountId: String) {
        _selectedAccountId.value = accountId
    }

    fun updateSettings(useSandbox: Boolean, clientId: String, secret: String, apiKey: String) {
        repository.setUseSandbox(useSandbox)
        repository.setClientId(clientId)
        repository.setClientSecret(secret)
        repository.setApiKey(apiKey)

        _useSandbox.value = useSandbox
        _clientId.value = clientId
        _clientSecret.value = secret
        _apiKey.value = apiKey

        // Retrigger sync with new configurations
        refreshData()
    }

    fun clearMessage() {
        _syncMessage.value = null
    }
}
