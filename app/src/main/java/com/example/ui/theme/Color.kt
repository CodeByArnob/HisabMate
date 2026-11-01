package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ============================================================================
// ROOT-LEVEL SEMANTIC DESIGN TOKENS (Eye-Soothing Slate Canvas & Vibrant Cards)
// ============================================================================

// Original Vibrant Fintech Card Colors (Restored for crisp, lively cards)
val RoyalViolet = Color(0xFF7C5CFF)
val RoyalVioletDark = Color(0xFF5639CC)
val RoyalVioletLight = Color(0xFFE9E3FF)

val ElectricAzure = Color(0xFF1D9BF0)
val ElectricAzureDark = Color(0xFF0E6EB8)
val ElectricAzureLight = Color(0xFFDDF2FF)

val EmeraldMint = Color(0xFF0EA568)
val EmeraldMintDark = Color(0xFF087447)
val EmeraldMintLight = Color(0xFFD1FAE5)

val VibrantTangerine = Color(0xFFFF6E1A)
val VibrantTangerineDark = Color(0xFFCC4E06)
val VibrantTangerineLight = Color(0xFFFFE8D6)

val RoseMagenta = Color(0xFFEC4899)
val RoseMagentaDark = Color(0xFFBE185D)
val RoseMagentaLight = Color(0xFFFCE7F3)

val AmberGold = Color(0xFFF59E0B)
val AmberGoldDark = Color(0xFFB45309)
val AmberGoldLight = Color(0xFFFEF3C7)

val CyanTeal = Color(0xFF06B6D4)
val IndigoDeep = Color(0xFF4338CA)

// Light Mode Neutral Canvas & Soft-White Surface Hierarchy
val LightBackground = Color(0xFFEEF2F7)          // Neutral, low-glare cool-gray canvas
val LightSurface = Color(0xFFFAFBFD)             // Soft-white card surface
val LightSurfaceVariant = Color(0xFFE8EDF4)      // Distinct interactive container, inputs & recessed strips
val LightSurfaceElevated = Color(0xFFFFFFFF)     // Distinct elevated surface / dialogs / hero top layer
val LightSurfaceHighlight = Color(0xFFDCE4EE)    // Active/pressed container
val LightWarmCream = Color(0xFFF7ECDA)           // Warm amber insight container
val LightWarmCreamBorder = Color(0xFFD9A006)

// Light Mode High-Contrast Bold Ink Hierarchy
val LightInkPrimary = Color(0xFF070E1A)          // Ultra-crisp deepest slate-black primary text
val LightInkSecondary = Color(0xFF182435)        // Strong dark slate secondary text (never washed out)
val LightInkTertiary = Color(0xFF2E3D52)         // Clear readable tertiary metadata text
val LightBorderStrong = Color(0xFFBECAD8)        // Defined 1.1.dp structural border
val LightBorderSubtle = Color(0xFFDCE3EC)        // Clean row divider

// Dark Mode Obsidian Hierarchy
val DarkBackground = Color(0xFF0B0E14)
val DarkSurface = Color(0xFF141923)
val DarkSurfaceVariant = Color(0xFF1E2636)
val DarkSurfaceElevated = Color(0xFF232C3F)
val DarkSurfaceHighlight = Color(0xFF2C374F)
val DarkWarmCream = Color(0xFF262015)
val DarkWarmCreamBorder = Color(0xFF5C481D)

// Dark Mode Ink Hierarchy
val DarkInkPrimary = Color(0xFFF8FAFC)
val DarkInkSecondary = Color(0xFFCBD5E1)
val DarkInkTertiary = Color(0xFF94A3B8)
val DarkBorderStrong = Color(0xFF2A3449)
val DarkBorderSubtle = Color(0xFF1F2737)

// High-Contrast Financial Semantic Colors
val IncomeGreen = Color(0xFF059669)
val IncomeGreenDarkTheme = Color(0xFF34D399)
val IncomeGreenBgLight = Color(0xFFD1FAE5)
val IncomeGreenBgDark = Color(0xFF064E3B)

val ExpenseRed = Color(0xFFDC2626)
val ExpenseRedDarkTheme = Color(0xFFF87171)
val ExpenseRedBgLight = Color(0xFFFEE2E2)
val ExpenseRedBgDark = Color(0xFF450A0A)

val WarningAmber = Color(0xFFD97706)
val WarningAmberDarkTheme = Color(0xFFFBBF24)
val WarningAmberBgLight = Color(0xFFFEF3C7)
val WarningAmberBgDark = Color(0xFF451A03)

val ObsidianDock = Color(0xFF0F172A)
val ObsidianDockDark = Color(0xFF1E2536)

@Immutable
data class HisabExtendedColors(
    val isDark: Boolean,
    val canvasBackground: Color,
    val cardSurface: Color,
    val cardSurfaceElevated: Color,
    val containerInteractive: Color,
    val containerHighlight: Color,
    val borderStrong: Color,
    val borderSubtle: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val incomeText: Color,
    val incomeContainer: Color,
    val expenseText: Color,
    val expenseContainer: Color,
    val warningText: Color,
    val warningContainer: Color,
    val insightBannerBg: Color,
    val insightBannerBorder: Color,
    val dockBackground: Color
)

