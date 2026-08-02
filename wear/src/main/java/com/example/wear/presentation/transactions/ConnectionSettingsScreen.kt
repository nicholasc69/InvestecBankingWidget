package com.example.wear.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import com.example.wear.presentation.theme.*

@Composable
fun ConnectionSettingsScreen(
    useSandbox: Boolean,
    hasCredentials: Boolean,
    clientIdMasked: String,
    statusMessage: String?,
    isSyncing: Boolean,
    onToggleSandbox: (Boolean) -> Unit,
    onFetchCredentials: () -> Unit,
    onBackClicked: () -> Unit
) {
    val listState = rememberScalingLazyListState()

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
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 24.dp)
        ) {
            // Title
            item {
                Text(
                    text = "SETTINGS",
                    fontSize = 11.sp,
                    color = InvestecGold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Current Active Mode Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    onClick = { onToggleSandbox(!useSandbox) },
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = if (useSandbox) InvestecNavy else Color(0xFF0F382C),
                        endBackgroundColor = if (useSandbox) InvestecNavy else Color(0xFF0F382C)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = if (useSandbox) "SANDBOX MODE" else "SECURE API MODE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (useSandbox) InvestecGold else CreditGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (useSandbox) "openapisandbox.investec.com" else "openapi.investec.com",
                            fontSize = 8.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Toggle Switcher between Sandbox & Secure API
            item {
                Spacer(modifier = Modifier.height(4.dp))
                ToggleChip(
                    checked = !useSandbox, // checked = Secure API active
                    onCheckedChange = { isSecureApi ->
                        onToggleSandbox(!isSecureApi)
                    },
                    label = {
                        Text(
                            text = if (!useSandbox) "Secure API Mode" else "Sandbox Mode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (!useSandbox) "Production Investec API" else "Investec Demo Sandbox",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    },
                    toggleControl = {
                        Switch(
                            checked = !useSandbox,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CreditGreen,
                                checkedTrackColor = CreditGreen.copy(alpha = 0.5f)
                            )
                        )
                    },
                    colors = ToggleChipDefaults.toggleChipColors(
                        checkedStartBackgroundColor = CardBackground,
                        checkedEndBackgroundColor = CardBackground,
                        uncheckedStartBackgroundColor = CardBackground,
                        uncheckedEndBackgroundColor = CardBackground
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Android App Credentials Summary Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    onClick = onFetchCredentials,
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = SurfaceDark,
                        endBackgroundColor = SurfaceDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = "ANDROID APP CREDENTIALS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = InvestecBlueAccent
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Client ID: $clientIdMasked",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (hasCredentials) "Secret & Key: Stored" else "Credentials: Not Configured",
                            fontSize = 9.sp,
                            color = if (hasCredentials) CreditGreen else DebitRed
                        )
                    }
                }
            }

            // Action Chip: Fetch credentials from phone
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Chip(
                    onClick = onFetchCredentials,
                    enabled = !isSyncing,
                    label = {
                        Text(
                            text = if (isSyncing) "Fetching..." else "Fetch from App",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "Get credentials from Android app",
                            fontSize = 8.sp,
                            color = TextSecondary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Status message (if any)
            if (statusMessage != null) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 9.sp,
                        color = InvestecBlueAccent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Back button
            item {
                Spacer(modifier = Modifier.height(10.dp))
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
