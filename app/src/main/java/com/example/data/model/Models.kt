package com.example.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ==========================================
// ROOM ENTITIES (Local DB Caching)
// ==========================================

@Immutable
@Entity(tableName = "bank_accounts")
@JsonClass(generateAdapter = true)
data class BankAccountEntity(
    @PrimaryKey val accountId: String,
    val accountNumber: String,
    val accountName: String,
    val referenceName: String,
    val productName: String,
    val kycCompliant: Boolean,
    val profileId: String,
    val profileName: String,
    // Balance details merged into the account entity
    val currentBalance: Double,
    val availableBalance: Double,
    val currency: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Immutable
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["accountId", "postingDate"])]
)
@JsonClass(generateAdapter = true)
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

// ==========================================
// API RESPONSE MODELS (Moshi Serialization)
// ==========================================

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @param:Json(name = "access_token") val accessToken: String,
    @param:Json(name = "token_type") val tokenType: String,
    @param:Json(name = "expires_in") val expiresIn: Long,
    val scope: String?
)

@JsonClass(generateAdapter = true)
data class ApiResponseWrapper<T>(
    val data: T
)

// Accounts models
@JsonClass(generateAdapter = true)
data class AccountsData(
    val accounts: List<ApiAccount>
)

@JsonClass(generateAdapter = true)
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

// Balance models
@JsonClass(generateAdapter = true)
data class ApiBalance(
    val accountId: String,
    val currentBalance: Double,
    val availableBalance: Double,
    val budgetBalance: Double?,
    val straightBalance: Double?,
    val cashBalance: Double?,
    val currency: String
)

// Transactions models
@JsonClass(generateAdapter = true)
data class TransactionsData(
    val transactions: List<ApiTransaction>
)

@JsonClass(generateAdapter = true)
data class ApiTransaction(
    val accountId: String?,
    val type: String,
    val transactionType: String?,
    val status: String,
    val description: String,
    val amount: Double,
    val runningBalance: Double?,
    val postingDate: String?,
    val transactionDate: String?,
    val uuid: String?
)

// Beneficiaries models
@JsonClass(generateAdapter = true)
data class BeneficiariesData(
    val beneficiaries: List<ApiBeneficiary>
)

@JsonClass(generateAdapter = true)
data class ApiBeneficiary(
    val beneficiaryId: String,
    val accountNumber: String,
    val code: String,
    val bank: String,
    val beneficiaryName: String,
    val lastPaymentAmount: Double?,
    val lastPaymentDate: String?
)

// Cards models
@JsonClass(generateAdapter = true)
data class CardsData(
    val cards: List<ApiCard>
)

@JsonClass(generateAdapter = true)
data class ApiCard(
    val cardId: String,
    val cardNumber: String,
    val status: String,
    val cardType: String,
    val brand: String
)

// Payments models
@JsonClass(generateAdapter = true)
data class PaymentRequest(
    val paymentList: List<PaymentItem>
)

@JsonClass(generateAdapter = true)
data class PaymentItem(
    val beneficiaryId: String,
    val amount: String,
    val myReference: String,
    val theirReference: String
)

@JsonClass(generateAdapter = true)
data class PaymentResponse(
    val transferList: List<PaymentResult>? = null,
    val paymentList: List<PaymentResult>? = null
)

@JsonClass(generateAdapter = true)
data class PaymentResult(
    val paymentId: String?,
    val status: String?,
    val message: String?
)

// Transfers models
@JsonClass(generateAdapter = true)
data class TransferRequest(
    val transferList: List<TransferItem>
)

@JsonClass(generateAdapter = true)
data class TransferItem(
    val beneficiaryAccountId: String,
    val amount: String,
    val myReference: String,
    val theirReference: String
)

@JsonClass(generateAdapter = true)
data class TransferResponse(
    val transferList: List<TransferResult>? = null
)

@JsonClass(generateAdapter = true)
data class TransferResult(
    val transferId: String?,
    val status: String?,
    val message: String?
)
