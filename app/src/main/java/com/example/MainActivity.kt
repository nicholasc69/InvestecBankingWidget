package com.example

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.util.Log
import android.app.AlarmManager
import android.app.PendingIntent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.ui.chat.ChatScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.data.ai.LiteRtEngineManager
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var engineManager: LiteRtEngineManager

    private val isAppAuthenticated = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val authenticated by isAppAuthenticated

                LaunchedEffect(authenticated) {
                    if (!authenticated) {
                        val prefs = getSharedPreferences("widget_security_prefs", Context.MODE_PRIVATE)
                        val widgetUnlocked = prefs.getBoolean("widget_unlocked", false)
                        val lastAuthTime = prefs.getLong("last_authenticated_time", 0)
                        val isRecent = (System.currentTimeMillis() - lastAuthTime) < 5_000

                        if (widgetUnlocked && isRecent) {
                            isAppAuthenticated.value = true
                        } else {
                            showBiometricPrompt {
                                isAppAuthenticated.value = true
                                unlockApplicationAndWidget()
                                if (intent.getBooleanExtra("JUST_AUTHENTICATE", false)) {
                                    finish()
                                }
                            }
                        }
                    }
                }

                if (authenticated) {
                    val viewModel: DashboardViewModel = hiltViewModel()
                    var currentDestination by rememberSaveable() { mutableStateOf(ChatNavDestination.HOME) }

                    val backgroundLight = Color(0xFFF3F4F9)
                    val textPrimary = Color(0xFF1A1C1E)
                    val textSecondary = Color(0xFF44474E)
                    val accentContainer = Color(0xFFD6E3FF)
                    val accentOnContainer = Color(0xFF001B3E)

                    val customItemColors = androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults.itemColors(
                        navigationBarItemColors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = accentOnContainer,
                            selectedTextColor = accentOnContainer,
                            indicatorColor = accentContainer,
                            unselectedIconColor = textSecondary,
                            unselectedTextColor = textSecondary
                        ),
                        navigationRailItemColors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                            selectedIconColor = accentOnContainer,
                            selectedTextColor = accentOnContainer,
                            indicatorColor = accentContainer,
                            unselectedIconColor = textSecondary,
                            unselectedTextColor = textSecondary
                        )
                    )

                    NavigationSuiteScaffold(
                        navigationSuiteColors = androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults.colors(
                            navigationBarContainerColor = backgroundLight,
                            navigationBarContentColor = textPrimary,
                            navigationRailContainerColor = backgroundLight,
                            navigationRailContentColor = textPrimary
                        ),
                        containerColor = backgroundLight,
                        contentColor = textPrimary,
                        navigationSuiteItems = {
                            ChatNavDestination.entries.forEach { destination ->
                                item(
                                    icon = {
                                        val painter = if (destination.iconRes != null) {
                                            androidx.compose.ui.res.painterResource(destination.iconRes)
                                        } else {
                                            androidx.compose.ui.graphics.vector.rememberVectorPainter(destination.iconVector!!)
                                        }
                                        Icon(
                                            painter = painter,
                                            contentDescription = destination.label
                                        )
                                    },
                                    label = { Text(destination.label) },
                                    selected = destination == currentDestination,
                                    onClick = { currentDestination = destination },
                                    colors = customItemColors
                                )
                            }
                        }
                    ) {
                        when (currentDestination) {
                            ChatNavDestination.HOME -> DashboardScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )

                            ChatNavDestination.CHAT -> ChatScreen(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    LockedSplashScreen(onUnlockClick = {
                        showBiometricPrompt {
                            isAppAuthenticated.value = true
                            unlockApplicationAndWidget()
                        }
                    })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        cancelWidgetLockAlarm()
    }

    override fun onStop() {
        super.onStop()
        isAppAuthenticated.value = false
        
        // Refresh the authentication timestamp upon leaving the app to give a full 5 seconds of visibility
        val prefs = getSharedPreferences("widget_security_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("widget_unlocked", false)) {
            prefs.edit().putLong("last_authenticated_time", System.currentTimeMillis()).apply()
            scheduleWidgetLockAlarm()
        }
    }



    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e("MainActivity", "Authentication error: $errString ($errorCode)")
                    lockApplicationAndWidget()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.d("MainActivity", "Authentication failed")
                    lockApplicationAndWidget()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Investec Security Verification")
            .setSubtitle("Authenticate using your biometric credential to unlock Private Banking")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun unlockApplicationAndWidget() {
        val prefs = getSharedPreferences("widget_security_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("widget_unlocked", true)
            .putLong("last_authenticated_time", System.currentTimeMillis())
            .apply()
        triggerWidgetUpdate()
    }

    private fun lockApplicationAndWidget() {
        val prefs = getSharedPreferences("widget_security_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("widget_unlocked", false)
            .putLong("last_authenticated_time", 0L)
            .apply()
        cancelWidgetLockAlarm()
        triggerWidgetUpdate()
    }

    private fun scheduleWidgetLockAlarm() {
        val prefs = getSharedPreferences("widget_security_prefs", Context.MODE_PRIVATE)
        val isUnlocked = prefs.getBoolean("widget_unlocked", false)
        if (isUnlocked) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, com.example.receiver.BankWidgetProvider::class.java).apply {
                action = "com.example.receiver.ACTION_LOCK_WIDGET"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerTime = System.currentTimeMillis() + 5_000 // 5 seconds
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            Log.d("MainActivity", "Scheduled widget lock alarm for 5 seconds from now")
        }
    }

    private fun cancelWidgetLockAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, com.example.receiver.BankWidgetProvider::class.java).apply {
            action = "com.example.receiver.ACTION_LOCK_WIDGET"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("MainActivity", "Cancelled widget lock alarm")
    }

    private fun triggerWidgetUpdate() {
        val intent = Intent(this, com.example.receiver.BankWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = android.appwidget.AppWidgetManager.getInstance(application)
                .getAppWidgetIds(android.content.ComponentName(application, com.example.receiver.BankWidgetProvider::class.java))
            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(intent)
    }
}

@Composable
fun LockedSplashScreen(onUnlockClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F9)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(48.dp))
                    .background(Color(0xFFD6E3FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_zebra_head),
                    contentDescription = null,
                    tint = Color(0xFF001B3E),
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Investec Private Banking",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C1E),
                fontSize = 22.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Security Verification Required",
                color = Color(0xFF74777F),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onUnlockClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF001B3E),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock with Biometrics")
            }
        }
    }
}

enum class ChatNavDestination(
    val label: String,
    val iconRes: Int? = null,
    val iconVector: ImageVector? = null,
) {
    HOME("Home", iconVector = Icons.Filled.Home),
    CHAT("Chat", iconVector = Icons.AutoMirrored.Filled.Chat)
}