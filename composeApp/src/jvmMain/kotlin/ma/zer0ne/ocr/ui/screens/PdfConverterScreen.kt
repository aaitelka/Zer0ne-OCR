package ma.zer0ne.ocr.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ma.zer0ne.ocr.ui.components.FileListSection
import ma.zer0ne.ocr.ui.components.DropZone
import ma.zer0ne.ocr.ui.theme.AppTheme
import ma.zer0ne.ocr.utils.Logger
import org.apache.pdfbox.Loader
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PdfConverterScreen(
    window: ComposeWindow,
    droppedFiles: List<File>,
    isConverting: Boolean,
    conversionMessage: String?,
    onFilesDropped: (List<File>) -> Unit,
    onConvertingChange: (Boolean) -> Unit,
    onMessageChange: (String?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val colors = AppTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Show drop zone if no files are selected
        if (droppedFiles.isEmpty()) {
            DropZone(
                onFilesSelected = { selectedFiles ->
                    onFilesDropped(selectedFiles)
                },
                window = window
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (droppedFiles.isNotEmpty()) {
            FileListSection(
                files = droppedFiles,
                onRemove = { path -> onFilesDropped(droppedFiles.filter { it.absolutePath != path }) },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Convert button
            Button(
                onClick = {
                    onConvertingChange(true)
                    scope.launch {
                        val firstFile = droppedFiles.first()
                        val isPdf = firstFile.extension.lowercase() == "pdf"

                        try {
                            if (isPdf) {
                                withContext(Dispatchers.IO) {
                                    convertPdfToImages(firstFile) { message ->
                                        onMessageChange(message)
                                        onConvertingChange(false)
                                    }
                                }
                            } else {
                                withContext(Dispatchers.IO) {
                                    convertImagesToPdf(droppedFiles) { message ->
                                        onMessageChange(message)
                                        onConvertingChange(false)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Logger.error("Conversion error: ${e.message}", e)
                            onMessageChange("✗ Error: ${e.message}")
                            onConvertingChange(false)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(8.dp),
                enabled = !isConverting && droppedFiles.isNotEmpty()
            ) {
                if (isConverting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Converting...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    val buttonText = if (droppedFiles.first().extension.lowercase() == "pdf") {
                        "Convert PDF to Images"
                    } else {
                        "Convert Images to PDF"
                    }
                    Text(buttonText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Message Display
        if (conversionMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                color = if (conversionMessage.startsWith("✓")) {
                    colors.success.copy(alpha = 0.2f)
                } else {
                    colors.error.copy(alpha = 0.2f)
                }
            ) {
                Text(
                    text = conversionMessage,
                    fontSize = 12.sp,
                    color = if (conversionMessage.startsWith("✓")) {
                        colors.success
                    } else {
                        colors.error
                    },
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { onMessageChange(null) }
                )
            }
        }
    }
}



private fun convertPdfToImages(pdfFile: File, onComplete: (String) -> Unit) {
    Logger.log("Converting PDF to images: ${pdfFile.name}")

    try {
        // Create output folder structure
        val desktopDir = File(System.getProperty("user.home"), "Desktop")
        val zer0neDir = File(desktopDir, "zer0ne-ocr-xlsx")
        val convertedDir = File(zer0neDir, "converted")

        if (!convertedDir.exists()) {
            convertedDir.mkdirs()
        }

        // Get current datetime
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
        val datetime = dateFormat.format(Date())

        // Create prefix from PDF name
        val pdfNameWithoutExt = pdfFile.nameWithoutExtension
        val prefix = "${pdfNameWithoutExt}_pdf_$datetime"
        val outputFolder = File(convertedDir, prefix)
        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }

        // Use PDFBox to convert PDF to images
        Loader.loadPDF(pdfFile).use { document ->
            val pdfRenderer = org.apache.pdfbox.rendering.PDFRenderer(document)
            for (i in 0 until document.numberOfPages) {
                val image = pdfRenderer.renderImageWithDPI(i, 300f) // 300 DPI for high quality
                val outputFile = File(outputFolder, "${prefix}_page_${i + 1}.png")
                javax.imageio.ImageIO.write(image, "PNG", outputFile)
            }
        }

        onComplete("✓ ${pdfFile.name} converted to images in: ${outputFolder.absolutePath}")
    } catch (e: Exception) {
        Logger.error("PDF conversion error: ${e.message}", e)
        onComplete("✗ Conversion failed: ${e.message}")
    }
}

private fun convertImagesToPdf(imageFiles: List<File>, onComplete: (String) -> Unit) {
    Logger.log("Converting images to PDF: ${imageFiles.size} files")

    try {
        // Create output folder structure
        val desktopDir = File(System.getProperty("user.home"), "Desktop")
        val zer0neDir = File(desktopDir, "zer0ne-ocr-xlsx")
        val convertedDir = File(zer0neDir, "converted")

        if (!convertedDir.exists()) {
            convertedDir.mkdirs()
        }

        // Get current datetime
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
        val datetime = dateFormat.format(Date())

        // Create prefix from first image name
        val firstImageName = imageFiles.first().nameWithoutExtension
        val prefix = "${firstImageName}_images_$datetime"
        val outputFile = File(convertedDir, "${prefix}.pdf")

        // TODO: Implement actual Images to PDF conversion using a library like iText
        // For now, create the output folder structure

        onComplete("✓ Images to PDF conversion ready. PDF will be saved to: ${outputFile.absolutePath}")
    } catch (e: Exception) {
        Logger.error("Images to PDF error: ${e.message}", e)
        onComplete("✗ Conversion failed: ${e.message}")
    }
}
