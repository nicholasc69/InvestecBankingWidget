package com.example

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BankDatabase
import com.example.data.model.BankAccountEntity
import com.example.data.repository.BankRepository
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.dashboard.DashboardUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: BankDatabase
    private lateinit var settings: com.example.data.repository.KeyValueSettings
    private lateinit var repository: BankRepository
    private lateinit var tempFile: File

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BankDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        tempFile = File(context.filesDir, "datastore/test_dashboard_vm.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create {
            tempFile
        }
        val encryptedPrefs = context.getSharedPreferences("test_dashboard_encrypted_prefs", Context.MODE_PRIVATE)

        settings = com.example.di.AndroidKeyValueSettings(dataStore, encryptedPrefs)
        repository = BankRepository(
            accountDao = db.bankAccountDao(),
            transactionDao = db.transactionDao(),
            settings = settings
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    @Test
    fun testStaleProfileIdFallbackPopulatesData() = runTest {
        // Save a stale profile ID in repository settings
        repository.setSelectedProfileId("stale_old_profile_id")

        // Insert new accounts with a different profile ID into the database
        val newAccount = BankAccountEntity(
            accountId = "acc-new-1",
            accountNumber = "987654321",
            accountName = "New Production Account",
            referenceName = "ProdRef",
            productName = "Private Banking",
            kycCompliant = true,
            profileId = "prof-prod-new",
            profileName = "Production Profile",
            currentBalance = 50000.0,
            availableBalance = 48000.0,
            currency = "ZAR"
        )
        db.bankAccountDao().insertAccounts(listOf(newAccount))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = DashboardViewModel(repository, settings, context)

        // Await until uiState emits Success with resolved fallback profile accounts
        val state = viewModel.uiState.first { state ->
            state is DashboardUiState.Success && state.accounts.isNotEmpty()
        } as DashboardUiState.Success

        assertEquals(1, state.accounts.size)
        assertEquals("acc-new-1", state.accounts[0].accountId)
        assertEquals("prof-prod-new", state.selectedProfileId)
    }
}
