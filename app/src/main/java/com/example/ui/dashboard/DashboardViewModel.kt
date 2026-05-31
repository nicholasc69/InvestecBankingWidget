package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.BankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(
        val accounts: List<BankAccountEntity>,
        val selectedAccount: BankAccountEntity?
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: BankRepository
) : ViewModel() {

    // State properties for configuration settings
    val useSandbox: StateFlow<Boolean> = repository.useSandboxFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

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

    // [OPTIMIZATION] Consolidate Room flows into cohesive dashboard UI state
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAccountsFlow(),
        _selectedAccountId
    ) { accounts, selectId ->
        if (accounts.isEmpty()) {
            if (_isRefreshing.value) DashboardUiState.Loading else DashboardUiState.Success(emptyList(), null)
        } else {
            val activeAccount = accounts.find { it.accountId == selectId } ?: accounts.first()
            if (_selectedAccountId.value != activeAccount.accountId) {
                _selectedAccountId.value = activeAccount.accountId
            }
            DashboardUiState.Success(accounts, activeAccount)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )

    // [OPTIMIZATION] Observe transactions for selected account separately to avoid nested fetches in combine
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
        viewModelScope.launch {
            repository.setUseSandbox(useSandbox)
            repository.setClientId(clientId)
            repository.setClientSecret(secret)
            repository.setApiKey(apiKey)

            _clientId.value = clientId
            _clientSecret.value = secret
            _apiKey.value = apiKey

            // Retrigger sync
            refreshData()
        }
    }

    fun clearMessage() {
        _syncMessage.value = null
    }
}
