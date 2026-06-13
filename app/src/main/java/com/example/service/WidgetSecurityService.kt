package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log

class WidgetSecurityService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("WidgetSecurityService", "onTaskRemoved called - App swiped away from recents")
        
        // Lock the widget immediately
        val prefs = getSharedPreferences("widget_security_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("widget_unlocked", false)
            .putLong("last_authenticated_time", 0L)
            .apply()
            
        // Trigger widget update to show the locked screen immediately
        val updateIntent = Intent(this, com.example.receiver.BankWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = android.appwidget.AppWidgetManager.getInstance(application)
                .getAppWidgetIds(android.content.ComponentName(application, com.example.receiver.BankWidgetProvider::class.java))
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(updateIntent)
        
        stopSelf()
    }
}
