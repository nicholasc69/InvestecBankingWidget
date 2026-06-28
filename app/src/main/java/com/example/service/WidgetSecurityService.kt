package com.example.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.content.edit
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetSecurityService : Service() {

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                Log.d("WidgetSecurityService", "ACTION_SCREEN_OFF received - Locking widget immediately")
                lockWidgetImmediately()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("WidgetSecurityService", "Service created - registering SCREEN_OFF receiver")
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("WidgetSecurityService", "onStartCommand called")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("WidgetSecurityService", "Service destroyed - unregistering SCREEN_OFF receiver")
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            Log.e("WidgetSecurityService", "Error unregistering screenOffReceiver", e)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("WidgetSecurityService", "onTaskRemoved called - App swiped away from recents")
        lockWidgetImmediately()
        stopSelf()
    }

    private fun lockWidgetImmediately() {
        val prefs = getSharedPreferences("widget_security_prefs", MODE_PRIVATE)
        prefs.edit(commit = true) {
            putBoolean("widget_unlocked", false)
            putLong("last_authenticated_time", 0L)
        }

        // Trigger widget update to show the locked screen immediately
        val updateIntent = Intent(this, com.example.receiver.BankWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = android.appwidget.AppWidgetManager.getInstance(application)
                .getAppWidgetIds(android.content.ComponentName(application, com.example.receiver.BankWidgetProvider::class.java))
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(updateIntent)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.example.receiver.BankGlanceWidget().updateAll(applicationContext)
            } catch (e: Exception) {
                Log.e("WidgetSecurityService", "Error updating Glance widget: ${e.message}", e)
            }
        }
    }
}
