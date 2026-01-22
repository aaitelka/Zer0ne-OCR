package ma.zer0ne.ocr.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import ma.zer0ne.ocr.model.InvoiceFile
import ma.zer0ne.ocr.ui.components.TabItem
import ma.zer0ne.ocr.ui.components.TabNavigation
import ma.zer0ne.ocr.ui.theme.AppTheme
import java.io.File

/**
 * Main content area with tabs and screen switching
 */
@Composable
fun MainContent(
    selectedTab: TabItem,
    onTabSelected: (TabItem) -> Unit,
    window: ComposeWindow,
    files: List<InvoiceFile>,
    processing: Boolean,
    onFilesSelected: (List<File>) -> Unit,
    onRemoveFile: (String) -> Unit,
    onProcess: () -> Unit,
    onStop: () -> Unit,
    pdfToolFiles: List<File>,
    isPdfToolConverting: Boolean,
    pdfToolMessage: String?,
    onPdfFilesDropped: (List<File>) -> Unit,
    onPdfConvertingChange: (Boolean) -> Unit,
    onPdfMessageChange: (String?) -> Unit
) {
    // Tab navigation
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = AppTheme.colors.bgMedium.copy(alpha = 0.5f)
    ) {
        TabNavigation(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected
        )
    }

    // Tab content
    when (selectedTab) {
        TabItem.CONVERT -> {
            ConvertScreen(
                files = files,
                processing = processing,
                onFilesSelected = onFilesSelected,
                onRemoveFile = onRemoveFile,
                onProcess = onProcess,
                onStop = onStop,
                window = window
            )
        }

        TabItem.PDF_CONVERTER -> {
            PdfConverterScreen(
                window = window,
                droppedFiles = pdfToolFiles,
                isConverting = isPdfToolConverting,
                conversionMessage = pdfToolMessage,
                onFilesDropped = onPdfFilesDropped,
                onConvertingChange = onPdfConvertingChange,
                onMessageChange = onPdfMessageChange
            )
        }

        TabItem.HISTORY -> {
            HistoryScreen()
        }
    }
}
