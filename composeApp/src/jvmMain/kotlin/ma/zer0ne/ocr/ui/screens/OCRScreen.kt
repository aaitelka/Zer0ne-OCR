package ma.zer0ne.ocr.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import ma.zer0ne.ocr.model.InvoiceFile
import ma.zer0ne.ocr.ui.components.ActionButtonMode
import ma.zer0ne.ocr.ui.components.ActionButtons
import ma.zer0ne.ocr.ui.components.FileListSection
import ma.zer0ne.ocr.ui.components.DropZone

@Composable
fun ConvertScreen(
    files: List<InvoiceFile>,
    processing: Boolean,
    onFilesSelected: (List<java.io.File>) -> Unit,
    onRemoveFile: (String) -> Unit,
    onProcess: () -> Unit,
    onStop: () -> Unit = {},
    window: ComposeWindow
) {
    // Check if any files are PDFs that need conversion
    val hasPdfs = files.any { it.path.lowercase().endsWith(".pdf") }

    // Determine button mode based on file types
    val buttonMode = if (hasPdfs) ActionButtonMode.CONVERT_PDF else ActionButtonMode.PROCESS_IMAGES

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Show drop zone only if no files are selected
        if (files.isEmpty()) {
            DropZone(
                onFilesSelected = onFilesSelected,
                window = window
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (files.isNotEmpty()) {

            FileListSection(
                files = files,
                onRemove = onRemoveFile,
                isProcessing = processing,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            ActionButtons(
                processing = processing,
                hasFiles = files.isNotEmpty(),
                onProcess = onProcess,
                onStop = onStop,
                mode = buttonMode
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
