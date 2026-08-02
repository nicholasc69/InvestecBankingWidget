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

import android.content.Context
import com.example.data.repository.KeyValueSettings
import com.example.data.sync.WearSettingsSyncHelper
import dagger.hilt.android.qualifiers.ApplicationContext

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
    private val repository: BankRepository,
    private val settings: KeyValueSettings,
    @ApplicationContext private val context: Context
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

    // Currently selected account ID
    private val _selectedAccountId = MutableStateFlow<String?>(null)

    // Sync activity indicators
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    // Consolidate Room flows into cohesive dashboard UI state, filtering by profile
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAccountsFlow(),
        _selectedProfileId,
        _selectedAccountId,
        _isRefreshing
    ) { accounts, selectProfileId, selectAccountId, refreshing ->
        if (accounts.isEmpty()) {
            if (refreshing) DashboardUiState.Loading else DashboardUiState.Success(
                emptyList(),
                null,
                emptyList(),
                null
            )
        } else {
            // Extract distinct profiles from all synced accounts
            val profilesList = accounts.map { it.profileId to it.profileName }.distinct()

            // Resolve the active profile ID (validating against available profiles in synced data)
            val storedProfileId = selectProfileId ?: repository.getSelectedProfileId()
            val activeProfileId = if (storedProfileId != null && profilesList.any { it.first == storedProfileId }) {
                storedProfileId
            } else {
                profilesList.firstOrNull()?.first
            }

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
            WearSettingsSyncHelper.pushSettingsToWear(context, settings)
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

            // Reset selected profile & account to ensure clean profile resolution with new credentials
            repository.setSelectedProfileId(null)
            _selectedProfileId.value = null
            _selectedAccountId.value = null

            _clientId.value = clientId
            _clientSecret.value = secret
            _apiKey.value = apiKey

            // Sync settings to Wear OS DataLayer
            WearSettingsSyncHelper.pushSettingsToWear(context, settings)

            // Retrigger sync
            refreshData()
        }
    }

    fun clearMessage() {
        _syncMessage.value = null
    }
}
