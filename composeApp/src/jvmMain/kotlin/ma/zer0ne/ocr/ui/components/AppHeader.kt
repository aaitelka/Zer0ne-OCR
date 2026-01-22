package ma.zer0ne.ocr.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ma.zer0ne.ocr.ui.theme.AppTheme
import ocr.composeapp.generated.resources.Res
import ocr.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource

/**
 * App header with logo, title and settings dropdown
 */
@Composable
fun AppHeader(
    onSettingsClick: () -> Unit,
    onExitClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = 24.dp, end = 16.dp)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(Res.drawable.compose_multiplatform),
                contentDescription = "App Logo",
                tint = AppTheme.colors.primary,
                modifier = Modifier.size(48.dp).padding(8.dp)
            )
            Column {
                Text(
                    "Zer0ne",
                    style = AppTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary
                )
                Text(
                    "OCR",
                    style = AppTheme.typography.bodyLarge,
                    color = AppTheme.colors.text
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Settings dropdown aligned to the right
            SettingsDropdown(
                onSettingsClick = onSettingsClick,
                onExitClick = onExitClick,
                modifier = Modifier.size(24.dp).padding(top = 8.dp)
            )
        }
    }
}

/**
 * Custom snackbar host with themed styling
 */
@Composable
fun AppSnackbarHost(snackbarHostState: SnackbarHostState) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(16.dp),
        snackbar = { snackbarData ->
            Snackbar(
                modifier = Modifier.padding(12.dp).clip(RoundedCornerShape(8.dp)),
                containerColor = AppTheme.colors.bgMedium,
                contentColor = AppTheme.colors.text,
                action = {
                    if (snackbarData.visuals.actionLabel != null) {
                        TextButton(onClick = { snackbarData.performAction() }) {
                            Text(
                                text = snackbarData.visuals.actionLabel!!,
                                color = Color.White
                            )
                        }
                    }
                },
                dismissAction = {
                    IconButton(
                        onClick = { snackbarData.dismiss() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = AppTheme.colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            ) {
                Text(text = snackbarData.visuals.message)
            }
        }
    )
}
