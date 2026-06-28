package com.example

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.BankingToolSet
import com.example.data.local.BankDatabase
import com.example.data.model.BankAccountEntity
import com.example.data.repository.BankRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BankingToolSetTest {

    private lateinit var db: BankDatabase
    private lateinit var repository: BankRepository
    private lateinit var toolSet: BankingToolSet
    private lateinit var tempFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BankDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        tempFile = File(context.filesDir, "datastore/test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create {
            tempFile
        }
        val encryptedPrefs = context.getSharedPreferences("test_encrypted_prefs", Context.MODE_PRIVATE)

        repository = BankRepository(
            context = context,
            accountDao = db.bankAccountDao(),
            transactionDao = db.transactionDao(),
            dataStore = dataStore,
            encryptedPrefs = encryptedPrefs
        )

        toolSet = BankingToolSet(repository)
    }

    @After
    fun tearDown() {
        db.close()
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    @Test
    fun testGetAllAccountsEmpty() = runTest {
        val result = toolSet.getAllAccounts()
        assertNotNull(result)
        assertEquals("[]", result)
    }

    @Test
    fun testGetAllAccountsWithData() = runTest {
        val account = BankAccountEntity(
            accountId = "acc-123",
            accountNumber = "1234567890",
            accountName = "Test Account",
            referenceName = "Ref",
            productName = "Private Savings",
            kycCompliant = true,
            profileId = "prof-1",
            profileName = "Nick",
            currentBalance = 1000.0,
            availableBalance = 950.0,
            currency = "ZAR"
        )
        db.bankAccountDao().insertAccounts(listOf(account))

        val result = toolSet.getAllAccounts()
        assertNotNull(result)
        assertTrue(result.contains("acc-123"))
        assertTrue(result.contains("Test Account"))
        assertTrue(result.contains("1000.0"))
    }

    @Test
    fun testGetAllAccountsFilteredBySelectedProfile() = runTest {
        val account1 = BankAccountEntity(
            accountId = "acc-1",
            accountNumber = "123",
            accountName = "Account 1",
            referenceName = "Ref",
            productName = "Savings",
            kycCompliant = true,
            profileId = "prof-1",
            profileName = "Nick",
            currentBalance = 1000.0,
            availableBalance = 950.0,
            currency = "ZAR"
        )
        val account2 = BankAccountEntity(
            accountId = "acc-2",
            accountNumber = "456",
            accountName = "Account 2",
            referenceName = "Ref",
            productName = "Savings",
            kycCompliant = true,
            profileId = "prof-2",
            profileName = "John",
            currentBalance = 2000.0,
            availableBalance = 1950.0,
            currency = "ZAR"
        )
        db.bankAccountDao().insertAccounts(listOf(account1, account2))

        // Set selected profile to prof-1
        repository.setSelectedProfileId("prof-1")
        val result1 = toolSet.getAllAccounts()
        assertNotNull(result1)
        assertTrue(result1.contains("acc-1"))
        assertTrue(!result1.contains("acc-2"))

        // Set selected profile to prof-2
        repository.setSelectedProfileId("prof-2")
        val result2 = toolSet.getAllAccounts()
        assertNotNull(result2)
        assertTrue(!result2.contains("acc-1"))
        assertTrue(result2.contains("acc-2"))
    }

    @Test
    fun testGetRecentAndAllTransactionsFilteredByProfile() = runTest {
        val account1 = BankAccountEntity(
            accountId = "acc-1",
            accountNumber = "123",
            accountName = "Account 1",
            referenceName = "Ref",
            productName = "Savings",
            kycCompliant = true,
            profileId = "prof-1",
            profileName = "Nick",
            currentBalance = 1000.0,
            availableBalance = 950.0,
            currency = "ZAR"
        )
        val account2 = BankAccountEntity(
            accountId = "acc-2",
            accountNumber = "456",
            accountName = "Account 2",
            referenceName = "Ref",
            productName = "Savings",
            kycCompliant = true,
            profileId = "prof-2",
            profileName = "John",
            currentBalance = 2000.0,
            availableBalance = 1950.0,
            currency = "ZAR"
        )
        db.bankAccountDao().insertAccounts(listOf(account1, account2))

        val tx1 = com.example.data.model.TransactionEntity(
            accountId = "acc-1",
            type = "DEBIT",
            transactionType = "Card",
            status = "Posted",
            description = "Coffee",
            amount = 50.0,
            runningBalance = 950.0,
            postingDate = "2026-06-20",
            transactionDate = "2026-06-20",
            uuid = "uuid-1"
        )
        val tx2 = com.example.data.model.TransactionEntity(
            accountId = "acc-2",
            type = "DEBIT",
            transactionType = "Card",
            status = "Posted",
            description = "Book",
            amount = 100.0,
            runningBalance = 1850.0,
            postingDate = "2026-06-20",
            transactionDate = "2026-06-20",
            uuid = "uuid-2"
        )
        db.transactionDao().insertTransactions(listOf(tx1, tx2))

        // Profile 1
        repository.setSelectedProfileId("prof-1")
        val txs1 = toolSet.getRecentTransactions("ALL")
        assertTrue(txs1.contains("Coffee"))
        assertTrue(!txs1.contains("Book"))

        val allTxs1 = toolSet.getAllTransactions("ALL")
        assertTrue(allTxs1.contains("Coffee"))
        assertTrue(!allTxs1.contains("Book"))

        // Profile 2
        repository.setSelectedProfileId("prof-2")
        val txs2 = toolSet.getRecentTransactions("ALL")
        assertTrue(!txs2.contains("Coffee"))
        assertTrue(txs2.contains("Book"))

        val allTxs2 = toolSet.getAllTransactions("ALL")
        assertTrue(!allTxs2.contains("Coffee"))
        assertTrue(allTxs2.contains("Book"))

        // Requesting non-profile accountId explicitly should return empty
        val txsSpecificBlocked = toolSet.getRecentTransactions("acc-1")
        assertTrue(!txsSpecificBlocked.contains("Coffee"))
        assertTrue(!txsSpecificBlocked.contains("Book"))
    }

    @Test
    fun testMethodSignaturesForReflection() {
        val methods = BankingToolSet::class.java.declaredMethods
        
        val getAllAccountsMethod = methods.firstOrNull { it.name == "getAllAccounts" }
        assertNotNull(getAllAccountsMethod)
        assertEquals(0, getAllAccountsMethod!!.parameterCount)

        val getRecentTransactionsMethod = methods.firstOrNull { it.name == "getRecentTransactions" }
        assertNotNull(getRecentTransactionsMethod)
        assertEquals(1, getRecentTransactionsMethod!!.parameterCount)
        assertEquals(String::class.java, getRecentTransactionsMethod.parameterTypes[0])

        val getAllTransactionsMethod = methods.firstOrNull { it.name == "getAllTransactions" }
        assertNotNull(getAllTransactionsMethod)
        assertEquals(1, getAllTransactionsMethod!!.parameterCount)
        assertEquals(String::class.java, getAllTransactionsMethod.parameterTypes[0])

        val syncBankingDataMethod = methods.firstOrNull { it.name == "syncBankingData" }
        assertNotNull(syncBankingDataMethod)
        assertEquals(0, syncBankingDataMethod!!.parameterCount)
    }
}
