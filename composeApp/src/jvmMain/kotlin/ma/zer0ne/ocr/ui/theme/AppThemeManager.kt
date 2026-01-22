package ma.zer0ne.ocr.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * Manager for theme and color scheme creation
 */
object AppThemeManager {
    /**
     * Create color scheme based on theme preference
     */
    fun createColorScheme(isDarkTheme: Boolean): ColorScheme {
        val themeColors: AppColorScheme = if (isDarkTheme) AIColors else AILightColors

        return if (isDarkTheme) {
            darkColorScheme(
                background = themeColors.bgDark,
                surface = themeColors.bgMedium,
                primary = themeColors.primary,
                onBackground = themeColors.text,
                onSurface = themeColors.text,
                error = themeColors.error
            )
        } else {
            lightColorScheme(
                background = themeColors.bgDark,
                surface = themeColors.bgMedium,
                primary = themeColors.primary,
                onBackground = themeColors.text,
                onSurface = themeColors.text,
                error = themeColors.error
            )
        }
    }
}
