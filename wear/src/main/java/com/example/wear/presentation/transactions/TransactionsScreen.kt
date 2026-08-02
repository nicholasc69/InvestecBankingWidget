package com.example.wear.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import com.example.wear.presentation.theme.*
import java.text.DecimalFormat

@Composable
fun TransactionsScreen(
    uiState: WearTransactionsUiState,
    onTransactionClicked: (TransactionEntity) -> Unit,
    onAccountPickerRequested: () -> Unit,
    onFilterSelected: (TransactionFilter) -> Unit,
    onRefreshClicked: () -> Unit,
    onClearError: () -> Unit,
    onConnectionSettingsRequested: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val df = DecimalFormat("#,##0.00")

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 24.dp)
        ) {

            // 1. Account Summary Banner Header
            item {
                AccountSummaryHeader(
                    selectedAccount = uiState.selectedAccount,
                    totalAccounts = uiState.accounts.size,
                    onAccountPickerRequested = onAccountPickerRequested
                )
            }

            // 2. Filter Bar (ALL, DEBIT, CREDIT, PENDING)
            item {
                Spacer(modifier = Modifier.height(6.dp))
                FilterChipGroup(
                    activeFilter = uiState.filter,
                    onFilterSelected = onFilterSelected
                )
            }

            // 3. Sync Error Chip (if any)
            val err = uiState.errorMessage
            if (err != null) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        onClick = onClearError,
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = Color(0xFF3E1A1A),
                            endBackgroundColor = Color(0xFF3E1A1A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.caption2,
                                color = DebitRed,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Tap to dismiss",
                                style = MaterialTheme.typography.caption3,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // 4. Loading Spinner (during sync)
            if (uiState.isSyncing) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            indicatorColor = InvestecBlueAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Syncing...",
                            style = MaterialTheme.typography.caption2,
                            color = InvestecBlueAccent
                        )
                    }
                }
            }

            // 5. Income / Expense Stats Banner
            if (uiState.transactions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (uiState.totalIncome > 0.0) {
                            Text(
                                text = "+R ${df.format(uiState.totalIncome)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CreditGreen
                            )
                        }
                        if (uiState.totalExpenses > 0.0) {
                            Text(
                                text = "-R ${df.format(uiState.totalExpenses)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DebitRed
                            )
                        }
                    }
                }
            }

            // 6. Transactions List
            if (uiState.transactions.isEmpty() && !uiState.isSyncing) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "No Transactions Found",
                            style = MaterialTheme.typography.body2,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap Refresh to sync from Investec",
                            style = MaterialTheme.typography.caption3,
                            color = TextSecondary.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(uiState.transactions) { tx ->
                    Spacer(modifier = Modifier.height(4.dp))
                    TransactionCardItem(
                        transaction = tx,
                        currencySymbol = getCurrencySymbol(uiState.selectedAccount?.currency ?: "ZAR"),
                        onClick = { onTransactionClicked(tx) }
                    )
                }
            }

            // 7. Footer Action Buttons
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Chip(
                        onClick = onRefreshClicked,
                        enabled = !uiState.isSyncing,
                        label = {
                            Text(
                                text = if (uiState.isSyncing) "Syncing..." else "Refresh Sync",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Chip(
                        onClick = onConnectionSettingsRequested,
                        label = {
                            Text(
                                text = "Connection Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        secondaryLabel = {
                            Text(
                                text = if (uiState.useSandbox) "Sandbox Mode" else "Secure API Mode",
                                fontSize = 9.sp,
                                color = if (uiState.useSandbox) InvestecGold else CreditGreen,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    CompactChip(
                        onClick = onAccountPickerRequested,
                        label = {
                            Text(
                                text = "Switch Account",
                                fontSize = 10.sp,
                                color = InvestecBlueAccent
                            )
                        },
                        colors = ChipDefaults.chipColors(backgroundColor = Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSummaryHeader(
    selectedAccount: BankAccountEntity?,
    totalAccounts: Int,
    onAccountPickerRequested: () -> Unit
) {
    val df = DecimalFormat("#,##0.00")
    val title = selectedAccount?.accountName ?: "All Accounts"
    val symbol = getCurrencySymbol(selectedAccount?.currency ?: "ZAR")
    val balanceText = if (selectedAccount != null) {
        "$symbol${df.format(selectedAccount.availableBalance)}"
    } else {
        "$totalAccounts Accounts Linked"
    }
    val subtitleText = if (selectedAccount != null) {
        "Available • ${maskAccNum(selectedAccount.accountNumber)}"
    } else {
        "Tap to filter by account"
    }

    Card(
        onClick = onAccountPickerRequested,
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = InvestecNavy,
            endBackgroundColor = InvestecNavy
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "INVESTEC",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = InvestecGold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.caption1,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = balanceText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitleText,
                fontSize = 10.sp,
                color = InvestecBlueAccent
            )
        }
    }
}

@Composable
private fun FilterChipGroup(
    activeFilter: TransactionFilter,
    onFilterSelected: (TransactionFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransactionFilter.entries.forEach { filter ->
            val isSelected = activeFilter == filter
            val labelText = when (filter) {
                TransactionFilter.ALL -> "All"
                TransactionFilter.DEBIT -> "Debits"
                TransactionFilter.CREDIT -> "Credits"
                TransactionFilter.PENDING -> "Pending"
            }

            CompactChip(
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = labelText,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = if (isSelected) {
                    ChipDefaults.chipColors(
                        backgroundColor = InvestecBlueAccent,
                        contentColor = Color.Black
                    )
                } else {
                    ChipDefaults.chipColors(
                        backgroundColor = SurfaceDark,
                        contentColor = TextSecondary
                    )
                },
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@Composable
private fun TransactionCardItem(
    transaction: TransactionEntity,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val isCredit = transaction.type.equals("CREDIT", ignoreCase = true)
    val amountColor = if (isCredit) CreditGreen else Color.White
    val signSymbol = if (isCredit) "+" else "-"
    val df = DecimalFormat("#,##0.00")
    val formattedAmount = "$signSymbol$currencySymbol${df.format(transaction.amount)}"

    val iconColor = if (isCredit) CreditGreen else DebitRed
    val iconBadge = if (isCredit) "↓" else "↑"

    Card(
        onClick = onClick,
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = CardBackground,
            endBackgroundColor = CardBackground
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        color = iconColor.copy(alpha = 0.2f),
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconBadge,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.description,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transaction.postingDate ?: transaction.transactionDate ?: transaction.transactionType,
                    fontSize = 9.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = formattedAmount,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                textAlign = TextAlign.End
            )
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

private fun maskAccNum(accNum: String): String {
    return if (accNum.length > 4) "•••• ${accNum.takeLast(4)}" else accNum
}
