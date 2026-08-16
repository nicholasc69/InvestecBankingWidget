package com.example.ui.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.BankAccountEntity
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                            "Financial Insights & Analytics",
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundLight,
                    titleContentColor = textPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundLight)
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is AnalyticsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentOnContainer)
                    }
                }

                is AnalyticsUiState.Success -> {
                    val accounts = state.accounts

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section: Account Filter Chips
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            AccountFilterRow(
                                accounts = accounts,
                                selectedAccountId = state.selectedAccountId,
                                onSelectAccount = { viewModel.selectAccount(it) }
                            )
                        }

                        // Section: Metric Cards Summary Banner
                        item {
                            MetricsSummaryCard(
                                totalBalance = state.totalNetBalance,
                                totalIncome = state.totalIncome,
                                totalExpenses = state.totalExpenses,
                                savingsRate = state.savingsRate
                            )
                        }

                        // Section 1: Asset Allocation Donut Chart
                        if (state.accountAllocations.isNotEmpty()) {
                            item {
                                DonutChartCard(
                                    allocations = state.accountAllocations,
                                    totalBalance = state.totalNetBalance
                                )
                            }
                        }

                        // Section 2: Cash Flow Bar Chart (Income vs Expense)
                        item {
                            CashFlowBarChartCard(
                                totalIncome = state.totalIncome,
                                totalExpenses = state.totalExpenses
                            )
                        }

                        // Section 3: Spending Category Breakdown
                        if (state.categoryBreakdowns.isNotEmpty()) {
                            item {
                                CategoryBreakdownCard(
                                    categories = state.categoryBreakdowns
                                )
                            }
                        }

                        // Section 4: Daily Net Stream Trend Chart
                        if (state.dailyTrends.isNotEmpty()) {
                            item {
                                DailyTrendChartCard(
                                    trends = state.dailyTrends
                                )
                            }
                        }

                        // Section 5: Top Outflows / Merchants List
                        if (state.topMerchants.isNotEmpty()) {
                            item {
                                TopMerchantsCard(
                                    merchants = state.topMerchants
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountFilterRow(
    accounts: List<BankAccountEntity>,
    selectedAccountId: String,
    onSelectAccount: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            val isSelected = selectedAccountId == "ALL"
            Card(
                onClick = { onSelectAccount("ALL") },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF001B3E) else Color(0xFFFDFBFF)
                ),
                border = BorderStroke(1.dp, if (isSelected) Color(0xFF001B3E) else Color(0xFFC4C6D0))
            ) {
                Text(
                    text = "All Accounts",
                    color = if (isSelected) Color.White else Color(0xFF1A1C1E),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        items(accounts) { account ->
            val isSelected = selectedAccountId == account.accountId
            Card(
                onClick = { onSelectAccount(account.accountId) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF001B3E) else Color(0xFFFDFBFF)
                ),
                border = BorderStroke(1.dp, if (isSelected) Color(0xFF001B3E) else Color(0xFFC4C6D0))
            ) {
                Text(
                    text = account.productName,
                    color = if (isSelected) Color.White else Color(0xFF1A1C1E),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun MetricsSummaryCard(
    totalBalance: Double,
    totalIncome: Double,
    totalExpenses: Double,
    savingsRate: Float
) {
    val df = DecimalFormat("#,##0.00")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF001B3E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NET ASSET LIQUIDITY",
                    color = Color(0xFFD6E3FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (savingsRate >= 0) Color(0xFF116D34) else Color(0xFFBA1A1A))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Savings Rate: ${String.format(java.util.Locale.US, "%.1f", savingsRate)}%",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "R ${df.format(totalBalance)}",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF116D34).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Income",
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Inflow / Income", color = Color(0xFF9EA3B0), fontSize = 10.sp)
                        Text(
                            "+R ${df.format(totalIncome)}",
                            color = Color(0xFF4ADE80),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFBA1A1A).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Expense",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Outflow / Expenses", color = Color(0xFF9EA3B0), fontSize = 10.sp)
                        Text(
                            "-R ${df.format(totalExpenses)}",
                            color = Color(0xFFF87171),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChartCard(
    allocations: List<AccountAllocation>,
    totalBalance: Double
) {
    val df = DecimalFormat("#,##0.00")
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(allocations) {
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = Color(0xFF001B3E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Account Asset Allocation",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Chart Canvas
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 24.dp.toPx()
                        var startAngle = -90f

                        allocations.forEach { alloc ->
                            val sweepAngle = (alloc.percentage / 100f) * 360f * animationProgress.value
                            drawArc(
                                color = alloc.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                                size = Size(size.width, size.height)
                            )
                            startAngle += sweepAngle
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Accounts", fontSize = 10.sp, color = Color(0xFF74777F))
                        Text(
                            "${allocations.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001B3E)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Allocation Legend
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allocations.forEach { alloc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(alloc.color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alloc.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A1C1E)
                                )
                                Text(
                                    text = "R ${df.format(alloc.balance)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF44474E)
                                )
                            }
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", alloc.percentage)}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001B3E)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CashFlowBarChartCard(
    totalIncome: Double,
    totalExpenses: Double
) {
    val df = DecimalFormat("#,##0.00")
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(totalIncome, totalExpenses) {
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    val maxVal = maxOf(totalIncome, totalExpenses, 1.0)
    val incomeHeightRatio = (totalIncome / maxVal).toFloat() * animationProgress.value
    val expenseHeightRatio = (totalExpenses / maxVal).toFloat() * animationProgress.value

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = Color(0xFF001B3E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Income vs. Expenses Cash Flow",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Income Bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.height(130.dp)
                ) {
                    Text(
                        text = "R ${df.format(totalIncome)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF116D34)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .fillMaxWidth(0.35f)
                            .height((100 * incomeHeightRatio).dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(Color(0xFF116D34))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Total Income", fontSize = 11.sp, color = Color(0xFF44474E))
                }

                // Expense Bar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.height(130.dp)
                ) {
                    Text(
                        text = "R ${df.format(totalExpenses)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBA1A1A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .fillMaxWidth(0.35f)
                            .height((100 * expenseHeightRatio).dp)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(Color(0xFFBA1A1A))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Total Expenses", fontSize = 11.sp, color = Color(0xFF44474E))
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(
    categories: List<CategoryBreakdown>
) {
    val df = DecimalFormat("#,##0.00")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = Color(0xFF001B3E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Spending Category Breakdown",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                categories.forEach { cat ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(cat.color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.categoryName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1C1E)
                                )
                            }
                            Text(
                                text = "R ${df.format(cat.totalAmount)} (${String.format(java.util.Locale.US, "%.1f", cat.percentage)}%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF44474E)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { cat.percentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = cat.color,
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyTrendChartCard(
    trends: List<DailyTrendPoint>
) {
    val df = DecimalFormat("#,##0")
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(trends) {
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = Color(0xFF001B3E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Daily Cash Stream Trend",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxAmt = trends.maxOfOrNull { kotlin.math.abs(it.amount) }?.coerceAtLeast(1.0) ?: 1.0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / (trends.size - 1).coerceAtLeast(1)

                    val path = Path()
                    trends.forEachIndexed { i, pt ->
                        val normY = (pt.amount / maxAmt).toFloat()
                        val x = i * stepX
                        val y = height / 2 - (normY * (height / 2.2f) * animationProgress.value)

                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            val prevX = (i - 1) * stepX
                            val prevNormY = (trends[i - 1].amount / maxAmt).toFloat()
                            val prevY = height / 2 - (prevNormY * (height / 2.2f) * animationProgress.value)

                            val controlX1 = prevX + (x - prevX) / 2
                            path.cubicTo(controlX1, prevY, controlX1, y, x, y)
                        }
                    }

                    // Zero baseline
                    drawLine(
                        color = Color(0xFFC4C6D0),
                        start = Offset(0f, height / 2),
                        end = Offset(width, height / 2),
                        strokeWidth = 1.dp.toPx()
                    )

                    drawPath(
                        path = path,
                        color = Color(0xFF001B3E),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                trends.forEach { pt ->
                    Text(
                        text = pt.dateLabel,
                        fontSize = 10.sp,
                        color = Color(0xFF74777F)
                    )
                }
            }
        }
    }
}

@Composable
fun TopMerchantsCard(
    merchants: List<MerchantOutflow>
) {
    val df = DecimalFormat("#,##0.00")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBFF)),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = null,
                    tint = Color(0xFF001B3E),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Top Merchant Outflows",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                merchants.forEach { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFD6E3FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = m.merchantName.take(1),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF001B3E),
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = m.merchantName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1C1E)
                                )
                                Text(
                                    text = "${m.count} transactions",
                                    fontSize = 10.sp,
                                    color = Color(0xFF74777F)
                                )
                            }
                        }

                        Text(
                            text = "-R ${df.format(m.totalSpent)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBA1A1A)
                        )
                    }
                }
            }
        }
    }
}
