package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class FinancialColors(
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val skyBlue: Color,
    val skyBlueContainer: Color,
    val gold: Color,
    val goldDeep: Color,
    val goldContainer: Color,
    val border: Color,
    val heroStart: Color,
    val heroEnd: Color
)

val LocalFinancialColors = staticCompositionLocalOf {
    FinancialColors(
        income = IncomeGreen,
        incomeContainer = IncomeGreenSoft,
        expense = ExpenseRed,
        expenseContainer = ExpenseRedSoft,
        skyBlue = SkyBluePrimary,
        skyBlueContainer = SkyBlueSoft,
        gold = GoldAccent,
        goldDeep = GoldDeep,
        goldContainer = GoldSoft,
        border = SlateBorder,
        heroStart = Color(0xFF05131F),
        heroEnd = Color(0xFF0C273D)
    )
}

private val BlackFintechColorScheme = darkColorScheme(
    primary = SkyBluePrimary,
    onPrimary = Color(0xFF001E2E),
    primaryContainer = SkyBlueSoft,
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = SkyBlueAccent,
    onSecondary = Color(0xFF001E2E),
    secondaryContainer = SkyBlueSoft,
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = GoldAccent,
    onTertiary = Color(0xFF261A00),
    tertiaryContainer = GoldSoft,
    onTertiaryContainer = Color(0xFFFDE68A),
    background = PureBlack,
    onBackground = TextPrimaryLight,
    surface = OnyxSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CharcoalCard,
    onSurfaceVariant = TextSecondaryMuted,
    outline = SlateBorder,
    outlineVariant = Color(0xFF18202C),
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = ExpenseRedSoft,
    onErrorContainer = Color(0xFFFECACA)
)

private val LedgerShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LedgerPulseTheme(
    content: @Composable () -> Unit
) {
    val financialColors = FinancialColors(
        income = IncomeGreen,
        incomeContainer = IncomeGreenSoft,
        expense = ExpenseRed,
        expenseContainer = ExpenseRedSoft,
        skyBlue = SkyBluePrimary,
        skyBlueContainer = SkyBlueSoft,
        gold = GoldAccent,
        goldDeep = GoldDeep,
        goldContainer = GoldSoft,
        border = SlateBorder,
        heroStart = Color(0xFF041421),
        heroEnd = Color(0xFF0A2A42)
    )

    CompositionLocalProvider(LocalFinancialColors provides financialColors) {
        MaterialTheme(
            colorScheme = BlackFintechColorScheme,
            typography = LedgerTypography,
            shapes = LedgerShapes,
            content = content
        )
    }
}
