package com.example.data.ai

import com.example.data.repository.BankRepository
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking

class BankingToolSet(val repository: BankRepository) : ToolSet {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    @Tool(description = "Get a list of all bank accounts including their balances and available funds.")
    fun getAllAccounts(): String = runBlocking {
        val allAccounts = repository.getAccounts()
        val selectedProfileId = repository.getSelectedProfileId()
        val profileAccounts = if (!selectedProfileId.isNullOrBlank()) {
            allAccounts.filter { it.profileId == selectedProfileId }
        } else {
            allAccounts
        }
        val accounts = if (profileAccounts.isNotEmpty()) profileAccounts else allAccounts
        json.encodeToString(accounts)
    }

    @Tool(description = "Get the 5 most recent transactions for a specific account or ALL accounts.")
    fun getRecentTransactions(
        @ToolParam(description = "The unique ID of the account, or 'ALL' to retrieve recent transactions across all accounts.")
        accountId: String
    ): String = runBlocking {
        val allAccounts = repository.getAccounts()
        val selectedProfileId = repository.getSelectedProfileId()
        val profileAccounts = if (!selectedProfileId.isNullOrBlank()) {
            allAccounts.filter { it.profileId == selectedProfileId }
        } else {
            allAccounts
        }
        val activeAccounts = if (profileAccounts.isNotEmpty()) profileAccounts else allAccounts
        val activeAccountIds = activeAccounts.map { it.accountId }

        val txs = if (accountId.equals("ALL", ignoreCase = true) || accountId.isBlank()) {
            if (activeAccountIds.isNotEmpty()) {
                repository.getLastFiveTransactionsForAccounts(activeAccountIds)
            } else {
                emptyList()
            }
        } else {
            if (activeAccountIds.contains(accountId)) {
                repository.getLastFiveTransactions(accountId)
            } else {
                emptyList()
            }
        }
        json.encodeToString(txs)
    }

    @Tool(description = "Get all transactions for a specific account or ALL accounts.")
    fun getAllTransactions(
        @ToolParam(description = "The unique ID of the account, or 'ALL' to retrieve transactions across all accounts.")
        accountId: String
    ): String = runBlocking {
        val allAccounts = repository.getAccounts()
        val selectedProfileId = repository.getSelectedProfileId()
        val profileAccounts = if (!selectedProfileId.isNullOrBlank()) {
            allAccounts.filter { it.profileId == selectedProfileId }
        } else {
            allAccounts
        }
        val activeAccounts = if (profileAccounts.isNotEmpty()) profileAccounts else allAccounts
        val activeAccountIds = activeAccounts.map { it.accountId }

        val txs = if (accountId.equals("ALL", ignoreCase = true) || accountId.isBlank()) {
            if (activeAccountIds.isNotEmpty()) {
                repository.getAllTransactionsForAccounts(activeAccountIds)
            } else {
                emptyList()
            }
        } else {
            if (activeAccountIds.contains(accountId)) {
                repository.getAllTransactions(accountId)
            } else {
                emptyList()
            }
        }
        json.encodeToString(txs)
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