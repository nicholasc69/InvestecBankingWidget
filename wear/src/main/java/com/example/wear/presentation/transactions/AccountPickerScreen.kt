package com.example.wear.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.example.data.model.BankAccountEntity
import com.example.wear.presentation.theme.CreditGreen
import com.example.wear.presentation.theme.InvestecBlueAccent
import com.example.wear.presentation.theme.InvestecGold
import com.example.wear.presentation.theme.TextSecondary
import java.text.DecimalFormat

@Composable
fun AccountPickerScreen(
    accounts: List<BankAccountEntity>,
    selectedAccountId: String,
    useSandbox: Boolean = false,
    onAccountSelected: (String) -> Unit,
    onConnectionSettingsRequested: () -> Unit,
    onBackClicked: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val df = DecimalFormat("#,##0.00")

    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 24.dp)
        ) {
            item {
                Text(
                    text = "SELECT ACCOUNT",
                    style = MaterialTheme.typography.caption2,
                    color = InvestecGold,
                    fontWeight = FontWeight.Bold
                )
            }

            // Option: All Accounts
            item {
                Spacer(modifier = Modifier.height(8.dp))
                val isSelected = selectedAccountId == "ALL"
                Chip(
                    onClick = { onAccountSelected("ALL") },
                    label = {
                        Text(
                            text = "All Accounts",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "${accounts.size} linked accounts",
                            color = TextSecondary
                        )
                    },
                    colors = if (isSelected) {
                        ChipDefaults.chipColors(
                            backgroundColor = InvestecBlueAccent,
                            contentColor = Color.Black
                        )
                    } else {
                        ChipDefaults.secondaryChipColors()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Individual Accounts
            items(accounts) { account ->
                val isSelected = selectedAccountId == account.accountId
                val symbol = getCurrencySymbol(account.currency)
                val formattedBalance = "$symbol${df.format(account.availableBalance)}"
                val maskedAccNum = maskAccNum(account.accountNumber)

                Spacer(modifier = Modifier.height(4.dp))
                Chip(
                    onClick = { onAccountSelected(account.accountId) },
                    label = {
                        Text(
                            text = account.accountName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "$maskedAccNum • $formattedBalance",
                            fontSize = 10.sp,
                            color = if (isSelected) Color.Black.copy(alpha = 0.8f) else TextSecondary
                        )
                    },
                    colors = if (isSelected) {
                        ChipDefaults.chipColors(
                            backgroundColor = InvestecBlueAccent,
                            contentColor = Color.Black
                        )
                    } else {
                        ChipDefaults.secondaryChipColors()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Connection Settings Button
            item {
                Spacer(modifier = Modifier.height(12.dp))
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
                            text = if (useSandbox) "Sandbox Mode" else "Secure API Mode",
                            fontSize = 9.sp,
                            color = if (useSandbox) InvestecGold else CreditGreen,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                CompactChip(
                    onClick = onBackClicked,
                    label = {
                        Text(
                            text = "Done",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = ChipDefaults.primaryChipColors()
                )
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

private fun maskAccNum(accNum: String): String {
    return if (accNum.length > 4) "•••• ${accNum.takeLast(4)}" else accNum
}
