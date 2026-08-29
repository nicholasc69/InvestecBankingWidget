package com.example.data.sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.repository.KeyValueSettings
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

object WearSettingsSyncHelper {
    private const val TAG = "WearSettingsSyncHelper"
    const val SETTINGS_PATH = "/investec_api_settings"
    const val REQUEST_SETTINGS_PATH = "/request_investec_settings"

    const val KEY_USE_SANDBOX = "use_sandbox"
    const val KEY_CLIENT_ID = "client_id"
    const val KEY_CLIENT_SECRET = "client_secret"
    const val KEY_API_KEY = "api_key"
    const val KEY_SELECTED_PROFILE_ID = "selected_profile_id"
    const val KEY_TIMESTAMP = "timestamp"

    /**
     * Pushes current phone app settings to Google Play Services Wearable DataLayer
     */
    fun pushSettingsToWear(context: Context, settings: KeyValueSettings) {
        try {
            val useSandbox = settings.getBoolean(KEY_USE_SANDBOX, true)
            var clientId = settings.getString(KEY_CLIENT_ID, "")
            var clientSecret = settings.getString(KEY_CLIENT_SECRET, "")
            var apiKey = settings.getString(KEY_API_KEY, "")

            if (useSandbox && (clientId.isBlank() || clientSecret.isBlank() || apiKey.isBlank())) {
                val defaultCid = settings.getString("default_sandbox_client_id", "")
                val defaultSec = settings.getString("default_sandbox_client_secret", "")
                val defaultKey = settings.getString("default_sandbox_api_key", "")
                if (clientId.isBlank()) clientId = defaultCid
                if (clientSecret.isBlank()) clientSecret = defaultSec
                if (apiKey.isBlank()) apiKey = defaultKey
            }

            val request = PutDataMapRequest.create(SETTINGS_PATH).apply {
                dataMap.putBoolean(KEY_USE_SANDBOX, useSandbox)
                dataMap.putString(KEY_CLIENT_ID, clientId)
                dataMap.putString(KEY_CLIENT_SECRET, clientSecret)
                dataMap.putString(KEY_API_KEY, apiKey)
                dataMap.putString(KEY_SELECTED_PROFILE_ID, settings.getString(KEY_SELECTED_PROFILE_ID, ""))
                dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            Wearable.getDataClient(context).putDataItem(request)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully synced settings DataItem to Wear OS DataLayer")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to sync settings to Wear OS DataLayer: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing settings to Wear OS: ${e.message}", e)
        }
    }

    /**
     * Extracts settings from DataEventBuffer and saves them to local settings
     */
    fun applySettingsFromDataEvents(events: DataEventBuffer, settings: KeyValueSettings): Boolean {
        var updated = false
        for (event in events) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == SETTINGS_PATH) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val useSandbox = dataMap.getBoolean(KEY_USE_SANDBOX, true)
                val clientId = dataMap.getString(KEY_CLIENT_ID, "")
                val clientSecret = dataMap.getString(KEY_CLIENT_SECRET, "")
                val apiKey = dataMap.getString(KEY_API_KEY, "")
                val selectedProfileId = dataMap.getString(KEY_SELECTED_PROFILE_ID, "")

                settings.setBoolean(KEY_USE_SANDBOX, useSandbox)
                settings.setString(KEY_CLIENT_ID, clientId)
                settings.setString(KEY_CLIENT_SECRET, clientSecret)
                settings.setString(KEY_API_KEY, apiKey)
                settings.setString(KEY_SELECTED_PROFILE_ID, selectedProfileId)
                updated = true
                Log.d(TAG, "Updated local Wear settings from Phone DataLayer event")
            }
        }
        return updated
    }

    /**
     * Pulls cached settings from Wearable DataClient on demand
     */
    fun fetchLatestSettingsFromDataLayer(context: Context, settings: KeyValueSettings, onSettingsFetched: (() -> Unit)? = null) {
        try {
            Log.d(TAG, "Fetching latest settings from Wear DataLayer...")
            val uri = Uri.parse("wear://$SETTINGS_PATH")
            Wearable.getDataClient(context).getDataItems(uri)
                .addOnSuccessListener { dataItems ->
                    Log.d(TAG, "Found ${dataItems.count} items in DataLayer for $SETTINGS_PATH")
                    var updated = false
                    for (item in dataItems) {
                        if (item.uri.path == SETTINGS_PATH) {
                            val dataMap = DataMapItem.fromDataItem(item).dataMap
                            val useSandbox = dataMap.getBoolean(KEY_USE_SANDBOX, true)
                            val clientId = dataMap.getString(KEY_CLIENT_ID, "")
                            val clientSecret = dataMap.getString(KEY_CLIENT_SECRET, "")
                            val apiKey = dataMap.getString(KEY_API_KEY, "")
                            val selectedProfileId = dataMap.getString(KEY_SELECTED_PROFILE_ID, "")

                            Log.d(TAG, "Extracted from DataLayer: useSandbox=$useSandbox, clientId=${clientId.take(4)}...")

                            settings.setBoolean(KEY_USE_SANDBOX, useSandbox)
                            settings.setString(KEY_CLIENT_ID, clientId)
                            settings.setString(KEY_CLIENT_SECRET, clientSecret)
                            settings.setString(KEY_API_KEY, apiKey)
                            settings.setString(KEY_SELECTED_PROFILE_ID, selectedProfileId)
                            updated = true
                        }
                    }
                    dataItems.release()
                    if (updated) {
                        Log.d(TAG, "Fetched and applied Wear OS settings from DataLayer")
                    } else {
                        Log.w(TAG, "No matching settings found in DataLayer")
                    }
                    // Always invoke to unblock UI if waiting
                    onSettingsFetched?.invoke()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to fetch DataItems from DataLayer: ${e.message}")
                    onSettingsFetched?.invoke()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching settings from DataLayer: ${e.message}", e)
            onSettingsFetched?.invoke()
        }
    }

    /**
     * Sends a message from Wear OS to Phone asking the phone app to push latest settings to DataLayer
     */
    fun requestSettingsFromPhone(context: Context) {
        try {
            Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
                for (node in nodes) {
                    Wearable.getMessageClient(context).sendMessage(
                        node.id,
                        REQUEST_SETTINGS_PATH,
                        byteArrayOf()
                    ).addOnSuccessListener {
                        Log.d(TAG, "Sent request settings message to node: ${node.id}")
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "Failed to send request settings message to node ${node.id}: ${e.message}")
                    }
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to get connected nodes: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting settings from phone: ${e.message}", e)
        }
    }
}
