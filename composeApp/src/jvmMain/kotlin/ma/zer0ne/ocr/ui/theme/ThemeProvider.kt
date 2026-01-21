package ma.zer0ne.ocr.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// CompositionLocal for current theme colors - accessible anywhere in the compose tree
val LocalAppColors = compositionLocalOf<AppColorScheme> { AIColors }

// CompositionLocal for theme state (dark/light)
val LocalIsDarkTheme = compositionLocalOf { true }

// Typography definitions
object AppTypography {
    val titleLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    )

    val titleMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.15.sp
    )

    val titleSmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    )

    val bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp
    )

    val bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.25.sp
    )

    val bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp
    )

    val labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    )

    val labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )

    val labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )

    val labelTiny = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp
    )
}

// Theme state holder
class ThemeState(initialIsDark: Boolean = true) {
    var isDarkTheme by mutableStateOf(initialIsDark)

    val colors: AppColorScheme
        get() = if (isDarkTheme) AIColors else AILightColors

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
    }
}

// CompositionLocal for theme state management
val LocalThemeState = compositionLocalOf<ThemeState> { error("ThemeState not provided") }

/**
 * Theme provider that wraps the app content and provides theme colors via CompositionLocal
 */
@Composable
fun AppThemeProvider(
    isDarkTheme: Boolean = true,
    onThemeChange: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    val themeState = remember { ThemeState(isDarkTheme) }

    // Update theme state when isDarkTheme changes
    themeState.isDarkTheme = isDarkTheme

    val colors = if (isDarkTheme) AIColors else AILightColors

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalIsDarkTheme provides isDarkTheme,
        LocalThemeState provides themeState
    ) {
        content()
    }
}

/**
 * Convenience object to access theme colors and typography from any composable
 * Usage: AppTheme.colors.primary, AppTheme.typography.titleLarge, etc.
 */
object AppTheme {
    val colors: AppColorScheme
        @Composable
        get() = LocalAppColors.current

    val isDark: Boolean
        @Composable
        get() = LocalIsDarkTheme.current

    val typography = AppTypography
}
