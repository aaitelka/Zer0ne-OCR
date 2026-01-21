package ma.zer0ne.ocr.ui.components.background

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import ma.zer0ne.ocr.ui.theme.AppTheme
import ma.zer0ne.ocr.ui.theme.JBColors

@Composable
fun AnimatedBackgroundBubbles() {
    val colors = AppTheme.colors

    // Smooth infinite transitions for continuous animation
    val infiniteTransition1 = rememberInfiniteTransition(label = "bubble1")
    val infiniteTransition2 = rememberInfiniteTransition(label = "bubble2")
    val infiniteTransition3 = rememberInfiniteTransition(label = "bubble3")
    val infiniteTransition4 = rememberInfiniteTransition(label = "bubble4")

    // Smooth Y-axis animations with extended duration for buttery smoothness
    val offsetY1 by infiniteTransition1.animateFloat(
        initialValue = -150f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY1"
    )

    val offsetY2 by infiniteTransition2.animateFloat(
        initialValue = 100f,
        targetValue = -100f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY2"
    )

    val offsetY3 by infiniteTransition3.animateFloat(
        initialValue = -120f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY3"
    )

    val offsetY4 by infiniteTransition4.animateFloat(
        initialValue = 80f,
        targetValue = -80f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY4"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Bubble 1 - Cyan/Blue gradient (Top-Left)
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(y = offsetY1.dp, x = (-100).dp)
                .align(Alignment.TopStart)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            JBColors.cyan.copy(alpha = 0.15f),
                            JBColors.blue.copy(alpha = 0.08f),
                            JBColors.blue.copy(alpha = 0.02f),
                            JBColors.blue.copy(alpha = 0.0f)
                        ),
                        radius = 200f
                    ),
                    shape = CircleShape
                )
        )

        // Bubble 2 - Purple/Magenta gradient (Bottom-Right)
        Box(
            modifier = Modifier
                .size(350.dp)
                .offset(y = offsetY2.dp, x = 100.dp)
                .align(Alignment.BottomEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            JBColors.purple.copy(alpha = 0.12f),
                            JBColors.magenta.copy(alpha = 0.06f),
                            JBColors.pink.copy(alpha = 0.02f),
                            JBColors.pink.copy(alpha = 0.0f)
                        ),
                        radius = 175f
                    ),
                    shape = CircleShape
                )
        )

        // Bubble 3 - Green/Lime gradient (Center-Right)
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(y = offsetY3.dp, x = 80.dp)
                .align(Alignment.CenterEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            JBColors.green.copy(alpha = 0.10f),
                            JBColors.lime.copy(alpha = 0.05f),
                            JBColors.lime.copy(alpha = 0.01f),
                            JBColors.lime.copy(alpha = 0.0f)
                        ),
                        radius = 140f
                    ),
                    shape = CircleShape
                )
        )

        // Bubble 4 - Orange/Yellow gradient (Top-Right)
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(y = offsetY4.dp, x = 50.dp)
                .align(Alignment.TopEnd)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            JBColors.orange.copy(alpha = 0.10f),
                            JBColors.yellow.copy(alpha = 0.05f),
                            JBColors.yellow.copy(alpha = 0.01f),
                            JBColors.yellow.copy(alpha = 0.0f)
                        ),
                        radius = 125f
                    ),
                    shape = CircleShape
                )
        )
    }
}

private val EaseInOutQuad: Easing = object : Easing {
    override fun transform(fraction: Float): Float {
        return if (fraction < 0.5) {
            2 * fraction * fraction
        } else {
            1 - (-2 * fraction + 2).let { it * it / 2 }
        }
    }
}
