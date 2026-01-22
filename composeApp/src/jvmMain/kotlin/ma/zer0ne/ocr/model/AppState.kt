package ma.zer0ne.ocr.model

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Stable
import kotlinx.coroutines.Job
import ma.zer0ne.ocr.ui.components.TabItem
import java.io.File

/**
 * Data class for Convert screen state
 */
@Stable
data class ConvertScreenState(
    val files: List<InvoiceFile> = emptyList(),
    val processing: Boolean = false,
    val processingJob: Job? = null
)

/**
 * Data class for PDF Converter screen state
 */
@Stable
data class PdfConverterScreenState(
    val files: List<File> = emptyList(),
    val isConverting: Boolean = false,
    val message: String? = null
)

/**
 * Data class for app-level state
 */
@Stable
data class AppScreenState(
    val selectedTab: TabItem = TabItem.CONVERT,
    val showSettingsScreen: Boolean = false,
    val isDarkTheme: Boolean = true,
    val snackbarHostState: SnackbarHostState = SnackbarHostState()
)
