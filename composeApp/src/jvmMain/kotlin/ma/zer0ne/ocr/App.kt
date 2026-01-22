package ma.zer0ne.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ma.zer0ne.ocr.model.InvoiceFile
import ma.zer0ne.ocr.processors.convertPdfsToImages
import ma.zer0ne.ocr.processors.processImagesToExcel
import ma.zer0ne.ocr.ui.components.SettingsDropdown
import ma.zer0ne.ocr.ui.components.TabItem
import ma.zer0ne.ocr.ui.components.TabNavigation
import ma.zer0ne.ocr.ui.components.background.AnimatedBackgroundBubbles
import ma.zer0ne.ocr.ui.components.titlebar.CustomTitleBar
import ma.zer0ne.ocr.ui.screens.ConvertScreen
import ma.zer0ne.ocr.ui.screens.HistoryScreen
import ma.zer0ne.ocr.ui.screens.PdfConverterScreen
import ma.zer0ne.ocr.ui.screens.SettingsScreen
import ma.zer0ne.ocr.ui.theme.AIColors
import ma.zer0ne.ocr.ui.theme.AILightColors
import ma.zer0ne.ocr.ui.theme.AppColorScheme
import ma.zer0ne.ocr.ui.theme.AppTheme
import ma.zer0ne.ocr.ui.theme.AppThemeProvider
import ma.zer0ne.ocr.utils.filesToInvoiceFiles
import ma.zer0ne.ocr.utils.openFolderInExplorer
import ma.zer0ne.ocr.utils.setupDragAndDrop
import ocr.composeapp.generated.resources.Res
import ocr.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource
import java.io.File

@Composable
fun App(window: ComposeWindow) {
    // App state
    var files by remember { mutableStateOf(listOf<InvoiceFile>()) }
    var processing by remember { mutableStateOf(false) }
    var processingJob by remember { mutableStateOf<Job?>(null) }
    var selectedTab by remember { mutableStateOf(TabItem.CONVERT) }
    val snackbarHostState = remember { SnackbarHostState() }

    // PDF Converter Screen state
    var pdfToolFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isPdfToolConverting by remember { mutableStateOf(false) }
    var pdfToolMessage by remember { mutableStateOf<String?>(null) }

    // Settings state
    var showSettingsScreen by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    // Initialize on startup
    LaunchedEffect(Unit) {
        if (!SecureApiKeyManager.getInstance().hasKeys()) {
            showApiKeyDialog = true
        }
        setupDragAndDrop(window) { droppedFiles ->
            files = files + filesToInvoiceFiles(droppedFiles)
        }
    }

    // Theme setup
    val themeColors: AppColorScheme = if (isDarkTheme) AIColors else AILightColors
    val colorScheme = createColorScheme(isDarkTheme, themeColors)

    AppThemeProvider(isDarkTheme = isDarkTheme, onThemeChange = { isDarkTheme = it }) {
        MaterialTheme(colorScheme = colorScheme) {

            AppScaffold(
                snackbarHostState = snackbarHostState,
                window = window,
                showSettingsScreen = showSettingsScreen,
                onSettingsClick = { showSettingsScreen = true },
                onSettingsBack = { showSettingsScreen = false },
                isDarkTheme = isDarkTheme,
                onDarkThemeChange = { isDarkTheme = it },
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                // Convert Screen
                files = files,
                processing = processing,
                onFilesSelected = { selectedFiles ->
                    files = files + filesToInvoiceFiles(selectedFiles)
                },
                onRemoveFile = { id ->
                    if (!processing) {
                        files = files.filter { it.id != id }
                    }
                },
                onProcess = {
                    processing = true
                    processingJob = scope.launch {
                        // Check if there are PDFs to convert first
                        val hasPdfs = files.any { it.path.lowercase().endsWith(".pdf") }

                        if (hasPdfs) {
                            // Step 1: Convert PDFs to images only
                            convertPdfsToImages(
                                files = files,
                                onUpdate = { updatedFiles -> files = updatedFiles },
                                onError = { message ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = message.text,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                },
                                onComplete = {
                                    processing = false
                                    processingJob = null
                                    // Don't clear files - let user review and remove unwanted images
                                }
                            )
                        } else {
                            // Step 2: Process images to Excel
                            processImagesToExcel(
                                files = files,
                                apiKey = SecureApiKeyManager.getInstance().getNextApiKey() ?: "",
                                onUpdate = { updatedFiles -> files = updatedFiles },
                                onError = { message ->
                                    scope.launch {
                                        val result = if (message.folderPath != null) {
                                            snackbarHostState.showSnackbar(
                                                message = message.text,
                                                actionLabel = "Open",
                                                duration = SnackbarDuration.Indefinite
                                            )
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                message = message.text,
                                                duration = SnackbarDuration.Long
                                            )
                                        }
                                        if (result == SnackbarResult.ActionPerformed && message.folderPath != null) {
                                            openFolderInExplorer(message.folderPath)
                                        }
                                    }
                                },
                                onComplete = {
                                    processing = false
                                    processingJob = null
                                    scope.launch {
                                        delay(2000)
                                        val currentFiles = files.toList()
                                        for (i in currentFiles.indices) {
                                            files = currentFiles.drop(i + 1)
                                            delay(300)
                                        }
                                        files = emptyList()
                                    }
                                }
                            )
                        }
                    }
                },
                onStop = {
                    // Immediately cancel the processing job and clear files
                    processingJob?.cancel()
                    processingJob = null
                    processing = false
                    files = emptyList()
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Processing stopped.",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                // PDF Converter Screen
                pdfToolFiles = pdfToolFiles,
                isPdfToolConverting = isPdfToolConverting,
                pdfToolMessage = pdfToolMessage,
                onPdfFilesDropped = { pdfToolFiles = it },
                onPdfConvertingChange = { isPdfToolConverting = it },
                onPdfMessageChange = { pdfToolMessage = it }
            )
        }
    }
}

