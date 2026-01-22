package ma.zer0ne.ocr.ui.components.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import ma.zer0ne.ocr.ui.theme.JBColors
import ma.zer0ne.ocr.ui.theme.LocalIsDarkTheme

@Composable
fun AnimatedBackgroundBubbles() {
    val isDarkTheme = LocalIsDarkTheme.current
    // Higher alpha multiplier for light mode to make gradients visible
    val themeAlpha = if (isDarkTheme) 1f else 2.5f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Two-cell gradient background - Top-Left and Bottom-Right

        // Cell 1 - Blue/Cyan/Purple (Top-Left corner, extends to center)
        for (i in 0..3) {
            val offsetMultiplier = 1f + (i * 0.08f)
            val alphaMultiplier = (1f - (i * 0.2f)) * themeAlpha
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to JBColors.blue.copy(alpha = (0.08f * alphaMultiplier).coerceAtMost(1f)),
                        0.08f to JBColors.blue.copy(alpha = (0.065f * alphaMultiplier).coerceAtMost(1f)),
                        0.15f to JBColors.cyan.copy(alpha = (0.05f * alphaMultiplier).coerceAtMost(1f)),
                        0.25f to JBColors.cyan.copy(alpha = (0.038f * alphaMultiplier).coerceAtMost(1f)),
                        0.35f to JBColors.purple.copy(alpha = (0.028f * alphaMultiplier).coerceAtMost(1f)),
                        0.45f to JBColors.purple.copy(alpha = (0.018f * alphaMultiplier).coerceAtMost(1f)),
                        0.55f to JBColors.purple.copy(alpha = (0.012f * alphaMultiplier).coerceAtMost(1f)),
                        0.65f to JBColors.purple.copy(alpha = (0.007f * alphaMultiplier).coerceAtMost(1f)),
                        0.75f to JBColors.purple.copy(alpha = (0.004f * alphaMultiplier).coerceAtMost(1f)),
                        0.85f to JBColors.purple.copy(alpha = (0.002f * alphaMultiplier).coerceAtMost(1f)),
                        0.95f to JBColors.purple.copy(alpha = (0.001f * alphaMultiplier).coerceAtMost(1f)),
                        1.00f to Color.Transparent
                    ),
                    center = Offset(-width * 0.2f, -height * 0.2f),
                    radius = width * 1.3f * offsetMultiplier
                ),
                center = Offset(-width * 0.2f, -height * 0.2f),
                radius = width * 1.3f * offsetMultiplier
            )
        }

        // Cell 2 - Pink/Magenta/Orange (Bottom-Right corner, extends to center)
        for (i in 0..3) {
            val offsetMultiplier = 1f + (i * 0.08f)
            val alphaMultiplier = (1f - (i * 0.2f)) * themeAlpha
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to JBColors.pink.copy(alpha = (0.07f * alphaMultiplier).coerceAtMost(1f)),
                        0.08f to JBColors.pink.copy(alpha = (0.055f * alphaMultiplier).coerceAtMost(1f)),
                        0.15f to JBColors.magenta.copy(alpha = (0.042f * alphaMultiplier).coerceAtMost(1f)),
                        0.25f to JBColors.magenta.copy(alpha = (0.032f * alphaMultiplier).coerceAtMost(1f)),
                        0.35f to JBColors.orange.copy(alpha = (0.022f * alphaMultiplier).coerceAtMost(1f)),
                        0.45f to JBColors.orange.copy(alpha = (0.015f * alphaMultiplier).coerceAtMost(1f)),
                        0.55f to JBColors.purple.copy(alpha = (0.01f * alphaMultiplier).coerceAtMost(1f)),
                        0.65f to JBColors.purple.copy(alpha = (0.006f * alphaMultiplier).coerceAtMost(1f)),
                        0.75f to JBColors.purple.copy(alpha = (0.003f * alphaMultiplier).coerceAtMost(1f)),
                        0.85f to JBColors.purple.copy(alpha = (0.002f * alphaMultiplier).coerceAtMost(1f)),
                        0.95f to JBColors.purple.copy(alpha = (0.001f * alphaMultiplier).coerceAtMost(1f)),
                        1.00f to Color.Transparent
                    ),
                    center = Offset(width * 1.2f, height * 1.2f),
                    radius = width * 1.3f * offsetMultiplier
                ),
                center = Offset(width * 1.2f, height * 1.2f),
                radius = width * 1.3f * offsetMultiplier
            )
        }
    }
}
