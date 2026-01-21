package ma.zer0ne.ocr.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ma.zer0ne.ocr.ui.theme.AppTheme
import ma.zer0ne.ocr.utils.getAllSupportedFiles
import java.awt.FileDialog
import java.io.File


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DropZone(
    onFilesSelected: (List<File>) -> Unit,
    window: ComposeWindow
) {
    var isHovered by remember { mutableStateOf(false) }
    val colors = AppTheme.colors

    val borderColor = if (isHovered) colors.primary else colors.accent.copy(alpha = 0.5f)
    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = dashPathEffect
                    ),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
            .background(colors.bgMedium.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .onClick {
                val fileDialog = FileDialog(window, "Select Invoice Files or Folder", FileDialog.LOAD)
                fileDialog.isMultipleMode = true
                fileDialog.isVisible = true

                val selectedFiles = fileDialog.files
                if (selectedFiles.isNotEmpty()) {
                    val allFiles = getAllSupportedFiles(selectedFiles.toList())
                    if (allFiles.isNotEmpty()) {
                        onFilesSelected(allFiles)
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = "Upload",
                modifier = Modifier.size(48.dp),
                tint = colors.accent
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Drop invoice files or folder here",
                fontSize = 15.sp,
                color = colors.text,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "or click to browse (JPG, PNG, PDF)",
                fontSize = 12.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}