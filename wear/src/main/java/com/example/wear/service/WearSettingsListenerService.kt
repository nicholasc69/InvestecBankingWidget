package com.example.wear.service

import android.util.Log
import com.example.data.repository.BankRepository
import com.example.data.repository.KeyValueSettings
import com.example.data.sync.WearSettingsSyncHelper
import com.example.tile.BankTileService
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WearSettingsListenerService : WearableListenerService() {

    @Inject
    lateinit var settings: KeyValueSettings

    @Inject
    lateinit var repository: BankRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "WearSettingsListener"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(TAG, "onDataChanged triggered on Wear OS")
        val updated = WearSettingsSyncHelper.applySettingsFromDataEvents(dataEvents, settings)
        if (updated) {
            Log.d(TAG, "Wear OS settings updated from phone app. Triggering Wear bank repository sync...")
            serviceScope.launch {
                repository.syncData()
                try {
                    BankTileService.requestTileUpdate(applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating Wear tile: ${e.message}")
                }
            }
        }
    }
}
