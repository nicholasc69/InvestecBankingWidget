package com.example.ui.dashboard

import com.example.R
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.BankRepository
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val transactions by viewModel.selectedAccountTransactions.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    // Configuration Settings state
    val useSandbox by viewModel.useSandbox.collectAsState()
    val clientId by viewModel.clientId.collectAsState()
    val clientSecret by viewModel.clientSecret.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    // Professional Polish Color Palette Definitions
    val backgroundLight = remember { Color(0xFFF3F4F9) }
    val cardSurface = remember { Color(0xFFFDFBFF) }
    val cardBorder = remember { Color(0xFFC4C6D0) }
    val textPrimary = remember { Color(0xFF1A1C1E) }
    val textSecondary = remember { Color(0xFF44474E) }
    val textMuted = remember { Color(0xFF74777F) }
    val accentContainer = remember { Color(0xFFD6E3FF) }
    val accentOnContainer = remember { Color(0xFF001B3E) }
    val greenCredit = remember { Color(0xFF116D34) }
    val redDebit = remember { Color(0xFFBA1A1A) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.background(backgroundLight),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(accentContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_zebra_head),
                                contentDescription = null,
                                tint = accentOnContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Investec Private Banking",
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshData() },
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = accentOnContainer,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh data",
                                tint = textSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = { showSettings = !showSettings },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Edit Credentials Settings",
                            tint = if (showSettings) accentOnContainer else textSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundLight,
                    titleContentColor = textPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundLight)
                .padding(padding)
        ) {
            AnimatedVisibility(
                visible = showSettings,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                CredentialsSettingsForm(
                    useSandbox = useSandbox,
                    clientId = clientId,
                    clientSecret = clientSecret,
                    apiKey = apiKey,
                    onSave = { sb, cid, secret, key ->
                        viewModel.updateSettings(sb, cid, secret, key)
                        showSettings = false
                    },
                    onCancel = { showSettings = false }
                )
            }

            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = accentOnContainer)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading bank summary from secure cache...",
                                color = textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                is DashboardUiState.Success -> {
                    val accounts = state.accounts
                    val selectedAccount = state.selectedAccount
                    val profiles = state.profiles
                    val selectedProfileId = state.selectedProfileId

                    if (accounts.isEmpty()) {
                        EmptyStateView(
                            onRefresh = { viewModel.refreshData() },
                            onOpenSettings = { showSettings = true }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Section: Profile Selection Dropdown
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                                var expanded by remember { mutableStateOf(false) }
                                val currentProfileName = profiles.find { it.first == selectedProfileId }?.second ?: "Default Profile"

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { expanded = true },
                                        colors = CardDefaults.cardColors(containerColor = cardSurface),
                                        border = BorderStroke(1.dp, cardBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Select Profile",
                                                    color = textMuted,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = currentProfileName,
                                                    color = textPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Select Profile",
                                                tint = textSecondary
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .background(cardSurface)
                                    ) {
                                        profiles.forEach { (profileId, profileName) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = profileName,
                                                        fontWeight = if (profileId == selectedProfileId) FontWeight.Bold else FontWeight.Normal,
                                                        color = textPrimary
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.selectProfile(profileId)
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Section: Horizontal Bank Accounts Card List
                            item {
                                Text(
                                    text = "Your Accounts",
                                    color = textSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(accounts) { account ->
                                        val isCurrent =
                                            account.accountId == selectedAccount?.accountId
                                        BankAccountGlossyCard(
                                            account = account,
                                            isSelected = isCurrent,
                                            onSelect = { viewModel.selectAccount(account.accountId) }
                                        )
                                    }
                                }
                            }

                            // Selected Account Info Details
                            if (selectedAccount != null) {
                                item {
                                    BalanceDetailedMetricsCard(
                                        account = selectedAccount
                                    )
                                }

                                // Section: Transactions header
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF1F3F9))
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "RECENT TRANSACTIONS",
                                                color = textSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.2.sp
                                            )

                                            // KYC status pill
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(
                                                        if (selectedAccount.kycCompliant) Color(
                                                            0xFFE8F5E9
                                                        ) else Color(0xFFFFEBEE)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (selectedAccount.kycCompliant) "KYC COMPLIANT" else "KYC PENDING",
                                                    color = if (selectedAccount.kycCompliant) greenCredit else redDebit,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // List transactions
                                if (transactions.isEmpty()) {
                                    item {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = cardSurface),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, cardBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(
                                                        Icons.Default.Info,
                                                        contentDescription = null,
                                                        tint = textMuted,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        "No recent transaction postings found for this account.",
                                                        color = textSecondary,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    items(transactions) { tx ->
                                        TransactionRow(
                                            transaction = tx,
                                            currencySymbol = getCurrencySymbol(selectedAccount.currency)
                                        )
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }

                is DashboardUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = redDebit,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                state.message,
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.refreshData() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentOnContainer,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Retry Synchronizing")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CredentialsSettingsForm(
    useSandbox: Boolean,
    clientId: String,
    clientSecret: String,
    apiKey: String,
    onSave: (useSandbox: Boolean, clientId: String, clientSecret: String, apiKey: String) -> Unit,
    onCancel: () -> Unit
) {
    var sbState by remember { mutableStateOf(useSandbox) }
    var cidState by remember { mutableStateOf(clientId) }
    var secretState by remember { mutableStateOf(clientSecret) }
    var apiKeyState by remember { mutableStateOf(apiKey) }

    val keyboardController = LocalSoftwareKeyboardController.current

    // Aesthetic color variables
    val cardSurface = Color(0xFFFDFBFF)
    val cardBorder = Color(0xFFC4C6D0)
    val textPrimary = Color(0xFF1A1C1E)
    val textSecondary = Color(0xFF44474E)
    val accentContainer = Color(0xFFD6E3FF)
    val accentOnContainer = Color(0xFF001B3E)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Secure API Connection Settings",
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "By default, the app uses standard, fully open-access Investec API Sandboxes so you can browse, review balances, and test immediate refresh integrations instantly.",
                color = textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // Sandbox mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { sbState = !sbState }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Developer Sandbox Mode",
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Deploys read-only mock banks", color = textSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = sbState,
                    onCheckedChange = { sbState = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentOnContainer,
                        checkedTrackColor = accentContainer,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = cardBorder
                    )
                )
            }

            if (!sbState) {
                // Client ID input
                OutlinedTextField(
                    value = cidState,
                    onValueChange = { cidState = it },
                    label = { Text("Client ID") },
                    textStyle = LocalTextStyle.current.copy(color = textPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_id_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentOnContainer,
                        unfocusedBorderColor = cardBorder,
                        focusedLabelColor = accentOnContainer,
                        unfocusedLabelColor = textSecondary
                    )
                )

                // Secret input
                OutlinedTextField(
                    value = secretState,
                    onValueChange = { secretState = it },
                    label = { Text("Client Secret") },
                    textStyle = LocalTextStyle.current.copy(color = textPrimary),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("client_secret_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentOnContainer,
                        unfocusedBorderColor = cardBorder,
                        focusedLabelColor = accentOnContainer,
                        unfocusedLabelColor = textSecondary
                    )
                )

                // API Key input
                OutlinedTextField(
                    value = apiKeyState,
                    onValueChange = { apiKeyState = it },
                    label = { Text("x-api-key") },
                    textStyle = LocalTextStyle.current.copy(color = textPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentOnContainer,
                        unfocusedBorderColor = cardBorder,
                        focusedLabelColor = accentOnContainer,
                        unfocusedLabelColor = textSecondary
                    )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F3F9))
                        .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            "Active Sandbox Credentials:",
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Client ID: ${BankRepository.DEFAULT_SANDBOX_CLIENT_ID.take(12)}...",
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Secret: **** (Active Personal sandbox)",
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "BaseUrl: https://openapisandbox.investec.com",
                            color = Color(0xFF116D34),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Buttons actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = textSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onSave(sbState, cidState, secretState, apiKeyState)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentContainer,
                        contentColor = accentOnContainer
                    )
                ) {
                    Text("Apply & Sync")
                }
            }
        }
    }
}

