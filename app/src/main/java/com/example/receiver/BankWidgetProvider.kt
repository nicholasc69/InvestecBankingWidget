package com.example.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.BankDatabase
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class BankWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val TAG = "BankWidgetProvider"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for AppWidgets")
        
        // Retrieve database data on an IO Coroutine thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = BankDatabase.getDatabase(context)
                val accountsFlow = database.bankAccountDao().getAccountsFlow()
                val accounts = accountsFlow.firstOrNull() ?: emptyList()
                
                val activeAccount = accounts.firstOrNull()
                val transactions = activeAccount?.let { 
                    database.transactionDao().getLastFiveTransactions(it.accountId)
                } ?: emptyList()

                for (appWidgetId in appWidgetIds) {
                    updateWidgetState(context, appWidgetManager, appWidgetId, activeAccount, transactions)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating app widget state: ${e.message}", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "onReceive triggered action: ${intent.action}")
        
        // Handle trigger update broadcast safely
        if (intent.action == "android.appwidget.action.APPWIDGET_UPDATE" || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BankWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private fun updateWidgetState(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        account: BankAccountEntity?,
        transactions: List<TransactionEntity>
    ) {
        val views = RemoteViews(context.packageName, R.layout.bank_widget_layout)

        if (account != null) {
            val df = DecimalFormat("#,##0.00")
            val symbol = getCurrencySymbol(account.currency)
            val displayBalance = df.format(account.availableBalance)
            val syncTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(account.lastUpdated))

            // Update Account Details
            views.setTextViewText(R.id.widget_account_name, account.accountName)
            views.setTextViewText(R.id.widget_account_number, "${account.productName} • ${maskAccountNumber(account.accountNumber)}")
            views.setTextViewText(R.id.widget_balance, "$symbol $displayBalance")
            views.setTextViewText(R.id.widget_last_synced, "Synced: $syncTime")

            // Bind Transactions (Row 1 to 5)
            views.setViewVisibility(R.id.widget_empty_txs, if (transactions.isEmpty()) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_tx_container, if (transactions.isEmpty()) View.GONE else View.VISIBLE)

            val rowIds = listOf(
                R.id.widget_tx_row_1 to (R.id.widget_tx_desc_1 to R.id.widget_tx_amount_1),
                R.id.widget_tx_row_2 to (R.id.widget_tx_desc_2 to R.id.widget_tx_amount_2),
                R.id.widget_tx_row_3 to (R.id.widget_tx_desc_3 to R.id.widget_tx_amount_3),
                R.id.widget_tx_row_4 to (R.id.widget_tx_desc_4 to R.id.widget_tx_amount_4),
                R.id.widget_tx_row_5 to (R.id.widget_tx_desc_5 to R.id.widget_tx_amount_5)
            )

            for (index in 0 until 5) {
                val (rowId, textIds) = rowIds[index]
                val (descId, amountId) = textIds
                val tx = transactions.getOrNull(index)

                if (tx != null) {
                    views.setViewVisibility(rowId, View.VISIBLE)
                    views.setTextViewText(descId, tx.description.trim().uppercase())
                    
                    val isCredit = tx.type.equals("CREDIT", ignoreCase = true)
                    val prefix = if (isCredit) "+" else "-"
                    val amountStr = "$prefix$symbol${df.format(tx.amount)}"
                    views.setTextViewText(amountId, amountStr)
                    
                    // Set color (Green #00E676 for credits, White #FFFFFF for debits)
                    val amountColor = if (isCredit) 0xFF00E676.toInt() else 0xFFFFFFFF.toInt()
                    views.setTextColor(amountId, amountColor)
                } else {
                    views.setViewVisibility(rowId, View.GONE)
                }
            }
        } else {
            // Un-cached Empty State Layout bindings
            views.setTextViewText(R.id.widget_account_name, "No Account Details Cached")
            views.setTextViewText(R.id.widget_account_number, "Open app and authenticate settings to sync")
            views.setTextViewText(R.id.widget_balance, "R --.--")
            views.setTextViewText(R.id.widget_last_synced, "Sync: Offline")
            views.setViewVisibility(R.id.widget_empty_txs, View.VISIBLE)
            views.setViewVisibility(R.id.widget_tx_container, View.GONE)
        }

        // Tap to open MainWindow MainActivity PendingIntent Configuration
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // Commit update parameters to system manager
        appWidgetManager.updateAppWidget(appWidgetId, views)
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
}
