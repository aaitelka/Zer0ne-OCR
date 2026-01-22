package ma.zer0ne.ocr.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ma.zer0ne.ocr.config.SecureApiKeyManager
import ma.zer0ne.ocr.ui.theme.AppTheme


@Composable
@Preview(showSystemUi = true)
fun SettingsScreenPreview() {
    SettingsScreen(
        onDarkThemeChange = {},
        onBack = {}
    )
}


@Composable
fun SettingsScreen(
    onDarkThemeChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val colors = AppTheme.colors
    val isDarkTheme = AppTheme.isDark

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowLeft,
                    contentDescription = "Back",
                    tint = colors.text
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                style = AppTheme.typography.labelSmall,
                color = colors.text
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Appearance Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = colors.bgMedium.copy(alpha = 0.7f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Section Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Appearance",
                        style = AppTheme.typography.labelSmall,
                        color = colors.text
                    )
                }

                HorizontalDivider(
                    color = colors.bgLight.copy(alpha = 0.5f),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Theme Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.bgLight.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (isDarkTheme) colors.primary else colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dark Theme",
                                style = AppTheme.typography.bodySmall,
                                color = colors.text
                            )
                            Text(
                                text = if (isDarkTheme) "Currently using dark mode" else "Currently using light mode",
                                style = AppTheme.typography.labelTiny,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onDarkThemeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.primary,
                            checkedTrackColor = colors.primary.copy(alpha = 0.5f),
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.bgLight
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ApiKeysSection()

        Spacer(modifier = Modifier.weight(1f))

        // Version info at bottom
        Text(
            text = "Invoice OCR v1.0.0",
            style = AppTheme.typography.labelTiny,
            color = colors.textSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun ApiKeysSection() {
    val colors = AppTheme.colors
    val keyManager = remember { SecureApiKeyManager.getInstance() }
    val apiKeys by keyManager.apiKeysFlow.collectAsState()
    var newKeyText by remember { mutableStateOf("") }
    var showNewKeyField by remember { mutableStateOf(false) }
    var showKeyPassword by remember { mutableStateOf(false) }
    var keyToDelete by remember { mutableStateOf<Int?>(null) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.bgMedium.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "API Keys",
                    style = AppTheme.typography.labelSmall,
                    color = colors.text
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${apiKeys.size} key(s)",
                    style = AppTheme.typography.labelTiny,
                    color = colors.textSecondary
                )
            }

            HorizontalDivider(
                color = colors.bgLight.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = "API keys are securely encrypted and stored locally. Add multiple keys to enable automatic rotation and avoid rate limits.",
                style = AppTheme.typography.labelTiny,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Existing Keys List
            if (apiKeys.isNotEmpty()) {
                apiKeys.forEachIndexed { index, maskedKey ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.bgLight.copy(alpha = 0.3f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = colors.success,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = maskedKey,
                            style = AppTheme.typography.bodySmall,
                            color = colors.text,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                keyToDelete = index
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete key",
                                tint = colors.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (index < apiKeys.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Text(
                    text = "No API keys stored.",
                    style = AppTheme.typography.labelTiny,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Add New Key Section
            AnimatedVisibility(visible = showNewKeyField) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.bgLight.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = newKeyText,
                        onValueChange = { newKeyText = it },
                        placeholder = {
                            Text(
                                "gsk_...",
                                style = AppTheme.typography.labelTiny,
                                color = colors.textSecondary.copy(alpha = 0.5f)
                            )
                        },
                        singleLine = true,
                        textStyle = AppTheme.typography.bodySmall.copy(color = colors.text),
                        visualTransformation = if (showKeyPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = { showKeyPassword = !showKeyPassword },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (showKeyPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showKeyPassword) "Hide" else "Show",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = colors.text,
                            unfocusedTextColor = colors.text,
                            unfocusedPlaceholderColor = colors.textSecondary.copy(alpha = 0.5f),
                            focusedPlaceholderColor = colors.textSecondary.copy(alpha = 0.5f),
                            cursorColor = colors.primary
                        )
                    )
                    Button(
                        onClick = {
                            if (newKeyText.isNotBlank()) {
                                keyManager.addKey(newKeyText)
                                newKeyText = ""
                                showNewKeyField = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.success),
                        modifier = Modifier.size(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (!showNewKeyField) {
                Button(
                    onClick = { showNewKeyField = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Add API Key", style = AppTheme.typography.labelMedium, color = Color.White)
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (keyToDelete != null) {
        AlertDialog(
            onDismissRequest = { keyToDelete = null },
            title = { Text("Delete API Key", style = AppTheme.typography.labelSmall, color = colors.text) },
            text = {
                Text(
                    "Are you sure you want to delete this API key?",
                    style = AppTheme.typography.labelTiny,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        keyToDelete?.let { index ->
                            val allKeys = keyManager.getAllKeys()
                            if (index < allKeys.size) {
                                keyManager.removeKey(allKeys[index])
                            }
                        }
                        keyToDelete = null
                    }
                ) {
                    Text("Delete", style = AppTheme.typography.labelSmall, color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { keyToDelete = null }) {
                    Text("Cancel", style = AppTheme.typography.labelSmall, color = Color.White)
                }
            },
            containerColor = colors.bgMedium,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
