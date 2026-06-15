package com.example.receiver

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import com.example.data.local.BankDatabase
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import androidx.core.content.edit

class BankWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BankGlanceWidget()

    companion object {
        private const val TAG = "BankWidgetProvider"
        const val ACTION_LOCK_WIDGET = "com.example.receiver.ACTION_LOCK_WIDGET"
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive triggered action: ${intent.action}")
        if (intent.action == ACTION_LOCK_WIDGET || intent.action == Intent.ACTION_USER_PRESENT) {
            val prefs = context.getSharedPreferences("widget_security_prefs", Context.MODE_PRIVATE)
            val lastAuthTime = prefs.getLong("last_authenticated_time", 0)
            val isRecent = (System.currentTimeMillis() - lastAuthTime) < 5_000

            if (intent.action == ACTION_LOCK_WIDGET || !isRecent) {
                prefs.edit(commit = true) {
                    putBoolean("widget_unlocked", false)
                        .putLong("last_authenticated_time", 0L)
                }

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        BankGlanceWidget().updateAll(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating glance widget: ${e.message}", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}

class BankGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d("BankGlanceWidget", "provideGlance called")
        val database = BankDatabase.getDatabase(context)
        val accounts = database.bankAccountDao().getAccounts()
        val activeAccount = accounts.firstOrNull()
        val transactions = activeAccount?.let {
            database.transactionDao().getAllTransactions(it.accountId)
        } ?: emptyList()

        provideContent {
            val prefs = context.getSharedPreferences("widget_security_prefs", Context.MODE_PRIVATE)
            val isUnlocked = prefs.getBoolean("widget_unlocked", false)
            val lastAuthTime = prefs.getLong("last_authenticated_time", 0)
            val currentTime = System.currentTimeMillis()
            val isExpired = (currentTime - lastAuthTime) > 5_000

            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            val isKeyguardLocked = keyguardManager.isKeyguardLocked

            val showUnlocked = isUnlocked && !isExpired && !isKeyguardLocked

            BankWidgetContent(
                context = context,
                showUnlocked = showUnlocked,
                account = activeAccount,
                transactions = transactions
            )
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun BankWidgetContent(
    context: Context,
    showUnlocked: Boolean,
    account: BankAccountEntity?,
    transactions: List<TransactionEntity>
) {
    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (!showUnlocked) {
            action = "com.example.action.AUTHENTICATE_WIDGET"
            putExtra("JUST_AUTHENTICATE", true)
        } else {
            action = "com.example.action.OPEN_APP"
        }
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_background))
            .padding(12.dp)
            .clickable(actionStartActivity(openAppIntent))
    ) {
        if (!showUnlocked) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_lock),
                    contentDescription = "Locked",
                    modifier = GlanceModifier.size(40.dp)
                )
                Spacer(modifier = GlanceModifier.size(12.dp))
                Text(
                    text = "Tap to Unlock",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF001B3E)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.size(4.dp))
                Text(
                    text = "Biometric Authentication Required",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF74777F)),
                        fontSize = 10.sp
                    )
                )
            }
        } else {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "Investec Private Banking",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF001B3E)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    val syncTime = if (account != null) {
                        SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(Date(account.lastUpdated))
                    } else {
                        "--:--"
                    }
                    Text(
                        text = "Sync: $syncTime",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF74777F)),
                            fontSize = 9.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.size(8.dp))

                // Account & Balance Card
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(ImageProvider(R.drawable.widget_account_background))
                        .padding(10.dp)
                ) {
                    if (account != null) {
                        val df = DecimalFormat("#,##0.00")
                        val symbol = getCurrencySymbol(account.currency)
                        val displayBalance = df.format(account.availableBalance)
                        val maskedAccNum = maskAccountNumber(account.accountNumber)

                        Text(
                            text = account.accountName,
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF001B3E)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = "${account.productName} • $maskedAccNum",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF44474E)),
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.size(4.dp))
                        Text(
                            text = "$symbol $displayBalance",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF001B3E)),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else {
                        Text(
                            text = "No Account Details Cached",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF001B3E)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Open app and authenticate settings to sync",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF44474E)),
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.size(4.dp))
                        Text(
                            text = "R --.--",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF001B3E)),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = GlanceModifier.size(8.dp))

                Text(
                    text = "RECENT POSTINGS:",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF74777F)),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.size(4.dp))

                if (transactions.isEmpty()) {
                    Box(
                        modifier = GlanceModifier.fillMaxWidth().padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (account != null) "No transaction details cached yet." else "Sync: Offline",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF74777F)),
                                fontSize = 10.sp
                            )
                        )
                    }
                } else {
                    Column(
                        modifier = GlanceModifier.fillMaxWidth()
                    ) {
                        transactions.take(10).forEach { tx ->
                            val df = DecimalFormat("#,##0.00")
                            val symbol = getCurrencySymbol(account?.currency ?: "ZAR")
                            val isCredit = tx.type.equals("CREDIT", ignoreCase = true)
                            val prefix = if (isCredit) "+" else "-"
                            val amountStr = "$prefix$symbol${df.format(tx.amount)}"
                            val amountColor = if (isCredit) Color(0xFF116D34) else Color(0xFFBA1A1A)

                            Row(
                                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Vertical.CenterVertically
                            ) {
                                Text(
                                    text = tx.description.trim().uppercase(),
                                    style = TextStyle(
                                        color = ColorProvider(Color(0xFF1A1C1E)),
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                                Spacer(modifier = GlanceModifier.size(2.dp))
                                Text(
                                    text = amountStr,
                                    style = TextStyle(
                                        color = ColorProvider(amountColor),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getCurrencySymbol(isoCode: String): String {
    return when (isoCode.uppercase()) {
        "ZAR" -> "R"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> isoCode
    }
}

private fun maskAccountNumber(accNum: String): String {
    if (accNum.length < 5) return accNum
    return "****" + accNum.substring(accNum.length - 4)
}
