package com.example.data.repository

import com.example.data.local.createDatabase
import com.example.data.local.getDatabaseBuilder

object IOSBankRepositoryFactory {
    fun create(): BankRepository {
        val dbBuilder = getDatabaseBuilder()
        val database = createDatabase(dbBuilder)
        val settings = IOSKeyValueSettings()
        return BankRepository(
            accountDao = database.bankAccountDao(),
            transactionDao = database.transactionDao(),
            settings = settings
        )
    }
}
