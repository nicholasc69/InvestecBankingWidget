package com.example.tile

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import com.example.data.repository.BankRepository
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import javax.inject.Inject

@AndroidEntryPoint
class BankTileService : TileService() {

    @Inject
    lateinit var repository: BankRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val RESOURCES_VERSION = "1"

        /**
         * Requests an immediate update of the Wear OS Tile.
         */
        fun requestTileUpdate(context: Context) {
            getUpdater(context).requestUpdate(BankTileService::class.java)
        }
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return CallbackToFutureAdapter.getFuture { completer ->
            serviceScope.launch {
                try {
                    val accounts = repository.getAccounts()
                    val primaryAccount = accounts.firstOrNull()
                    val lastTx = primaryAccount?.let {
                        repository.getLastFiveTransactions(it.accountId).firstOrNull()
                    }

                    val tile = buildTile(primaryAccount, accounts.size, lastTx)
                    completer.set(tile)
                } catch (e: Exception) {
                    val fallbackTile = buildTile(null, 0, null)
                    completer.set(fallbackTile)
                }
            }
            "BankTileService.onTileRequest"
        }
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return CallbackToFutureAdapter.getFuture { completer ->
            val resources = ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
            completer.set(resources)
            "BankTileService.onResourcesRequest"
        }
    }

    internal fun buildTile(
        primaryAccount: BankAccountEntity?,
        totalAccounts: Int,
        lastTx: TransactionEntity?
    ): TileBuilders.Tile {
        val pkg = try { packageName } catch (e: Exception) { "com.example.wear" }
        val clickAction = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(pkg)
                    .setClassName("com.example.wear.presentation.MainActivity")
                    .build()
            )
            .build()

        val clickable = ModifiersBuilders.Clickable.Builder()
            .setOnClick(clickAction)
            .setId("open_app_click")
            .build()

        val rootElement: LayoutElementBuilders.LayoutElement = if (primaryAccount != null) {
            buildAccountTileContent(primaryAccount, totalAccounts, lastTx, clickable)
        } else {
            buildEmptyTileContent(clickable)
        }

        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(rootElement)
                            .build()
                    )
                    .build()
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(timeline)
            .setFreshnessIntervalMillis(60 * 1000L)
            .build()
    }

    private fun buildAccountTileContent(
        account: BankAccountEntity,
        totalAccounts: Int,
        lastTx: TransactionEntity?,
        clickable: ModifiersBuilders.Clickable
    ): LayoutElementBuilders.LayoutElement {
        val df = DecimalFormat("#,##0.00")
        val formattedBalance = "${getCurrencySymbol(account.currency)}${df.format(account.availableBalance)}"
        val maskedAccNum = maskAccountNumber(account.accountNumber)

        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

        // Brand Label
        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText("INVESTEC BANK")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(10f))
                        .setColor(ColorBuilders.argb(0xFFD6E3FF.toInt()))
                        .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                        .build()
                )
                .build()
        )

        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())

        // Account Name
        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(account.accountName)
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(12f))
                        .setColor(ColorBuilders.argb(0xFF9EA3B0.toInt()))
                        .build()
                )
                .build()
        )

        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(2f)).build())

        // Available Balance Big Display
        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText(formattedBalance)
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(18f))
                        .setColor(ColorBuilders.argb(0xFFFFFFFF.toInt()))
                        .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                        .build()
                )
                .build()
        )

        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(2f)).build())

        // Account Details Subtitle
        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText("Available • $maskedAccNum")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(11f))
                        .setColor(ColorBuilders.argb(0xFF80D4FF.toInt()))
                        .build()
                )
                .build()
        )

        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(6f)).build())

        // Bottom Activity Pill / Badge
        val chipText = if (lastTx != null) {
            val txSymbol = if (lastTx.type.equals("DEBIT", ignoreCase = true)) "-" else "+"
            val txAmount = df.format(lastTx.amount)
            "Last: $txSymbol${getCurrencySymbol(account.currency)}$txAmount"
        } else if (totalAccounts > 1) {
            "$totalAccounts Accounts"
        } else {
            "Open Banking App"
        }

        val pillText = LayoutElementBuilders.Text.Builder()
            .setText(chipText)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(sp(10f))
                    .setColor(ColorBuilders.argb(0xFFD6E3FF.toInt()))
                    .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                    .build()
            )
            .build()

        val pillBox = LayoutElementBuilders.Box.Builder()
            .addContent(pillText)
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(ColorBuilders.argb(0xFF1A2638.toInt()))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(12f)).build())
                            .build()
                    )
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setStart(dp(10f))
                            .setEnd(dp(10f))
                            .setTop(dp(4f))
                            .setBottom(dp(4f))
                            .build()
                    )
                    .build()
            )
            .build()

        column.addContent(pillBox)

        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(column.build())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(clickable)
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(ColorBuilders.argb(0xFF001B3E.toInt()))
                            .build()
                    )
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setAll(dp(12f))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun buildEmptyTileContent(
        clickable: ModifiersBuilders.Clickable
    ): LayoutElementBuilders.LayoutElement {
        val column = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText("INVESTEC BANK")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(10f))
                        .setColor(ColorBuilders.argb(0xFFD6E3FF.toInt()))
                        .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                        .build()
                )
                .build()
        )

        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(8f)).build())

        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText("No Accounts")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(16f))
                        .setColor(ColorBuilders.argb(0xFFFFFFFF.toInt()))
                        .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                        .build()
                )
                .build()
        )

        column.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())

        column.addContent(
            LayoutElementBuilders.Text.Builder()
                .setText("Tap to setup connection")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(11f))
                        .setColor(ColorBuilders.argb(0xFF9EA3B0.toInt()))
                        .build()
                )
                .build()
        )

        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(column.build())
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(clickable)
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(ColorBuilders.argb(0xFF001B3E.toInt()))
                            .build()
                    )
                    .setPadding(
                        ModifiersBuilders.Padding.Builder()
                            .setAll(dp(12f))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun getCurrencySymbol(currency: String): String {
        return when (currency.uppercase()) {
            "ZAR" -> "R "
            "USD" -> "$ "
            "EUR" -> "€ "
            "GBP" -> "£ "
            else -> "$currency "
        }
    }

    private fun maskAccountNumber(accNum: String): String {
        return if (accNum.length > 4) {
            "•••• " + accNum.takeLast(4)
        } else {
            accNum
        }
    }
}
