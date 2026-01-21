package ma.zer0ne.ocr.ui.components.titlebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.zer0ne.ocr.ui.theme.AppTheme
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CustomTitleBar(
    window: ComposeWindow,
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit = { window.dispose() }
) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    val colors = AppTheme.colors

    LaunchedEffect(window) {
        setupNativeWindowDragging(window)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        color = colors.bgMedium.copy(alpha = 0.95f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title and Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(colors.primary, RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Invoice OCR",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            }

            // Window Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // Minimize button
                IconButton(
                    onClick = { window.isMinimized = true },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Minimize",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Close button
                IconButton(
                    onClick = { window.dispose() },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun setupNativeWindowDragging(window: ComposeWindow) {
    var dragStartX = 0
    var dragStartY = 0
    var windowStartX = 0
    var windowStartY = 0
    var isDragging = false

    val mouseListener = object : MouseListener {
        override fun mousePressed(e: MouseEvent) {
            if (e.y <= 56) {
                isDragging = true
                dragStartX = e.xOnScreen
                dragStartY = e.yOnScreen
                windowStartX = window.location.x
                windowStartY = window.location.y
            }
        }

        override fun mouseReleased(e: MouseEvent) {
            isDragging = false
        }

        override fun mouseClicked(e: MouseEvent) {}
        override fun mouseEntered(e: MouseEvent) {}
        override fun mouseExited(e: MouseEvent) {}
    }

    val motionListener = object : MouseMotionListener {
        override fun mouseDragged(e: MouseEvent) {
            if (isDragging) {
                val deltaX = e.xOnScreen - dragStartX
                val deltaY = e.yOnScreen - dragStartY
                window.location = java.awt.Point(
                    windowStartX + deltaX,
                    windowStartY + deltaY
                )
            }
        }

        override fun mouseMoved(e: MouseEvent) {}
    }

    window.addMouseListener(mouseListener)
    window.addMouseMotionListener(motionListener)
}
