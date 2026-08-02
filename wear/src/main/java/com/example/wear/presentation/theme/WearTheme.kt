package com.example.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val InvestecNavy = Color(0xFF001B3E)
val InvestecNavyLight = Color(0xFF0A2E5C)
val InvestecGold = Color(0xFFC5A059)
val InvestecBlueAccent = Color(0xFF80D4FF)

val DebitRed = Color(0xFFFF5252)
val CreditGreen = Color(0xFF00E676)

val SurfaceDark = Color(0xFF162032)
val CardBackground = Color(0xFF1E293B)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF94A3B8)

val WearColorPalette = Colors(
    primary = InvestecBlueAccent,
    primaryVariant = InvestecNavyLight,
    secondary = InvestecGold,
    secondaryVariant = InvestecNavy,
    background = Color.Black,
    surface = SurfaceDark,
    error = DebitRed,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = Color.White
)

@Composable
fun WearBankingTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = WearColorPalette,
        content = content
    )
}
