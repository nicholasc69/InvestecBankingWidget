package com.example.di

import com.example.data.api.InvestecApiClient
import com.example.data.api.InvestecApiService
import com.example.data.repository.BankRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideInvestecApiService(): InvestecApiService {
        // Initializing with Sandbox by default, but BankRepository handles dynamic switching
        return InvestecApiClient.getService(BankRepository.BASE_URL_SANDBOX)
    }
}
