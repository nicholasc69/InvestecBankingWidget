package com.example.service

import android.util.Log
import com.example.data.repository.KeyValueSettings
import com.example.data.sync.WearSettingsSyncHelper
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PhoneSettingsListenerService : WearableListenerService() {

    @Inject
    lateinit var settings: KeyValueSettings

    companion object {
        private const val TAG = "PhoneSettingsListener"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        if (messageEvent.path == WearSettingsSyncHelper.REQUEST_SETTINGS_PATH) {
            Log.d(TAG, "Received request for settings from Wear device. Pushing settings to Wear...")
            WearSettingsSyncHelper.pushSettingsToWear(applicationContext, settings)
        }
    }
}
