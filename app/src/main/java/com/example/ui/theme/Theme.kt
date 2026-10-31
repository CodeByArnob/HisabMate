package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val LightFintechColorScheme = lightColorScheme(
    primary = RoyalViolet,
    onPrimary = Color.White,
    primaryContainer = RoyalVioletLight,
    onPrimaryContainer = Color(0xFF3B1999),
    secondary = ElectricAzure,
    onSecondary = Color.White,
    secondaryContainer = ElectricAzureLight,
    onSecondaryContainer = Color(0xFF075985),
    tertiary = EmeraldMint,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldMintLight,
    onTertiaryContainer = Color(0xFF065F46),
    background = LightBackground,
    onBackground = LightInkPrimary,
    surface = LightSurface,
    onSurface = LightInkPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightInkSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = LightSurfaceHighlight,
    surfaceContainerHighest = Color(0xFFD0D9E8),
    outline = LightBorderStrong,
    outlineVariant = LightBorderSubtle,
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = ExpenseRedBgLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkFintechColorScheme = darkColorScheme(
    primary = Color(0xFF8B6EFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2B2159),
    onPrimaryContainer = Color(0xFFE0D7FF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF064E3B),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = DarkBackground,
    onBackground = DarkInkPrimary,
    surface = DarkSurface,
    onSurface = DarkInkPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkInkSecondary,
    surfaceContainerLowest = Color(0xFF080A0F),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceElevated,
    surfaceContainerHighest = DarkSurfaceHighlight,
    outline = DarkBorderStrong,
    outlineVariant = DarkBorderSubtle,
    error = ExpenseRedDarkTheme,
    onError = Color(0xFF450A0A),
    errorContainer = ExpenseRedBgDark,
    onErrorContainer = Color(0xFFFECACA)
)

val FintechShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

object HisabTheme {
    val colors: HisabExtendedColors
        @Composable
        get() = LocalHisabColors.current
}

@Composable
fun HisabMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkFintechColorScheme else LightFintechColorScheme
    val extendedColors = if (darkTheme) DarkHisabExtendedColors else LightHisabExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalHisabColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = FintechShapes,
            content = content
        )
    }
}
