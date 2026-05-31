package com.example.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.data.api.InvestecApiService
import com.example.data.local.BankAccountDao
import com.example.data.local.TransactionDao
import com.example.data.repository.BankRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideBankRepository(
        @ApplicationContext context: Context,
        accountDao: BankAccountDao,
        transactionDao: TransactionDao,
        apiService: InvestecApiService,
        dataStore: DataStore<Preferences>,
        encryptedPrefs: SharedPreferences
    ): BankRepository {
        return BankRepository(context, accountDao, transactionDao, apiService, dataStore, encryptedPrefs)
    }
}