@Composable
fun BankAccountGlossyCard(
    account: BankAccountEntity,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val df = DecimalFormat("#,##0.00")
    val displayBalance = df.format(account.availableBalance)
    val symbol = getCurrencySymbol(account.currency)

    // Colors
    val isSelectedBg = Color(0xFFD6E3FF)
    val normBg = Color(0xFFFDFBFF)
    val isSelectedBorder = Color(0xFF001B3E)
    val normBorder = Color(0xFFC4C6D0)

    val contentColor = if (isSelected) Color(0xFF001B3E) else Color(0xFF1A1C1E)
    val labelColor = if (isSelected) Color(0xFF001B3E).copy(alpha = 0.8f) else Color(0xFF44474E)
    val secondaryColor = if (isSelected) Color(0xFF001B3E).copy(alpha = 0.7f) else Color(0xFF74777F)

    Card(
        modifier = Modifier
            .size(width = 240.dp, height = 155.dp)
            .clickable { onSelect() }
            .testTag("account_card_${account.accountId}"),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) isSelectedBg else normBg),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) isSelectedBorder else normBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Visual accent top corner mark
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(isSelectedBorder)
                        .align(Alignment.TopEnd)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = account.productName.uppercase(),
                        color = labelColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = account.accountName,
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = maskAccountNumber(account.accountNumber),
                        color = secondaryColor,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column {
                    Text(
                        text = "Available Balance",
                        color = labelColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$symbol $displayBalance",
                        color = contentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceDetailedMetricsCard(
    account: BankAccountEntity
) {
    val df = DecimalFormat("#,##0.00")
    val symbol = getCurrencySymbol(account.currency)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("balances_detail_card"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with symbol or name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFD6E3FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_zebra_head),
                            contentDescription = null,
                            tint = Color(0xFF001B3E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = account.accountName,
                            color = Color(0xFF1A1C1E),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "**** " + account.accountNumber.takeLast(4),
                            color = Color(0xFF44474E),
                            fontSize = 12.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Secured connection",
                    tint = Color(0xFF44474E),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Balance Display
            Column {
                Text(
                    text = "AVAILABLE BALANCE",
                    color = Color(0xFF44474E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "$symbol ${df.format(account.availableBalance)}",
                    color = Color(0xFF1A1C1E),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFE1E2E9))
            Spacer(modifier = Modifier.height(14.dp))

            // Sub details (Current/Ledger Balance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Ledger Current Balance", color = Color(0xFF44474E), fontSize = 12.sp)
                    Text(
                        "$symbol ${df.format(account.currentBalance)}",
                        color = Color(0xFF1A1C1E),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(width = 1.dp, height = 36.dp)
                        .background(Color(0xFFE1E2E9))
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text("Currency Type", color = Color(0xFF44474E), fontSize = 12.sp)
                    Text(
                        account.currency.uppercase(),
                        color = Color(0xFF1A1C1E),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE1E2E9))
            Spacer(modifier = Modifier.height(12.dp))

            val timeStr = SimpleDateFormat("HH:mm:ss, dd MMM yyyy", LocalLocale.current.platformLocale)
                .format(Date(account.lastUpdated))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ref: ${account.referenceName.ifEmpty { "Investec Personal" }}",
                    color = Color(0xFF74777F),
                    fontSize = 11.sp
                )
                Text(
                    text = "Synced: $timeStr",
                    color = Color(0xFF74777F),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    currencySymbol: String
) {
    val df = DecimalFormat("#,##0.00")
    val isCredit = transaction.type.equals("CREDIT", ignoreCase = true)

    val textPrimary = Color(0xFF1A1C1E)
    val textMuted = Color(0xFF74777F)
    val amountColor = if (isCredit) Color(0xFF116D34) else Color(0xFFBA1A1A)
    val amountPrefix = if (isCredit) "+" else "-"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.uuid ?: transaction.id}")
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE1E2E9)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // White circular container with crisp thin border
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(Color.White)
                            .border(
                                BorderStroke(1.dp, Color(0xFFC4C6D0)),
                                RoundedCornerShape(19.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Dynamically assign stylish vector graphics
                        val iconVector = when {
                            transaction.description.contains(
                                "APPLE",
                                ignoreCase = true
                            ) || transaction.description.contains(
                                "STORE",
                                ignoreCase = true
                            ) -> Icons.Default.ShoppingCart

                            transaction.description.contains(
                                "COFFEE",
                                ignoreCase = true
                            ) || transaction.description.contains(
                                "CAFE",
                                ignoreCase = true
                            ) -> Icons.Default.Favorite

                            transaction.description.contains(
                                "SALARY",
                                ignoreCase = true
                            ) || transaction.description.contains(
                                "DEPOSIT",
                                ignoreCase = true
                            ) || transaction.description.contains(
                                "PAYMENT",
                                ignoreCase = true
                            ) -> Icons.Default.Check

                            transaction.description.contains(
                                "UTILITY",
                                ignoreCase = true
                            ) || transaction.description.contains(
                                "ELEC",
                                ignoreCase = true
                            ) || transaction.description.contains(
                                "POWER",
                                ignoreCase = true
                            ) -> Icons.Default.Star

                            transaction.description.contains(
                                "RESTAURANT",
                                ignoreCase = true
                            ) || transaction.description.contains(
                                "GRILL",
                                ignoreCase = true
                            ) || transaction.description.contains(
                                "FOOD",
                                ignoreCase = true
                            ) -> Icons.Default.Person

                            isCredit -> Icons.Default.Add
                            else -> Icons.Default.KeyboardArrowDown
                        }

                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF44474E)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = transaction.description.trim().uppercase(),
                            color = textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = transaction.transactionType,
                                color = textMuted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (transaction.status == "POSTED") Color(0xFFF1F3F9) else Color(
                                            0xFFD6E3FF
                                        )
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = transaction.status,
                                    color = if (transaction.status == "POSTED") Color(0xFF44474E) else Color(
                                        0xFF001B3E
                                    ),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$amountPrefix$currencySymbol${df.format(transaction.amount)}",
                        color = amountColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatDate(
                            transaction.postingDate ?: transaction.transactionDate ?: ""
                        ),
                        color = textMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val backgroundLight = Color(0xFFF3F4F9)
    val textPrimary = Color(0xFF1A1C1E)
    val textSecondary = Color(0xFF44474E)
    val accentContainer = Color(0xFFD6E3FF)
    val accentOnContainer = Color(0xFF001B3E)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundLight)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(accentContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = accentOnContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Secure Bank Cache Empty",
                color = textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "No local bank summaries exist yet. Let's sync with the Investec personal account sandbox server to load dynamic demo profiles, cash streams, and recent postings.",
                color = textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row {
                Button(
                    onClick = onOpenSettings,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE1E2E9),
                        contentColor = textPrimary
                    )
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("API Setup")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentOnContainer,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Sandbox Now")
                }
            }
        }
    }
}

// ==========================================
// VALUE FORMATTING HELPERS
// ==========================================

fun getCurrencySymbol(isoCode: String): String {
    return when (isoCode.uppercase()) {
        "ZAR" -> "R"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> isoCode
    }
}

fun maskAccountNumber(accNum: String): String {
    if (accNum.length < 5) return accNum
    return "****" + accNum.substring(accNum.length - 4)
}

fun formatDate(rawDate: String): String {
    if (rawDate.isEmpty()) return ""
    return try {
        // Handle standard simple format e.g., "2020-06-11"
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = inputFormat.parse(rawDate) ?: return rawDate
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        outputFormat.format(date)
    } catch (e: Exception) {
        // Fallback for full DateTime strings
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = inputFormat.parse(rawDate) ?: return rawDate
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
            outputFormat.format(date)
        } catch (e2: Exception) {
            rawDate
        }
    }
}
