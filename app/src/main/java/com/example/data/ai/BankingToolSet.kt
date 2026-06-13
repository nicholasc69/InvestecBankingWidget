package com.example.data.ai

import android.util.Log
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.ApiBeneficiary
import com.example.data.model.ApiCard
import com.example.data.model.PaymentResponse
import com.example.data.model.TransferResponse
import com.example.data.repository.BankRepository
import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.runBlocking

class BankingToolSet(val repository: BankRepository) : ToolSet {

    private val moshi = Moshi.Builder().build()

    private val accountsAdapter = moshi.adapter<List<BankAccountEntity>>(
        Types.newParameterizedType(List::class.java, BankAccountEntity::class.java)
    )

    private val transactionsAdapter = moshi.adapter<List<TransactionEntity>>(
        Types.newParameterizedType(List::class.java, TransactionEntity::class.java)
    )

    private val beneficiariesAdapter = moshi.adapter<List<ApiBeneficiary>>(
        Types.newParameterizedType(List::class.java, ApiBeneficiary::class.java)
    )

    private val cardsAdapter = moshi.adapter<List<ApiCard>>(
        Types.newParameterizedType(List::class.java, ApiCard::class.java)
    )

    private val paymentResponseAdapter = moshi.adapter(PaymentResponse::class.java)
    private val transferResponseAdapter = moshi.adapter(TransferResponse::class.java)

