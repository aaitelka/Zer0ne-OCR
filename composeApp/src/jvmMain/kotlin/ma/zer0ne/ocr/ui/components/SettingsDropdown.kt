package ma.zer0ne.ocr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false },
            modifier = Modifier
                .background(colors.bgMedium, RoundedCornerShape(8.dp))
                .widthIn(min = 140.dp)
        ) {
            DropdownMenuItem(
                modifier = Modifier.height(24.dp).padding(vertical = 4.dp),
                text = {
                    Text(
                        text = "Settings",
                        color = colors.text,
                        style = AppTheme.typography.labelSmall
                    )
                },
                onClick = {
                    showDropdownMenu = false
                    onSettingsClick()
                }
            )
            DropdownMenuItem(
                modifier = Modifier.height(24.dp).padding(2.dp),
                text = {
                    Text(
                        text = "Exit",
                        color = colors.error,
                        style = AppTheme.typography.labelSmall
                    )
                },
                onClick = {
                    showDropdownMenu = false
                    onExitClick()
                }
            )
        }
    }
}
