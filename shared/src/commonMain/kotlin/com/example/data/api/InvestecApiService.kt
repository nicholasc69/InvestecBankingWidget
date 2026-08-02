package com.example.data.api

import com.example.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class InvestecApiService(private val client: HttpClient) {

    suspend fun getAccessToken(
        basicAuthHeader: String,
        apiKey: String
    ): TokenResponse {
        return client.post("identity/v2/oauth2/token") {
            header("Authorization", basicAuthHeader)
            header("x-api-key", apiKey)
            setBody(FormDataContent(Parameters.build {
                append("grant_type", "client_credentials")
            }))
        }.body()
    }

    suspend fun getAccounts(
        bearerToken: String,
        apiKey: String
    ): ApiResponseWrapper<AccountsData> {
        return client.get("za/pb/v1/accounts") {
            header("Authorization", bearerToken)
            header("x-api-key", apiKey)
        }.body()
    }

    suspend fun getAccountBalance(
        bearerToken: String,
        apiKey: String,
        accountId: String
    ): ApiResponseWrapper<ApiBalance> {
        return client.get("za/pb/v1/accounts/$accountId/balance") {
            header("Authorization", bearerToken)
            header("x-api-key", apiKey)
        }.body()
    }

    suspend fun getAccountTransactions(
        bearerToken: String,
        apiKey: String,
        accountId: String,
        fromDate: String? = null,
        toDate: String? = null,
        includePending: Boolean = true
    ): ApiResponseWrapper<TransactionsData> {
        return client.get("za/pb/v1/accounts/$accountId/transactions") {
            header("Authorization", bearerToken)
            header("x-api-key", apiKey)
            parameter("fromDate", fromDate)
            parameter("toDate", toDate)
            parameter("includePending", includePending)
        }.body()
    }

    suspend fun getBeneficiaries(
        bearerToken: String,
        apiKey: String
    ): ApiResponseWrapper<BeneficiariesData> {
        return client.get("za/pb/v1/accounts/beneficiaries") {
            header("Authorization", bearerToken)
            header("x-api-key", apiKey)
        }.body()
    }

    suspend fun payBeneficiary(
        bearerToken: String,
        apiKey: String,
        accountId: String,
        request: PaymentRequest
    ): ApiResponseWrapper<PaymentResponse> {
        return client.post("za/pb/v1/accounts/$accountId/paymultiple") {
            header("Authorization", bearerToken)
            header("x-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun transferFunds(
        bearerToken: String,
        apiKey: String,
        accountId: String,
        request: TransferRequest
    ): ApiResponseWrapper<TransferResponse> {
        return client.post("za/pb/v1/accounts/$accountId/transfermultiple") {
            header("Authorization", bearerToken)
            header("x-api-key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getCards(
        bearerToken: String,
        apiKey: String
    ): ApiResponseWrapper<CardsData> {
        return client.get("za/v1/cards") {
            header("Authorization", bearerToken)
            header("x-api-key", apiKey)
        }.body()
    }
}

object InvestecApiClient {
    fun getService(baseUrl: String): InvestecApiService {
        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    prettyPrint = true
                })
            }
            install(io.ktor.client.plugins.HttpTimeout) {
                requestTimeoutMillis = 3000
                connectTimeoutMillis = 3000
                socketTimeoutMillis = 3000
            }
            defaultRequest {
                url(sanitizedBaseUrl)
            }
        }
        return InvestecApiService(client)
    }
}