    @Tool(
        description = "Returns a conversational list of all bank accounts and their current balances. " +
                "Use this to check the user's balances or list of available accounts. Remove all * characters from the output"
    )
    fun getAllAccounts(): String {
        Log.d("BankingToolSet", "getAllAccounts called")
        return try {
            val allAccounts = runBlocking { repository.getAccounts() }
            val selectedProfileId = repository.getSelectedProfileId()
            val accounts = if (selectedProfileId != null) {
                allAccounts.filter { it.profileId == selectedProfileId }
            } else {
                allAccounts
            }

            if (accounts.isEmpty()) {
                "No bank accounts were found on the active profile. Try synchronizing your data first."
            } else {
                val sb = java.lang.StringBuilder("You have the following accounts under the active profile:\n")
                accounts.forEach { acc ->
                    sb.append("- ${acc.accountName} (${acc.productName}): Account No: ${acc.accountNumber}, Available Balance: ${acc.currency} ${acc.availableBalance} (Current Balance: ${acc.currency} ${acc.currentBalance})\n")
                }
                sb.toString()
            }
        } catch (e: Exception) {
            Log.e("BankingToolSet", "getAllAccounts failed", e)
            "I couldn't retrieve your accounts list. Investec reported: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    @Tool(
        description = "Returns a conversational list of the 5 most recent transactions. " +
                "To get transactions across all accounts, or if the user doesn't specify an account, use 'ALL' as the accountId. " +
                "Otherwise, specify a specific accountId to fetch transactions for that account only."
    )
    fun getRecentTransactions(
        @ToolParam(description = "The unique accountId of the account, or 'ALL' to fetch recent transactions across all accounts belonging to the active profile.")
        accountId: String
    ): String {
        Log.d("BankingToolSet", "getRecentTransactions called for accountId: $accountId")
        return try {
            val allAccounts = runBlocking { repository.getAccounts() }
            val selectedProfileId = repository.getSelectedProfileId()
            val accounts = if (selectedProfileId != null) {
                allAccounts.filter { it.profileId == selectedProfileId }
            } else {
                allAccounts
            }

            if (accountId.equals("ALL", ignoreCase = true) || accountId.isBlank()) {
                val accountIds = accounts.map { it.accountId }
                if (accountIds.isEmpty()) {
                    return "No bank accounts were found on the active profile. Try synchronizing your data first."
                }
                val transactions = runBlocking { repository.getLastFiveTransactionsForAccounts(accountIds) }
                return if (transactions.isEmpty()) {
                    "No recent transactions were found across any of your accounts under the active profile."
                } else {
                    val sb = java.lang.StringBuilder("The 5 most recent transactions across all your accounts are:\n")
                    transactions.forEachIndexed { index, tx ->
                        val account = accounts.find { it.accountId == tx.accountId }
                        val accountName = account?.accountName ?: "Unknown Account"
                        val currency = account?.currency ?: "ZAR"
                        val dateStr = tx.transactionDate ?: tx.postingDate ?: "Unknown Date"
                        sb.append("${index + 1}. [Account: $accountName] ${tx.type} of $currency ${tx.amount} on $dateStr: '${tx.description}' [Type: ${tx.transactionType}, Status: ${tx.status}]\n")
                    }
                    sb.toString()
                }
            }

            val account = accounts.find { it.accountId == accountId }
            if (account == null) {
                return "Error: Account '$accountId' was not found or does not belong to the active profile."
            }
            val accountName = account.accountName

            val transactions = runBlocking { repository.getLastFiveTransactions(accountId) }
            if (transactions.isEmpty()) {
                "No recent transactions were found for your account '$accountName'."
            } else {
                val sb = java.lang.StringBuilder("The 5 most recent transactions for your account '$accountName' are:\n")
                transactions.forEachIndexed { index, tx ->
                    val dateStr = tx.transactionDate ?: tx.postingDate ?: "Unknown Date"
                    sb.append("${index + 1}. ${tx.type} of ${account.currency} ${tx.amount} on $dateStr: '${tx.description}' [Type: ${tx.transactionType}, Status: ${tx.status}]\n")
                }
                sb.toString()
            }
        } catch (e: Exception) {
            Log.e("BankingToolSet", "getRecentTransactions failed", e)
            "I couldn't fetch the transactions. Investec reported: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    @Tool(
        description = "Returns a conversational list of all transactions. " +
                "To get all transactions across all accounts, or if the user doesn't specify an account, use 'ALL' as the accountId. " +
                "Otherwise, specify a specific accountId to fetch all transactions for that account."
    )
    fun getAllTransactions(
        @ToolParam(description = "The unique accountId of the account, or 'ALL' to fetch all transactions across all accounts belonging to the active profile.")
        accountId: String
    ): String {
        Log.d("BankingToolSet", "getAllTransactions called for accountId: $accountId")
        return try {
            val allAccounts = runBlocking { repository.getAccounts() }
            val selectedProfileId = repository.getSelectedProfileId()
            val accounts = if (selectedProfileId != null) {
                allAccounts.filter { it.profileId == selectedProfileId }
            } else {
                allAccounts
            }

            if (accountId.equals("ALL", ignoreCase = true) || accountId.isBlank()) {
                val accountIds = accounts.map { it.accountId }
                if (accountIds.isEmpty()) {
                    return "No bank accounts were found on the active profile. Try synchronizing your data first."
                }
                val transactions = runBlocking { repository.getAllTransactionsForAccounts(accountIds) }
                return if (transactions.isEmpty()) {
                    "No transactions were found across any of your accounts under the active profile."
                } else {
                    val sb = java.lang.StringBuilder("All transactions across all your accounts are:\n")
                    transactions.forEachIndexed { index, tx ->
                        val account = accounts.find { it.accountId == tx.accountId }
                        val accountName = account?.accountName ?: "Unknown Account"
                        val currency = account?.currency ?: "ZAR"
                        val dateStr = tx.transactionDate ?: tx.postingDate ?: "Unknown Date"
                        sb.append("${index + 1}. [Account: $accountName] ${tx.type} of $currency ${tx.amount} on $dateStr: '${tx.description}' [Type: ${tx.transactionType}, Status: ${tx.status}]\n")
                    }
                    sb.toString()
                }
            }

            val account = accounts.find { it.accountId == accountId }
            if (account == null) {
                return "Error: Account '$accountId' was not found or does not belong to the active profile."
            }
            val accountName = account.accountName

            val transactions = runBlocking { repository.getAllTransactions(accountId) }
            if (transactions.isEmpty()) {
                "No transactions were found for your account '$accountName'."
            } else {
                val sb = java.lang.StringBuilder("All transactions for your account '$accountName' are:\n")
                transactions.forEachIndexed { index, tx ->
                    val dateStr = tx.transactionDate ?: tx.postingDate ?: "Unknown Date"
                    sb.append("${index + 1}. ${tx.type} of ${account.currency} ${tx.amount} on $dateStr: '${tx.description}' [Type: ${tx.transactionType}, Status: ${tx.status}]\n")
                }
                sb.toString()
            }
        } catch (e: Exception) {
            Log.e("BankingToolSet", "getAllTransactions failed", e)
            "I couldn't fetch the transactions. Investec reported: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    @Tool(
        description = "Triggers a synchronization of banking data from the Investec API to update balances and transactions. " +
                "Use this if the user asks for the latest or most recent information."
    )
    fun syncBankingData(): String {
        Log.d("BankingToolSet", "syncBankingData called")
        return try {
            val result = runBlocking { repository.syncData() }
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

    @Tool(
        description = "Returns a conversational list of all pre-approved beneficiaries saved in the user's profile."
    )
    fun getBeneficiaries(): String {
        Log.d("BankingToolSet", "getBeneficiaries called")
        return try {
            val beneficiaries = runBlocking { repository.getBeneficiaries() }
            if (beneficiaries.isEmpty()) {
                "You don't have any pre-approved beneficiaries saved on your profile yet. " +
                "You can add them via your online banking portal, and then synchronize data."
            } else {
                val sb = java.lang.StringBuilder("You have the following pre-approved beneficiaries saved on your profile:\n")
                beneficiaries.forEach { b ->
                    sb.append("- Name: ${b.beneficiaryName} (Bank: ${b.bank}, Account No: ${b.accountNumber}, Beneficiary ID: ${b.beneficiaryId})")
                    if (b.lastPaymentAmount != null) {
                        sb.append(", Last Payment Amount: ${b.lastPaymentAmount}")
                        if (b.lastPaymentDate != null) {
                            sb.append(" on ${b.lastPaymentDate}")
                        }
                    }
                    sb.append("\n")
                }
                sb.toString()
            }
        } catch (e: Exception) {
            Log.e("BankingToolSet", "getBeneficiaries failed", e)
            "I couldn't retrieve your beneficiaries. Investec reported: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    @Tool(
        description = "Executes a payment to a saved beneficiary. Note: The beneficiary must exist in the beneficiaries list and have been paid at least once via online banking."
    )
    fun payBeneficiary(
        @ToolParam(description = "The accountId to pay the money from.")
        sourceAccountId: String,
        @ToolParam(description = "The unique beneficiaryId of the saved beneficiary to pay.")
        beneficiaryId: String,
        @ToolParam(description = "The monetary amount to pay.")
        amount: Double,
        @ToolParam(description = "The payment reference showing on the statements.")
        reference: String
    ): String {
        Log.d("BankingToolSet", "payBeneficiary called. From: $sourceAccountId, To: $beneficiaryId, Amt: $amount, Ref: $reference")
        return try {
            val accounts = runBlocking { repository.getAccounts() }
            val selectedProfileId = repository.getSelectedProfileId()
            val profileAccounts = if (selectedProfileId != null) {
                accounts.filter { it.profileId == selectedProfileId }
            } else {
                accounts
            }

            val sourceAccount = profileAccounts.find { it.accountId == sourceAccountId }
            if (sourceAccount == null) {
                return "Error: Source account '$sourceAccountId' was not found or does not belong to the active profile."
            }
            val sourceName = sourceAccount.accountName

            // Look up beneficiary name
            val beneficiaries = try {
                runBlocking { repository.getBeneficiaries() }
            } catch (e: Exception) {
                emptyList()
            }
            val beneficiary = beneficiaries.find { it.beneficiaryId == beneficiaryId }
            val beneficiaryName = beneficiary?.beneficiaryName ?: "Beneficiary (ID: $beneficiaryId)"

            val response = runBlocking { repository.payBeneficiary(sourceAccountId, beneficiaryId, amount, reference) }
            
            // Check response status
            val firstResult = response.paymentList?.firstOrNull() ?: response.transferList?.firstOrNull()
            val status = firstResult?.status ?: "Submitted"
            val paymentId = firstResult?.paymentId ?: "N/A"
            val message = firstResult?.message ?: ""

            if (status.equals("Failed", ignoreCase = true)) {
                "Failed to process payment of R$amount to $beneficiaryName. Reason: ${message.ifBlank { "Rejected by bank" }}"
            } else {
                "Success: I have successfully paid R$amount to beneficiary '$beneficiaryName' from your account '$sourceName' with reference '$reference'. Status: $status. Reference ID: $paymentId."
            }
        } catch (e: Exception) {
            Log.e("BankingToolSet", "payBeneficiary failed", e)
            "I couldn't process the payment of R$amount. Investec reported: ${e.localizedMessage ?: "Unknown error"}. " +
            "Please verify that this beneficiary is pre-approved and has been paid at least once on the Investec online portal before making API payments."
        }
    }

    @Tool(
        description = "Transfers money between the user's own accounts (inter-account transfer)."
    )
    fun transferFunds(
        @ToolParam(description = "The accountId to transfer funds from.")
        sourceAccountId: String,
        @ToolParam(description = "The accountId (beneficiaryAccountId) to transfer funds to.")
        destinationAccountId: String,
        @ToolParam(description = "The monetary amount to transfer.")
        amount: Double,
        @ToolParam(description = "The transfer reference showing on the statements.")
        reference: String
    ): String {
        Log.d("BankingToolSet", "transferFunds called. From: $sourceAccountId, To: $destinationAccountId, Amt: $amount, Ref: $reference")
        return try {
            val accounts = runBlocking { repository.getAccounts() }
            val selectedProfileId = repository.getSelectedProfileId()
            val profileAccounts = if (selectedProfileId != null) {
                accounts.filter { it.profileId == selectedProfileId }
            } else {
                accounts
            }

            val sourceAccount = profileAccounts.find { it.accountId == sourceAccountId }
            val targetAccount = profileAccounts.find { it.accountId == destinationAccountId }
            
            if (sourceAccount == null) {
                return "Error: Source account '$sourceAccountId' was not found or does not belong to the active profile."
            }
            if (targetAccount == null) {
                return "Error: Destination account '$destinationAccountId' was not found or does not belong to the active profile."
            }
            
            val sourceName = sourceAccount.accountName
            val targetName = targetAccount.accountName

            val response = runBlocking { repository.transferFunds(sourceAccountId, destinationAccountId, amount, reference) }
            
            val firstResult = response.transferList?.firstOrNull()
            val status = firstResult?.status ?: "Completed"
            val transferId = firstResult?.transferId ?: "N/A"
            val message = firstResult?.message ?: ""

            if (status.equals("Failed", ignoreCase = true)) {
                "Failed to transfer R$amount from $sourceName to $targetName. Reason: ${message.ifBlank { "Rejected by bank" }}"
            } else {
                "Success: R$amount has been transferred from your account '$sourceName' to your account '$targetName' with reference '$reference'. Status: $status. Transfer ID: $transferId."
            }
        } catch (e: Exception) {
            Log.e("BankingToolSet", "transferFunds failed", e)
            "I couldn't complete the transfer of R$amount. Investec reported: ${e.localizedMessage ?: "Unknown error"}."
        }
    }

    @Tool(
        description = "Returns a conversational list of the user's active debit/credit cards."
    )
    fun getCards(): String {
        Log.d("BankingToolSet", "getCards called")
        return try {
            val cards = runBlocking { repository.getCards() }
            if (cards.isEmpty()) {
                "There are no active debit or credit cards linked to your profile."
            } else {
                val sb = java.lang.StringBuilder("You have the following active debit/credit cards:\n")
                cards.forEach { c ->
                    sb.append("- ${c.brand} ${c.cardType} Card: Number: ${c.cardNumber} [Status: ${c.status}, Card ID: ${c.cardId}]\n")
                }
                sb.toString()
            }
        } catch (e: Exception) {
            Log.e("BankingToolSet", "getCards failed", e)
            "I couldn't retrieve your cards. Investec reported: ${e.localizedMessage ?: "Unknown error"}"
        }
    }
}