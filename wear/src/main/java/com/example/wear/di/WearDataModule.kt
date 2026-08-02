@file:Suppress("DEPRECATION")

package com.example.wear.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.local.BankAccountDao
import com.example.data.local.TransactionDao
import com.example.data.repository.BankRepository
import com.example.data.repository.KeyValueSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

class WearKeyValueSettings(
    private val dataStore: DataStore<Preferences>,
    private val encryptedPrefs: SharedPreferences
) : KeyValueSettings {

    private val USE_SANDBOX = booleanPreferencesKey("use_sandbox")

    override fun getString(key: String, defaultValue: String): String {
        if (key == "default_sandbox_client_id") {
            return BankRepository.DEFAULT_SANDBOX_CLIENT_ID
        }
        if (key == "default_sandbox_client_secret") {
            return BankRepository.DEFAULT_SANDBOX_CLIENT_SECRET
        }
        if (key == "default_sandbox_api_key") {
            return BankRepository.DEFAULT_SANDBOX_API_KEY
        }
        return encryptedPrefs.getString(key, defaultValue) ?: defaultValue
    }

    override fun setString(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).commit()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        if (!encryptedPrefs.contains(key)) return defaultValue
        return encryptedPrefs.getBoolean(key, defaultValue)
    }

    override fun setBoolean(key: String, value: Boolean) {
        encryptedPrefs.edit().putBoolean(key, value).commit()
        if (key == "use_sandbox") {
            runBlocking {
                dataStore.edit { it[USE_SANDBOX] = value }
            }
        }
    }

    override fun getBooleanFlow(key: String, defaultValue: Boolean): Flow<Boolean> {
        if (key == "use_sandbox") {
            return dataStore.data.map { preferences ->
                preferences[USE_SANDBOX] ?: getBoolean("use_sandbox", defaultValue)
            }
        }
        return getStringFlow(key, "").map { getBoolean(key, defaultValue) }
    }

    override fun getStringFlow(key: String, defaultValue: String): Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, changedKey ->
            if (changedKey == key) {
                trySend(prefs.getString(key, defaultValue) ?: defaultValue)
            }
        }
        encryptedPrefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getString(key, defaultValue))
        awaitClose { encryptedPrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object WearDataModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("wear_investec_settings") }
        )
    }

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return try {
            EncryptedSharedPreferences.create(
                context,
                "wear_secure_investec_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.deleteSharedPreferences("wear_secure_investec_prefs")
            EncryptedSharedPreferences.create(
                context,
                "wear_secure_investec_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    @Provides
    @Singleton
    fun provideKeyValueSettings(
        dataStore: DataStore<Preferences>,
        encryptedPrefs: SharedPreferences
    ): KeyValueSettings {
        return WearKeyValueSettings(dataStore, encryptedPrefs)
    }

    @Provides
    @Singleton
    fun provideBankRepository(
        @ApplicationContext context: Context,
        accountDao: BankAccountDao,
        transactionDao: TransactionDao,
        settings: KeyValueSettings
    ): BankRepository {
        return BankRepository(accountDao, transactionDao, settings).apply {
            onSyncCompleted = {
                try {
                    com.example.tile.BankTileService.requestTileUpdate(context)
                } catch (e: Exception) {
                    android.util.Log.e("WearDataModule", "Error updating Wear Tile: ${e.message}")
                }
            }
        }
    }
}
