package com.example

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import com.example.receiver.BankWidgetProvider
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.lifecycle.lifecycleScope

class WidgetUnlockActivity : FragmentActivity() {

    private companion object {
        const val TAG = "WidgetUnlockActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        showBiometricPrompt()
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        var authFinished = false
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "Authentication error: $errString ($errorCode)")
                    if (!authFinished) {
                        authFinished = true
                        lockWidget()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "Authentication succeeded")
                    if (!authFinished) {
                        authFinished = true
                        unlockWidget()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.d(TAG, "Authentication failed (soft failure)")
                    // Allow the user to retry scanning; do not call finish() yet.
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Investec Security Verification")
            .setSubtitle("Authenticate using your biometric credential to unlock Private Banking Widget")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun unlockWidget() {
        val prefs = getSharedPreferences("widget_security_prefs", MODE_PRIVATE)
        prefs.edit(commit = true) {
            putBoolean("widget_unlocked", true)
            putLong("last_authenticated_time", System.currentTimeMillis())
        }
        scheduleWidgetLockAlarm()

        // Background Coroutine timer as a robust fallback for lock enforcement after 5 seconds
        CoroutineScope(Dispatchers.Default).launch {
            delay(5_000)
            if (!MainActivity.isAppInForeground) {
                val currentPrefs = getSharedPreferences("widget_security_prefs", MODE_PRIVATE)
                currentPrefs.edit(commit = true) {
                    putBoolean("widget_unlocked", false)
                    putLong("last_authenticated_time", 0L)
                }
                try {
                    com.example.receiver.BankGlanceWidget().updateAll(applicationContext)
                    Log.d(TAG, "Coroutine locked widget successfully after 5 seconds widget-unlock delay")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating Glance widget in widget-unlock coroutine: ${e.message}", e)
                }
            }
        }
        
        lifecycleScope.launch {
            try {
                com.example.receiver.BankGlanceWidget().updateAll(this@WidgetUnlockActivity)
                Log.d(TAG, "Glance widget updated on unlock successfully")
                delay(1500)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating Glance widget: ${e.message}", e)
            } finally {
                finish()
            }
        }
    }

    private fun lockWidget() {
        val prefs = getSharedPreferences("widget_security_prefs", MODE_PRIVATE)
        prefs.edit(commit = true) {
            putBoolean("widget_unlocked", false)
            putLong("last_authenticated_time", 0L)
        }
        cancelWidgetLockAlarm()
        
        lifecycleScope.launch {
            try {
                com.example.receiver.BankGlanceWidget().updateAll(this@WidgetUnlockActivity)
                Log.d(TAG, "Glance widget updated on lock successfully")
                delay(1500)
            } catch (e: Exception) {
                Log.e(TAG, "Error locking Glance widget: ${e.message}", e)
            } finally {
                finish()
            }
        }
    }

    private fun scheduleWidgetLockAlarm() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, BankWidgetProvider::class.java).apply {
            action = BankWidgetProvider.ACTION_LOCK_WIDGET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerTime = android.os.SystemClock.elapsedRealtime() + 5_000 // 5 seconds

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent)
                Log.d(TAG, "Scheduled exact widget lock alarm for 5 seconds from now")
            } else {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent)
                Log.d(TAG, "Scheduled exact widget lock alarm for 5 seconds from now")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm, falling back", e)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }

    private fun cancelWidgetLockAlarm() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, BankWidgetProvider::class.java).apply {
            action = BankWidgetProvider.ACTION_LOCK_WIDGET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled widget lock alarm")
    }
}
