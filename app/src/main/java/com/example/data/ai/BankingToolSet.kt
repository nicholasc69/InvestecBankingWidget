package com.example.data.ai

import com.example.data.repository.BankRepository
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.runBlocking

class BankingToolSet(val repository: BankRepository) : ToolSet {

    private val moshi = Moshi.Builder().build()

    @Tool(description = "Get a list of all bank accounts including their balances and available funds.")
    fun getAllAccounts(): String = runBlocking {
        val accounts = repository.getAccounts()
        val type = Types.newParameterizedType(List::class.java, BankAccountEntity::class.java)
        moshi.adapter<List<BankAccountEntity>>(type).toJson(accounts)
    }

    @Tool(description = "Get the 5 most recent transactions for a specific account or ALL accounts.")
    fun getRecentTransactions(
        @ToolParam(description = "The unique ID of the account, or 'ALL' to retrieve recent transactions across all accounts.")
        accountId: String
    ): String = runBlocking {
        val txs = if (accountId == "ALL") {
            val accounts = repository.getAccounts()
            repository.getLastFiveTransactionsForAccounts(accounts.map { it.accountId })
        } else {
            repository.getLastFiveTransactions(accountId)
        }
        val type = Types.newParameterizedType(List::class.java, TransactionEntity::class.java)
        moshi.adapter<List<TransactionEntity>>(type).toJson(txs)
    }

    @Tool(description = "Get all transactions for a specific account or ALL accounts.")
    fun getAllTransactions(
        @ToolParam(description = "The unique ID of the account, or 'ALL' to retrieve transactions across all accounts.")
        accountId: String
    ): String = runBlocking {
        val txs = if (accountId == "ALL") {
            val accounts = repository.getAccounts()
            repository.getAllTransactionsForAccounts(accounts.map { it.accountId })
        } else {
            repository.getAllTransactions(accountId)
        }
        val type = Types.newParameterizedType(List::class.java, TransactionEntity::class.java)
        moshi.adapter<List<TransactionEntity>>(type).toJson(txs)
    }

    @Tool(description = "Synchronize recent account balances and transaction data from the bank API.")
    fun syncBankingData(): String = runBlocking {
        val result = repository.syncData()
        if (result.isSuccess) {
            "Synchronization successful. All accounts and transactions updated."
        } else {
            "Synchronization failed: ${result.exceptionOrNull()?.message}"
        }
    }
}