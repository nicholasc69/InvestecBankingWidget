package com.example.wear

import com.example.data.local.BankAccountDao
import com.example.data.local.TransactionDao
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.BankRepository
import com.example.data.repository.KeyValueSettings
import com.example.wear.presentation.transactions.TransactionFilter
import com.example.wear.presentation.transactions.TransactionsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class FakeBankAccountDao : BankAccountDao {
    val accountsFlow = MutableStateFlow<List<BankAccountEntity>>(emptyList())
    override fun getAccountsFlow(): Flow<List<BankAccountEntity>> = accountsFlow
    override suspend fun getAccounts(): List<BankAccountEntity> = accountsFlow.value
    override suspend fun getAccountById(accountId: String): BankAccountEntity? = accountsFlow.value.find { it.accountId == accountId }
    override fun getAccountByIdFlow(accountId: String): Flow<BankAccountEntity?> = MutableStateFlow(accountsFlow.value.find { it.accountId == accountId })
    override suspend fun insertAccounts(accounts: List<BankAccountEntity>) { this.accountsFlow.value = accounts }
    override suspend fun clearAccounts() { accountsFlow.value = emptyList() }
}

class FakeTransactionDao : TransactionDao {
    val transactions = mutableListOf<TransactionEntity>()
    override fun getLastFiveTransactionsFlow(accountId: String): Flow<List<TransactionEntity>> = flowOf(transactions.take(5))
    override suspend fun getLastFiveTransactions(accountId: String): List<TransactionEntity> = transactions.take(5)
    override suspend fun getLastFiveTransactionsForAccounts(accountIds: List<String>): List<TransactionEntity> = transactions.filter { it.accountId in accountIds }.take(5)
    override suspend fun getAllTransactions(accountId: String): List<TransactionEntity> = transactions.filter { it.accountId == accountId }
    override suspend fun getAllTransactionsForAccounts(accountIds: List<String>): List<TransactionEntity> = transactions.filter { it.accountId in accountIds }
    override suspend fun insertTransactions(transactions: List<TransactionEntity>) { this.transactions.addAll(transactions) }
    override suspend fun deleteTransactionsForAccount(accountId: String) { transactions.removeAll { it.accountId == accountId } }
}

class FakeKeyValueSettings : KeyValueSettings {
    private val map = mutableMapOf<String, Any>()
    private val sandboxFlow = MutableStateFlow(true)
    override fun getString(key: String, defaultValue: String): String = map[key] as? String ?: defaultValue
    override fun setString(key: String, value: String) { map[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = map[key] as? Boolean ?: defaultValue
    override fun setBoolean(key: String, value: Boolean) {
        map[key] = value
        if (key == "use_sandbox") sandboxFlow.value = value
    }
    override fun getBooleanFlow(key: String, defaultValue: Boolean): Flow<Boolean> = if (key == "use_sandbox") sandboxFlow else flowOf(getBoolean(key, defaultValue))
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var accountDao: FakeBankAccountDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var settings: FakeKeyValueSettings
    private lateinit var repository: BankRepository

    private val sampleAccount = BankAccountEntity(
        accountId = "acc_1",
        accountNumber = "1001234567",
        accountName = "Private Bank Account",
        referenceName = "My Checking",
        productName = "Investec Private Bank",
        kycCompliant = true,
        profileId = "prof_1",
        profileName = "Personal",
        currentBalance = 50000.0,
        availableBalance = 48500.0,
        currency = "ZAR"
    )

    private val sampleTxDebit = TransactionEntity(
        id = 1L,
        accountId = "acc_1",
        type = "DEBIT",
        transactionType = "Card Purchase",
        status = "POSTED",
        description = "Woolworths Food",
        amount = 450.50,
        runningBalance = 48500.0,
        postingDate = "2026-08-01",
        transactionDate = "2026-08-01",
        uuid = "tx-uuid-1"
    )

    private val sampleTxCredit = TransactionEntity(
        id = 2L,
        accountId = "acc_1",
        type = "CREDIT",
        transactionType = "EFT Deposit",
        status = "POSTED",
        description = "Salary Deposit",
        amount = 15000.0,
        runningBalance = 48950.50,
        postingDate = "2026-08-01",
        transactionDate = "2026-08-01",
        uuid = "tx-uuid-2"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountDao = FakeBankAccountDao()
        transactionDao = FakeTransactionDao()
        settings = FakeKeyValueSettings()

        runTest {
            accountDao.insertAccounts(listOf(sampleAccount))
            transactionDao.insertTransactions(listOf(sampleTxDebit, sampleTxCredit))
        }

        repository = BankRepository(accountDao, transactionDao, settings)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateLoadsTransactions() = runTest {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = TransactionsViewModel(repository, settings, context)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.transactions.size)
        assertEquals(15000.0, state.totalIncome, 0.01)
        assertEquals(450.50, state.totalExpenses, 0.01)
        assertTrue(state.useSandbox)
    }

    @Test
    fun testFilterDebitTransactions() = runTest {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = TransactionsViewModel(repository, settings, context)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        viewModel.setFilter(TransactionFilter.DEBIT)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TransactionFilter.DEBIT, state.filter)
        assertEquals(1, state.transactions.size)
        assertEquals("Woolworths Food", state.transactions.first().description)
    }

    @Test
    fun testFilterCreditTransactions() = runTest {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = TransactionsViewModel(repository, settings, context)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        viewModel.setFilter(TransactionFilter.CREDIT)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TransactionFilter.CREDIT, state.filter)
        assertEquals(1, state.transactions.size)
        assertEquals("Salary Deposit", state.transactions.first().description)
    }

    @Test
    fun testSearchQueryFiltering() = runTest {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = TransactionsViewModel(repository, settings, context)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        viewModel.setSearchQuery("Salary")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.transactions.size)
        assertEquals("Salary Deposit", state.transactions.first().description)
    }

    @Test
    fun testToggleSandboxMode() = runTest {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = TransactionsViewModel(repository, settings, context)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.useSandbox)

        viewModel.setUseSandbox(false)
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.useSandbox)
        assertEquals("Switched to Secure API Mode", viewModel.uiState.value.connectionStatusMessage)
    }

    @Test
    fun testCredentialsStateAndMasking() = runTest {
        settings.setString("client_id", "my_secret_client_id_12345")
        settings.setString("client_secret", "my_secret_key")
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val viewModel = TransactionsViewModel(repository, settings, context)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasCredentials)
        assertEquals("my_s...2345", state.clientIdMasked)
    }
}
