package com.example.ui.analytics

import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class AccountAllocation(
    val accountId: String,
    val name: String,
    val productName: String,
    val balance: Double,
    val percentage: Float,
    val color: Color
)

data class CategoryBreakdown(
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Float,
    val transactionCount: Int,
    val color: Color
)

data class DailyTrendPoint(
    val dateLabel: String,
    val amount: Double
)

data class MerchantOutflow(
    val merchantName: String,
    val totalSpent: Double,
    val count: Int
)

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data class Success(
        val accounts: List<BankAccountEntity>,
        val selectedAccountId: String,
        val totalNetBalance: Double,
        val totalIncome: Double,
        val totalExpenses: Double,
        val savingsRate: Float,
        val accountAllocations: List<AccountAllocation>,
        val categoryBreakdowns: List<CategoryBreakdown>,
        val dailyTrends: List<DailyTrendPoint>,
        val topMerchants: List<MerchantOutflow>
    ) : AnalyticsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: BankRepository
) : ViewModel() {

    private val _selectedAccountId = MutableStateFlow("ALL")
    val selectedAccountId: StateFlow<String> = _selectedAccountId.asStateFlow()

    private val allocationColors = listOf(
        Color(0xFF001B3E),
        Color(0xFF2563EB),
        Color(0xFF116D34),
        Color(0xFFD97706),
        Color(0xFF7C3AED),
        Color(0xFF0D9488)
    )

    private val categoryColors = mapOf(
        "Retail & Groceries" to Color(0xFF2563EB),
        "Food & Dining" to Color(0xFFD97706),
        "Bills & Utilities" to Color(0xFF7C3AED),
        "Income & Deposits" to Color(0xFF116D34),
        "Transfers" to Color(0xFF0D9488),
        "General / Other" to Color(0xFF64748B)
    )

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getAccountsFlow(),
        _selectedAccountId
    ) { allAccounts, selectedAccId ->
        val selectedProfileId = repository.getSelectedProfileId()
        val profileAccounts = if (!selectedProfileId.isNullOrBlank()) {
            allAccounts.filter { it.profileId == selectedProfileId }
        } else {
            allAccounts
        }
        val activeAccounts = if (profileAccounts.isNotEmpty()) profileAccounts else allAccounts

        Pair(activeAccounts, selectedAccId)
    }.flatMapLatest { (accounts, selectedAccId) ->
        if (accounts.isEmpty()) {
            flowOf(
                AnalyticsUiState.Success(
                    accounts = emptyList(),
                    selectedAccountId = selectedAccId,
                    totalNetBalance = 0.0,
                    totalIncome = 0.0,
                    totalExpenses = 0.0,
                    savingsRate = 0f,
                    accountAllocations = emptyList(),
                    categoryBreakdowns = emptyList(),
                    dailyTrends = emptyList(),
                    topMerchants = emptyList()
                )
            )
        } else {
            val accountIds = if (selectedAccId == "ALL") {
                accounts.map { it.accountId }
            } else {
                listOf(selectedAccId)
            }

            kotlinx.coroutines.flow.flow {
                val transactions = repository.getAllTransactionsForAccounts(accountIds)
                val state = computeAnalyticsState(accounts, selectedAccId, transactions)
                emit(state)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState.Loading
    )

    fun selectAccount(accountId: String) {
        _selectedAccountId.value = accountId
    }

    private fun computeAnalyticsState(
        accounts: List<BankAccountEntity>,
        selectedAccId: String,
        transactions: List<TransactionEntity>
    ): AnalyticsUiState.Success {
        val filteredAccounts = if (selectedAccId == "ALL") {
            accounts
        } else {
            accounts.filter { it.accountId == selectedAccId }
        }

        val totalNetBalance = filteredAccounts.sumOf { it.availableBalance }

        // Compute Allocations
        val accountAllocations = filteredAccounts.mapIndexed { index, acc ->
            val pct = if (totalNetBalance > 0) ((acc.availableBalance / totalNetBalance) * 100).toFloat() else 0f
            AccountAllocation(
                accountId = acc.accountId,
                name = acc.accountName,
                productName = acc.productName,
                balance = acc.availableBalance,
                percentage = pct.coerceAtLeast(0f),
                color = allocationColors[index % allocationColors.size]
            )
        }

        // Compute Income vs Expenses
        val totalIncome = transactions.filter { it.type.equals("CREDIT", ignoreCase = true) }
            .sumOf { it.amount }
        val totalExpenses = transactions.filter { it.type.equals("DEBIT", ignoreCase = true) }
            .sumOf { it.amount }

        val savingsRate = if (totalIncome > 0) {
            (((totalIncome - totalExpenses) / totalIncome) * 100).toFloat().coerceIn(-100f, 100f)
        } else 0f

        // Compute Category Breakdown
        val categoryMap = mutableMapOf<String, Pair<Double, Int>>()
        transactions.forEach { tx ->
            val cat = categorizeTransaction(tx)
            val current = categoryMap[cat] ?: Pair(0.0, 0)
            categoryMap[cat] = Pair(current.first + tx.amount, current.second + 1)
        }

        val grandTotalTxAmount = transactions.sumOf { it.amount }
        val categoryBreakdowns = categoryMap.map { (catName, data) ->
            val (amount, count) = data
            val pct = if (grandTotalTxAmount > 0) ((amount / grandTotalTxAmount) * 100).toFloat() else 0f
            CategoryBreakdown(
                categoryName = catName,
                totalAmount = amount,
                percentage = pct,
                transactionCount = count,
                color = categoryColors[catName] ?: Color(0xFF64748B)
            )
        }.sortedByDescending { it.totalAmount }

        // Compute Daily Trends (sorted chronologically)
        val dailyMap = mutableMapOf<String, Double>()
        transactions.forEach { tx ->
            val dateKey = formatDateKey(tx.postingDate ?: tx.transactionDate ?: "")
            if (dateKey.isNotBlank()) {
                val current = dailyMap[dateKey] ?: 0.0
                val delta = if (tx.type.equals("CREDIT", ignoreCase = true)) tx.amount else -tx.amount
                dailyMap[dateKey] = current + delta
            }
        }
        val dailyTrends = dailyMap.map { (date, amt) ->
            DailyTrendPoint(dateLabel = date, amount = amt)
        }.takeLast(7)

        // Compute Top Merchants / Outflows
        val merchantMap = mutableMapOf<String, Pair<Double, Int>>()
        transactions.filter { it.type.equals("DEBIT", ignoreCase = true) }.forEach { tx ->
            val name = cleanMerchantName(tx.description)
            val current = merchantMap[name] ?: Pair(0.0, 0)
            merchantMap[name] = Pair(current.first + tx.amount, current.second + 1)
        }

        val topMerchants = merchantMap.map { (name, data) ->
            MerchantOutflow(merchantName = name, totalSpent = data.first, count = data.second)
        }.sortedByDescending { it.totalSpent }.take(5)

        return AnalyticsUiState.Success(
            accounts = accounts,
            selectedAccountId = selectedAccId,
            totalNetBalance = totalNetBalance,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            savingsRate = savingsRate,
            accountAllocations = accountAllocations,
            categoryBreakdowns = categoryBreakdowns,
            dailyTrends = dailyTrends,
            topMerchants = topMerchants
        )
    }

    private fun categorizeTransaction(tx: TransactionEntity): String {
        val desc = tx.description.uppercase()
        return when {
            desc.contains("SALARY") || desc.contains("DEPOSIT") || desc.contains("INTEREST") -> "Income & Deposits"
            desc.contains("RESTAURANT") || desc.contains("FOOD") || desc.contains("CAFE") || desc.contains("COFFEE") || desc.contains("GRILL") -> "Food & Dining"
            desc.contains("SHOPPING") || desc.contains("STORE") || desc.contains("RETAIL") || desc.contains("APPLE") || desc.contains("AMAZON") || desc.contains("WOOLWORTHS") || desc.contains("CHECKERS") || desc.contains("PICK N PAY") -> "Retail & Groceries"
            desc.contains("UTILITY") || desc.contains("ELEC") || desc.contains("POWER") || desc.contains("INTERNET") || desc.contains("TELKOM") || desc.contains("VODACOM") -> "Bills & Utilities"
            desc.contains("TRANSFER") || desc.contains("PAYMENT TO") -> "Transfers"
            else -> "General / Other"
        }
    }

    private fun cleanMerchantName(desc: String): String {
        val cleaned = desc.trim().uppercase()
        return when {
            cleaned.contains("APPLE") -> "Apple Store / Services"
            cleaned.contains("WOOLWORTHS") -> "Woolworths"
            cleaned.contains("CHECKERS") -> "Checkers"
            cleaned.contains("PICK N PAY") -> "Pick n Pay"
            cleaned.contains("STARBUCKS") || cleaned.contains("COFFEE") -> "Coffee & Bakery"
            cleaned.contains("AMAZON") -> "Amazon Retail"
            cleaned.contains("UBER") -> "Uber / Rideshare"
            cleaned.contains("ELEC") || cleaned.contains("POWER") -> "Electricity / Utility"
            else -> cleaned.take(20)
        }
    }

    private fun formatDateKey(rawDate: String): String {
        if (rawDate.isBlank()) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = inputFormat.parse(rawDate) ?: return rawDate
            val outputFormat = SimpleDateFormat("dd MMM", Locale.US)
            outputFormat.format(date)
        } catch (e: Exception) {
            rawDate.take(6)
        }
    }
}
