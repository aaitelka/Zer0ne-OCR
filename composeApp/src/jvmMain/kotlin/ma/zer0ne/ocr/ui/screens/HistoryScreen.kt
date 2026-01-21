package ma.zer0ne.ocr.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.zer0ne.ocr.ui.theme.AppTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ExcelFile(
    val file: File,
    val dateCreated: String,
    val fileSize: String
)

@Composable
fun HistoryScreen() {
    var excelFiles by remember { mutableStateOf(listOf<ExcelFile>()) }
    var fileToDelete by remember { mutableStateOf<ExcelFile?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val colors = AppTheme.colors

    LaunchedEffect(Unit) {
        // Load saved Excel files from desktop/masdac-ocr-xlsx folder
        val desktopDir = File(System.getProperty("user.home"), "Desktop")
        val excelDir = File(desktopDir, "masdac-ocr-xlsx")

        if (excelDir.exists() && excelDir.isDirectory) {
            val files = excelDir.listFiles()?.filter { it.name.endsWith(".xlsx") } ?: emptyList()
            excelFiles = files.sortedByDescending { it.lastModified() }.map { file ->
                val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
                val date = dateFormat.format(Date(file.lastModified()))
                val sizeInMB = String.format("%.2f MB", file.length() / (1024.0 * 1024.0))
                ExcelFile(file, date, sizeInMB)
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation && fileToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                fileToDelete = null
            },
            title = {
                Text(
                    text = "Delete File",
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${fileToDelete!!.file.name}\"?",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileToDelete?.let { file ->
                            if (file.file.delete()) {
                                excelFiles = excelFiles.filter { it.file.path != file.file.path }
                            }
                        }
                        showDeleteConfirmation = false
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        fileToDelete = null
                    }
                ) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.bgMedium,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Saved Excel Files",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (excelFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Excel files saved yet",
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Files will appear here after conversion",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(excelFiles) { excelFile ->
                    ExcelFileItem(
                        excelFile = excelFile,
                        onOpen = {
                            try {
                                java.awt.Desktop.getDesktop().open(excelFile.file)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        onDelete = {
                            fileToDelete = excelFile
                            showDeleteConfirmation = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExcelFileItem(
    excelFile: ExcelFile,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = AppTheme.colors

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(8.dp),
        color = colors.bgMedium.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = excelFile.file.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.text
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = excelFile.dateCreated,
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = excelFile.fileSize,
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = colors.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
