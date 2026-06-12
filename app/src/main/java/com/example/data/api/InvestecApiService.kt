package com.example.data.api

import com.example.data.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface InvestecApiService {

    @FormUrlEncoded
    @POST("identity/v2/oauth2/token")
    suspend fun getAccessToken(
        @Header("Authorization") basicAuthHeader: String,
        @Header("x-api-key") apiKey: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): TokenResponse

    @GET("za/pb/v1/accounts")
    suspend fun getAccounts(
        @Header("Authorization") bearerToken: String,
        @Header("x-api-key") apiKey: String
    ): ApiResponseWrapper<AccountsData>

    @GET("za/pb/v1/accounts/{accountId}/balance")
    suspend fun getAccountBalance(
        @Header("Authorization") bearerToken: String,
        @Header("x-api-key") apiKey: String,
        @Path("accountId") accountId: String
    ): ApiResponseWrapper<ApiBalance>

    @GET("za/pb/v1/accounts/{accountId}/transactions")
    suspend fun getAccountTransactions(
        @Header("Authorization") bearerToken: String,
        @Header("x-api-key") apiKey: String,
        @Path("accountId") accountId: String,
        @Query("includePending") includePending: Boolean = true
    ): ApiResponseWrapper<TransactionsData>
}

object InvestecApiClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getService(baseUrl: String): InvestecApiService {
        val sanitizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(sanitizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(InvestecApiService::class.java)
    }
}
