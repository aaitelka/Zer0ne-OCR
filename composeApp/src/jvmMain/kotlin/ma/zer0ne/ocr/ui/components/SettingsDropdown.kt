package ma.zer0ne.ocr.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import ma.zer0ne.ocr.ui.theme.AppTheme

/**
 * Settings dropdown menu component
 */
@Composable
fun SettingsDropdown(
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    val colors = AppTheme.colors

    Box(modifier = modifier) {
        IconButton(
            onClick = { showDropdownMenu = true },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        if (showDropdownMenu) {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { showDropdownMenu = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 28.dp)
                        .shadow(8.dp, RoundedCornerShape(8.dp))
                        .widthIn(min = 140.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = colors.bgMedium
                ) {
                    Column(
                        modifier = Modifier.width(IntrinsicSize.Max)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .clickable {
                                    showDropdownMenu = false
                                    onSettingsClick()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "Settings",
                                color = colors.text,
                                style = AppTheme.typography.labelSmall
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                .clickable {
                                    showDropdownMenu = false
                                    onExitClick()
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "Exit",
                                color = colors.error,
                                style = AppTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
