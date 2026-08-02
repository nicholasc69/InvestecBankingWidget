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
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.example.data.model.TransactionEntity
import com.example.wear.presentation.theme.CreditGreen
import com.example.wear.presentation.theme.DebitRed
import com.example.wear.presentation.theme.InvestecBlueAccent
import com.example.wear.presentation.theme.TextSecondary
import java.text.DecimalFormat

@Composable
fun TransactionDetailScreen(
    transaction: TransactionEntity,
    currencySymbol: String = "R ",
    onBackClicked: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val isCredit = transaction.type.equals("CREDIT", ignoreCase = true)
    val amountColor = if (isCredit) CreditGreen else DebitRed
    val signSymbol = if (isCredit) "+" else "-"
    val df = DecimalFormat("#,##0.00")
    val formattedAmount = "$signSymbol$currencySymbol${df.format(transaction.amount)}"

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
            // Screen Header Title
            item {
                Text(
                    text = "TRANSACTION DETAILS",
                    style = MaterialTheme.typography.caption2,
                    color = InvestecBlueAccent,
                    fontWeight = FontWeight.Bold
                )
            }

            // Big Amount Display
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedAmount,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    textAlign = TextAlign.Center
                )
            }

            // Type & Status Badges
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactChip(
                        onClick = {},
                        label = {
                            Text(
                                text = transaction.transactionType.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = ChipDefaults.secondaryChipColors()
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    CompactChip(
                        onClick = {},
                        label = {
                            Text(
                                text = transaction.status.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = if (transaction.status.equals("POSTED", ignoreCase = true)) {
                            ChipDefaults.chipColors(backgroundColor = Color(0xFF1E3A2B), contentColor = CreditGreen)
                        } else {
                            ChipDefaults.chipColors(backgroundColor = Color(0xFF3A2B1E), contentColor = Color(0xFFFFB74D))
                        }
                    )
                }
            }

            // Description Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text(
                            text = "Merchant / Description",
                            style = MaterialTheme.typography.caption2,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = transaction.description,
                            style = MaterialTheme.typography.body2,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Date & Time Info
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        Text(
                            text = "Transaction Date",
                            style = MaterialTheme.typography.caption2,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = transaction.postingDate ?: transaction.transactionDate ?: "N/A",
                            style = MaterialTheme.typography.body2,
                            color = Color.White
                        )
                    }
                }
            }

            // Running Balance Info
            if (transaction.runningBalance > 0.0) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            Text(
                                text = "Running Balance",
                                style = MaterialTheme.typography.caption2,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currencySymbol${df.format(transaction.runningBalance)}",
                                style = MaterialTheme.typography.body2,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // UUID / Ref
            val uuidStr = transaction.uuid
            if (!uuidStr.isNullOrBlank()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ref: ${uuidStr.take(12)}...",
                        style = MaterialTheme.typography.caption3,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Back Button
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Chip(
                    onClick = onBackClicked,
                    label = {
                        Text(
                            text = "Back to List",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = ChipDefaults.primaryChipColors()
                )
            }
        }
    }
}
