package com.example.data.local

import androidx.room.*
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY productName ASC, accountName ASC")
    fun getAccountsFlow(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE accountId = :accountId")
    suspend fun getAccountById(accountId: String): BankAccountEntity?

    @Query("SELECT * FROM bank_accounts WHERE accountId = :accountId")
    fun getAccountByIdFlow(accountId: String): Flow<BankAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<BankAccountEntity>)

    @Query("DELETE FROM bank_accounts")
    suspend fun clearAccounts()

    @Transaction
    suspend fun replaceAccounts(accounts: List<BankAccountEntity>) {
        clearAccounts()
        insertAccounts(accounts)
    }
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY postingDate DESC, id DESC LIMIT 5")
    fun getLastFiveTransactionsFlow(accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY postingDate DESC, id DESC LIMIT 5")
    suspend fun getLastFiveTransactions(accountId: String): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsForAccount(accountId: String)

    @Transaction
    suspend fun replaceTransactionsForAccount(accountId: String, transactions: List<TransactionEntity>) {
        deleteTransactionsForAccount(accountId)
        insertTransactions(transactions)
    }
}

@Database(entities = [BankAccountEntity::class, TransactionEntity::class], version = 1, exportSchema = false)
abstract class BankDatabase : RoomDatabase() {
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: BankDatabase? = null

        fun getDatabase(context: android.content.Context): BankDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BankDatabase::class.java,
                    "bank_tracker_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
