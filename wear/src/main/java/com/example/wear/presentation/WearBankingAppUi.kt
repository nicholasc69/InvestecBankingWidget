package com.example.wear.presentation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.data.model.TransactionEntity
import com.example.wear.presentation.theme.WearBankingTheme
import com.example.wear.presentation.transactions.AccountPickerScreen
import com.example.wear.presentation.transactions.TransactionDetailScreen
import com.example.wear.presentation.transactions.TransactionsScreen
import com.example.wear.presentation.transactions.TransactionsViewModel

import com.example.wear.presentation.transactions.ConnectionSettingsScreen

@Composable
fun WearBankingAppUi(
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    WearBankingTheme {
        val navController = rememberSwipeDismissableNavController()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

        SwipeDismissableNavHost(
            navController = navController,
            startDestination = "transactions_list"
        ) {
            composable("transactions_list") {
                TransactionsScreen(
                    uiState = uiState,
                    onTransactionClicked = { tx ->
                        selectedTransaction = tx
                        navController.navigate("transaction_detail")
                    },
                    onAccountPickerRequested = {
                        navController.navigate("account_picker")
                    },
                    onFilterSelected = { filter ->
                        viewModel.setFilter(filter)
                    },
                    onRefreshClicked = {
                        viewModel.syncData()
                    },
                    onClearError = {
                        viewModel.clearError()
                    },
                    onConnectionSettingsRequested = {
                        navController.navigate("connection_settings")
                    }
                )
            }

            composable("connection_settings") {
                ConnectionSettingsScreen(
                    useSandbox = uiState.useSandbox,
                    hasCredentials = uiState.hasCredentials,
                    clientIdMasked = uiState.clientIdMasked,
                    statusMessage = uiState.connectionStatusMessage,
                    isSyncing = uiState.isSyncing,
                    onToggleSandbox = { useSandbox ->
                        viewModel.setUseSandbox(useSandbox)
                    },
                    onFetchCredentials = {
                        viewModel.fetchCredentialsFromPhone()
                    },
                    onBackClicked = {
                        navController.popBackStack()
                    }
                )
            }

            composable("account_picker") {
                AccountPickerScreen(
                    accounts = uiState.accounts,
                    selectedAccountId = uiState.selectedAccountId,
                    onAccountSelected = { accountId ->
                        viewModel.selectAccount(accountId)
                        navController.popBackStack()
                    },
                    onBackClicked = {
                        navController.popBackStack()
                    }
                )
            }

            composable("transaction_detail") {
                val tx = selectedTransaction
                if (tx != null) {
                    TransactionDetailScreen(
                        transaction = tx,
                        currencySymbol = if (uiState.selectedAccount != null) {
                            getCurrencySymbol(uiState.selectedAccount!!.currency)
                        } else "R",
                        onBackClicked = {
                            navController.popBackStack()
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }
}

private fun getCurrencySymbol(currency: String): String {
    return when (currency.uppercase()) {
        "ZAR" -> "R "
        "USD" -> "$ "
        "EUR" -> "€ "
        "GBP" -> "£ "
        else -> "$currency "
    }
}
