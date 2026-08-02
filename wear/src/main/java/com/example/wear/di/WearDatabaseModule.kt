package com.example.wear.di

import android.content.Context
import com.example.data.local.BankAccountDao
import com.example.data.local.BankDatabase
import com.example.data.local.TransactionDao
import com.example.data.local.createDatabase
import com.example.data.local.getDatabaseBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WearDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BankDatabase {
        return createDatabase(getDatabaseBuilder(context))
    }

    @Provides
    fun provideBankAccountDao(database: BankDatabase): BankAccountDao {
        return database.bankAccountDao()
    }

    @Provides
    fun provideTransactionDao(database: BankDatabase): TransactionDao {
        return database.transactionDao()
    }
}