/**
 * Create color scheme based on theme
 */
private fun createColorScheme(isDarkTheme: Boolean, themeColors: AppColorScheme): ColorScheme {
    return if (isDarkTheme) {
        darkColorScheme(
            background = themeColors.bgDark,
            surface = themeColors.bgMedium,
            primary = themeColors.primary,
            onBackground = themeColors.text,
            onSurface = themeColors.text,
            error = themeColors.error
        )
    } else {
        lightColorScheme(
            background = themeColors.bgDark,
            surface = themeColors.bgMedium,
            primary = themeColors.primary,
            onBackground = themeColors.text,
            onSurface = themeColors.text,
            error = themeColors.error
        )
    }
}

/**
 * Main app scaffold with all UI components
 */
@Composable
private fun AppScaffold(
    snackbarHostState: SnackbarHostState,
    window: ComposeWindow,
    showSettingsScreen: Boolean,
    onSettingsClick: () -> Unit,
    onSettingsBack: () -> Unit,
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    selectedTab: TabItem,
    onTabSelected: (TabItem) -> Unit,
    // Convert Screen
    files: List<InvoiceFile>,
    processing: Boolean,
    onFilesSelected: (List<File>) -> Unit,
    onRemoveFile: (String) -> Unit,
    onProcess: () -> Unit,
    onStop: () -> Unit,
    // PDF Converter Screen
    pdfToolFiles: List<File>,
    isPdfToolConverting: Boolean,
    pdfToolMessage: String?,
    onPdfFilesDropped: (List<File>) -> Unit,
    onPdfConvertingChange: (Boolean) -> Unit,
    onPdfMessageChange: (String?) -> Unit
) {
    Scaffold(
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.bgDark)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Background layer
            AnimatedBackgroundBubbles()

            // Content layer
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Title Bar
                CustomTitleBar(
                    window = window,
                    onSettingsClick = onSettingsClick,
                    onExitClick = { window.dispose() }
                )

                // App Header with settings dropdown
                AppHeader(
                    onSettingsClick = onSettingsClick,
                    onExitClick = { window.dispose() }
                )

                // Content
                if (showSettingsScreen) {
                    SettingsScreen(
                        onDarkThemeChange = onDarkThemeChange,
                        onBack = onSettingsBack
                    )
                } else {
                    MainContent(
                        selectedTab = selectedTab,
                        onTabSelected = onTabSelected,
                        window = window,
                        files = files,
                        processing = processing,
                        onFilesSelected = onFilesSelected,
                        onRemoveFile = onRemoveFile,
                        onProcess = onProcess,
                        onStop = onStop,
                        pdfToolFiles = pdfToolFiles,
                        isPdfToolConverting = isPdfToolConverting,
                        pdfToolMessage = pdfToolMessage,
                        onPdfFilesDropped = onPdfFilesDropped,
                        onPdfConvertingChange = onPdfConvertingChange,
                        onPdfMessageChange = onPdfMessageChange
                    )
                }
            }
        }
    }
}

/**
 * App header with logo, title and settings dropdown
 */
@Composable
private fun AppHeader(
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
 * Main content area with tabs
 */
@Composable
private fun MainContent(
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

/**
 * Custom snackbar host with themed styling
 */
@Composable
private fun AppSnackbarHost(snackbarHostState: SnackbarHostState) {
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
