package com.example.data.ai

import android.util.Log
import com.example.data.model.BankAccountEntity
import com.example.data.repository.BankRepository
import com.example.data.model.TransactionEntity
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import kotlinx.coroutines.flow.first

class BankingToolSet(val repository : BankRepository) : ToolSet {

    @Tool(description = "Returns a list of all bank accounts and their current details " +
            "(accountId, accountName, availableBalance, currency, etc.).")
    suspend fun getAllAccounts() : List<BankAccountEntity> {
        Log.d("BankingToolSet", "getAllAccounts called")
        val accounts = repository.getAccountsFlow().first()
        Log.d("BankingToolSet", "getAllAccounts returning ${accounts.size} accounts")
        return accounts
    }

    @Tool(description = "Returns the 5 most recent transactions for a specific account.")
    suspend fun getRecentTransactions(
        @ToolParam(description = "The unique accountId of the account to fetch transactions for.")
        accountId: String
    ): List<TransactionEntity> {
        Log.d("BankingToolSet", "getRecentTransactions called for accountId: $accountId")
        val transactions = repository.getLastFiveTransactions(accountId)
        Log.d("BankingToolSet", "getRecentTransactions returning ${transactions.size} transactions")
        return transactions
    }

    @Tool(description = "Triggers a synchronization of banking data from the Investec API to update balances and transactions. " +
            "Use this if the user asks for the latest or most recent information.")
    suspend fun syncBankingData(): String {
        Log.d("BankingToolSet", "syncBankingData called")
        return try {
            val result = repository.syncData()
            if (result.isSuccess) {
                Log.d("BankingToolSet", "syncBankingData success")
                "Banking data synchronized successfully."
            } else {
                val error = result.exceptionOrNull()?.message
                Log.e("BankingToolSet", "syncBankingData failed: $error")
                "Failed to synchronize data: $error"
            }
        } catch (e: Exception) {
            Log.e("BankingToolSet", "syncBankingData exception", e)
            "Error during synchronization: ${e.message}"
        }
    }
}