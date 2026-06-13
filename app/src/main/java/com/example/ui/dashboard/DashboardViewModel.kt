package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.BankRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(
        val accounts: List<BankAccountEntity>,
        val selectedAccount: BankAccountEntity?,
        val profiles: List<Pair<String, String>> = emptyList(),
        val selectedProfileId: String? = null
    ) : DashboardUiState

    data class Error(val message: String) : DashboardUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
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

    // Currently selected profile ID
    private val _selectedProfileId = MutableStateFlow<String?>(null)
    val selectedProfileId: StateFlow<String?> = _selectedProfileId.asStateFlow()

    // Currently selected account ID
    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId: StateFlow<String?> = _selectedAccountId.asStateFlow()

    // Sync activity indicators
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    // Consolidate Room flows into cohesive dashboard UI state, filtering by profile
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAccountsFlow(),
        _selectedProfileId,
        _selectedAccountId
    ) { accounts, selectProfileId, selectAccountId ->
        if (accounts.isEmpty()) {
            if (_isRefreshing.value) DashboardUiState.Loading else DashboardUiState.Success(
                emptyList(),
                null,
                emptyList(),
                null
            )
        } else {
            // Extract distinct profiles from all synced accounts
            val profilesList = accounts.map { it.profileId to it.profileName }.distinct()

            // Resolve the active profile ID (default to first profile in the list)
            val activeProfileId = selectProfileId ?: repository.getSelectedProfileId() ?: profilesList.firstOrNull()?.first

            // Filter accounts belonging only to the active profile
            val filteredAccounts = if (activeProfileId != null) {
                accounts.filter { it.profileId == activeProfileId }
            } else {
                accounts
            }

            // Find the selected account, defaulting to the first account of the active profile
            val activeAccount = filteredAccounts.find { it.accountId == selectAccountId } ?: filteredAccounts.firstOrNull()

            // Synchronize the backing state values
            if (_selectedProfileId.value != activeProfileId) {
                _selectedProfileId.value = activeProfileId
                repository.setSelectedProfileId(activeProfileId)
            }
            if (activeAccount != null && _selectedAccountId.value != activeAccount.accountId) {
                _selectedAccountId.value = activeAccount.accountId
            }

            DashboardUiState.Success(
                accounts = filteredAccounts,
                selectedAccount = activeAccount,
                profiles = profilesList,
                selectedProfileId = activeProfileId
            )
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

    fun selectProfile(profileId: String) {
        if (_selectedProfileId.value != profileId) {
            _selectedProfileId.value = profileId
            _selectedAccountId.value = null
            repository.setSelectedProfileId(profileId)
            refreshData()
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
