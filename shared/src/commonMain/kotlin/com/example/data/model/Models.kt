package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey val accountId: String,
    val accountNumber: String,
    val accountName: String,
    val referenceName: String,
    val productName: String,
    val kycCompliant: Boolean,
    val profileId: String,
    val profileName: String,
    val currentBalance: Double,
    val availableBalance: Double,
    val currency: String,
    val lastUpdated: Long = 0L
)

@Serializable
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["accountId", "postingDate"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val type: String, // CREDIT or DEBIT
    val transactionType: String,
    val status: String,
    val description: String,
    val amount: Double,
    val runningBalance: Double,
    val postingDate: String?,
    val transactionDate: String?,
    val uuid: String?
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long,
    val scope: String? = null
)

@Serializable
data class ApiResponseWrapper<T>(
    val data: T
)

@Serializable
data class AccountsData(
    val accounts: List<ApiAccount>
)

@Serializable
data class ApiAccount(
    val accountId: String,
    val accountNumber: String,
    val accountName: String,
    val referenceName: String,
    val productName: String,
    val kycCompliant: Boolean,
    val profileId: String,
    val profileName: String
)

@Serializable
data class ApiBalance(
    val accountId: String,
    val currentBalance: Double,
    val availableBalance: Double,
    val budgetBalance: Double? = null,
    val straightBalance: Double? = null,
    val cashBalance: Double? = null,
    val currency: String
)

@Serializable
data class TransactionsData(
    val transactions: List<ApiTransaction>
)

@Serializable
data class ApiTransaction(
    val accountId: String? = null,
    val type: String,
    val transactionType: String? = null,
    val status: String,
    val description: String,
    val amount: Double,
    val runningBalance: Double? = null,
    val postingDate: String? = null,
    val transactionDate: String? = null,
    val uuid: String? = null
)

@Serializable
data class BeneficiariesData(
    val beneficiaries: List<ApiBeneficiary>
)

@Serializable
data class ApiBeneficiary(
    val beneficiaryId: String,
    val accountNumber: String,
    val code: String,
    val bank: String,
    val beneficiaryName: String,
    val lastPaymentAmount: Double? = null,
    val lastPaymentDate: String? = null
)

@Serializable
data class CardsData(
    val cards: List<ApiCard>
)

@Serializable
data class ApiCard(
    val cardId: String,
    val cardNumber: String,
    val status: String,
    val cardType: String,
    val brand: String
)

@Serializable
data class PaymentRequest(
    val paymentList: List<PaymentItem>
)

@Serializable
data class PaymentItem(
    val beneficiaryId: String,
    val amount: String,
    val myReference: String,
    val theirReference: String
)

@Serializable
data class PaymentResponse(
    val transferList: List<PaymentResult>? = null,
    val paymentList: List<PaymentResult>? = null
)

@Serializable
data class PaymentResult(
    val paymentId: String? = null,
    val status: String? = null,
    val message: String? = null
)

@Serializable
data class TransferRequest(
    val transferList: List<TransferItem>
)

@Serializable
data class TransferItem(
    val beneficiaryAccountId: String,
    val amount: String,
    val myReference: String,
    val theirReference: String
)

@Serializable
data class TransferResponse(
    val transferList: List<TransferResult>? = null
)

@Serializable
data class TransferResult(
    val transferId: String? = null,
    val status: String? = null,
    val message: String? = null
)
