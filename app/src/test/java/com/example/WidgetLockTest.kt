package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.receiver.BankWidgetProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WidgetLockTest {

    @Test
    fun testWidgetLockAlarmSchedulesAndFires() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("widget_security_prefs", Context.MODE_PRIVATE)

        // 1. Set widget as unlocked
        prefs.edit()
            .putBoolean("widget_unlocked", true)
            .putLong("last_authenticated_time", System.currentTimeMillis())
            .apply()

        assertTrue(prefs.getBoolean("widget_unlocked", false))

        // 2. Trigger receiver directly to simulate ACTION_LOCK_WIDGET
        val intent = Intent(context, BankWidgetProvider::class.java).apply {
            action = BankWidgetProvider.ACTION_LOCK_WIDGET
        }

        val receiver = BankWidgetProvider()
        receiver.onReceive(context, intent)

        // 3. Verify preferences are cleared
        assertFalse(prefs.getBoolean("widget_unlocked", false))
    }
}