val LightHisabExtendedColors = HisabExtendedColors(
    isDark = false,
    canvasBackground = LightBackground,
    cardSurface = LightSurface,
    cardSurfaceElevated = LightSurfaceElevated,
    containerInteractive = LightSurfaceVariant,
    containerHighlight = LightSurfaceHighlight,
    borderStrong = LightBorderStrong,
    borderSubtle = LightBorderSubtle,
    inkPrimary = LightInkPrimary,
    inkSecondary = LightInkSecondary,
    inkTertiary = LightInkTertiary,
    incomeText = Color(0xFF047857),
    incomeContainer = IncomeGreenBgLight,
    expenseText = Color(0xFFB91C1C),
    expenseContainer = ExpenseRedBgLight,
    warningText = Color(0xFFB45309),
    warningContainer = WarningAmberBgLight,
    insightBannerBg = LightWarmCream,
    insightBannerBorder = LightWarmCreamBorder,
    dockBackground = ObsidianDock
)

val DarkHisabExtendedColors = HisabExtendedColors(
    isDark = true,
    canvasBackground = DarkBackground,
    cardSurface = DarkSurface,
    cardSurfaceElevated = DarkSurfaceElevated,
    containerInteractive = DarkSurfaceVariant,
    containerHighlight = DarkSurfaceHighlight,
    borderStrong = DarkBorderStrong,
    borderSubtle = DarkBorderSubtle,
    inkPrimary = DarkInkPrimary,
    inkSecondary = DarkInkSecondary,
    inkTertiary = DarkInkTertiary,
    incomeText = IncomeGreenDarkTheme,
    incomeContainer = IncomeGreenBgDark,
    expenseText = ExpenseRedDarkTheme,
    expenseContainer = ExpenseRedBgDark,
    warningText = WarningAmberDarkTheme,
    warningContainer = WarningAmberBgDark,
    insightBannerBg = DarkWarmCream,
    insightBannerBorder = DarkWarmCreamBorder,
    dockBackground = ObsidianDockDark
)

val LocalHisabColors = staticCompositionLocalOf { LightHisabExtendedColors }

data class CategoryColorPair(
    val main: Color,
    val rim: Color,
    val highContrastTextLight: Color,
    val softBgLight: Color,
    val softBgDark: Color
)

/**
 * Restores the original vibrant, saturated card colors for WalletFolderCard & Account cards
 * while keeping high-contrast label colors for light/dark text badges.
 */
fun getCategoryPalette(colorHex: Long): CategoryColorPair {
    return when (colorHex) {
        0xFF7C5CFFL, 0xFF6D4AFFL, 0xFF845EF7L -> CategoryColorPair(
            main = Color(0xFF6844D8),
            rim = Color(0xFF4B2BAF),
            highContrastTextLight = Color(0xFF4C1D95),
            softBgLight = Color(0xFFE9E3FF),
            softBgDark = Color(0xFF2E1F5E)
        )
        0xFF1D9BF0L, 0xFF0284C7L, 0xFF228BE6L -> CategoryColorPair(
            main = Color(0xFF0868B2),
            rim = Color(0xFF07518A),
            highContrastTextLight = Color(0xFF075985),
            softBgLight = Color(0xFFDDF2FF),
            softBgDark = Color(0xFF0C3B5E)
        )
        0xFF0EA568L, 0xFF059669L, 0xFF12B886L -> CategoryColorPair(
            main = Color(0xFF087747),
            rim = Color(0xFF065C36),
            highContrastTextLight = Color(0xFF065F46),
            softBgLight = Color(0xFFD1FAE5),
            softBgDark = Color(0xFF064E3B)
        )
        0xFFFF6E1AL, 0xFFEA580CL, 0xFFFD7E14L -> CategoryColorPair(
            main = Color(0xFFC64B08),
            rim = Color(0xFF983805),
            highContrastTextLight = Color(0xFF9A3412),
            softBgLight = Color(0xFFFFE8D6),
            softBgDark = Color(0xFF4A1D08)
        )
        0xFFEC4899L, 0xFFDB2777L -> CategoryColorPair(
            main = Color(0xFFB91C63),
            rim = Color(0xFF8E174C),
            highContrastTextLight = Color(0xFF9D174D),
            softBgLight = Color(0xFFFCE7F3),
            softBgDark = Color(0xFF501131)
        )
        0xFFF59E0BL, 0xFFD97706L -> CategoryColorPair(
            main = Color(0xFF925B00),
            rim = Color(0xFF714600),
            highContrastTextLight = Color(0xFF92400E),
            softBgLight = Color(0xFFFEF3C7),
            softBgDark = Color(0xFF451A03)
        )
        0xFF06B6D4L, 0xFF0891B2L -> CategoryColorPair(
            main = Color(0xFF0E7490),
            rim = Color(0xFF0B586D),
            highContrastTextLight = Color(0xFF155E75),
            softBgLight = Color(0xFFCFFAFE),
            softBgDark = Color(0xFF083344)
        )
        else -> {
            val base = Color(colorHex)
            CategoryColorPair(
                main = base,
                rim = base.copy(alpha = 0.82f),
                highContrastTextLight = base,
                softBgLight = base.copy(alpha = 0.18f),
                softBgDark = base.copy(alpha = 0.26f)
            )
        }
    }
}
