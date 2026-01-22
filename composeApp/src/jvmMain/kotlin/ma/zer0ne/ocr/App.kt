package ma.zer0ne.ocr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ma.zer0ne.ocr.config.SecureApiKeyManager
import ma.zer0ne.ocr.model.InvoiceFile
import ma.zer0ne.ocr.processors.convertPdfsToImages
import ma.zer0ne.ocr.processors.processImagesToExcel
import ma.zer0ne.ocr.ui.components.AppHeader
import ma.zer0ne.ocr.ui.components.AppSnackbarHost
import ma.zer0ne.ocr.ui.components.TabItem
import ma.zer0ne.ocr.ui.components.background.AnimatedBackgroundBubbles
import ma.zer0ne.ocr.ui.components.titlebar.CustomTitleBar
import ma.zer0ne.ocr.ui.screens.MainContent
import ma.zer0ne.ocr.ui.screens.SettingsScreen
import ma.zer0ne.ocr.ui.theme.AppTheme
import ma.zer0ne.ocr.ui.theme.AppThemeManager
import ma.zer0ne.ocr.ui.theme.AppThemeProvider
import ma.zer0ne.ocr.utils.Logger
import ma.zer0ne.ocr.utils.filesToInvoiceFiles
import ma.zer0ne.ocr.utils.openFolderInExplorer
import ma.zer0ne.ocr.utils.setupDragAndDrop
import java.io.File

@Composable
fun App(window: ComposeWindow) {
    // Convert screen state
    var files by remember { mutableStateOf(listOf<InvoiceFile>()) }
    var processing by remember { mutableStateOf(false) }
    var processingJob by remember { mutableStateOf<Job?>(null) }

    // PDF Converter screen state
    var pdfToolFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isPdfToolConverting by remember { mutableStateOf(false) }
    var pdfToolMessage by remember { mutableStateOf<String?>(null) }

    // App state
    var selectedTab by remember { mutableStateOf(TabItem.CONVERT) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()

    // Initialize on startup
    LaunchedEffect(Unit) {
        setupDragAndDrop(window) { droppedFiles ->
            files = files + filesToInvoiceFiles(droppedFiles)
        }
    }

    // Theme setup
    val colorScheme = AppThemeManager.createColorScheme(isDarkTheme)

    AppThemeProvider(isDarkTheme = isDarkTheme, onThemeChange = { isDarkTheme = it }) {
        MaterialTheme(colorScheme = colorScheme) {
            AppScaffold(
                snackbarHostState = snackbarHostState,
                window = window,
                showSettingsScreen = showSettingsScreen,
                onSettingsClick = { showSettingsScreen = true },
                onSettingsBack = { showSettingsScreen = false },
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
                        handleProcessing(
                            files = files,
                            onFilesUpdate = { files = it },
                            onProcessingComplete = { processing = false; processingJob = null },
                            snackbarHostState = snackbarHostState,
                            scope = scope,
                            onSettingsClick = { showSettingsScreen = true }
                        )
                    }
                },
                onStop = {
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
 * Main app scaffold with all UI components
 */
@Composable
private fun AppScaffold(
    snackbarHostState: SnackbarHostState,
    window: ComposeWindow,
    showSettingsScreen: Boolean,
    onSettingsClick: () -> Unit,
    onSettingsBack: () -> Unit,
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
 * Handle the processing of files (PDFs or images)
 */
private suspend fun handleProcessing(
    files: List<InvoiceFile>,
    onFilesUpdate: (List<InvoiceFile>) -> Unit,
    onProcessingComplete: () -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onSettingsClick: () -> Unit
) {
    // Check if there are PDFs to convert first
    val hasPdfs = files.any { it.path.lowercase().endsWith(".pdf") }

    if (hasPdfs) {
        // Step 1: Convert PDFs to images only
        convertPdfsToImages(
            files = files,
            onUpdate = { updatedFiles -> onFilesUpdate(updatedFiles) },
            onError = { message ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message.text,
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onComplete = {
                onProcessingComplete()
                // Don't clear files - let user review and remove unwanted images
            }
        )
    } else {
        // Step 2: Process images to Excel
        processImagesToExcel(
            files = files,
            apiKey = SecureApiKeyManager.getInstance().getNextApiKey() ?: "",
            onUpdate = { updatedFiles -> onFilesUpdate(updatedFiles) },
            onError = { message ->
                Logger.error("-----------------------------------\n\n" + message.text)
                scope.launch {
                    when {
                        message.text.contains("Groq API error (401)") && message.text.contains("invalid_api_key", ignoreCase = true) -> {
                            val result = snackbarHostState.showSnackbar(
                                message = "No valid API key. Please add one in Settings.",
                                actionLabel = "Add API Key",
                                duration = SnackbarDuration.Indefinite
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                onSettingsClick()
                            }
                        }
                        message.folderPath != null -> {
                            val result = snackbarHostState.showSnackbar(
                                message = message.text,
                                actionLabel = "Open",
                                duration = SnackbarDuration.Indefinite
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                openFolderInExplorer(message.folderPath)
                            }
                        }
                        else -> {
                            snackbarHostState.showSnackbar(
                                message = message.text,
                                duration = SnackbarDuration.Long
                            )
                        }
                    }
                }
            },
            onComplete = {
                onProcessingComplete()
                scope.launch {
                    delay(2000)
                    val currentFiles = files.toList()
                    for (i in currentFiles.indices) {
                        onFilesUpdate(currentFiles.drop(i + 1))
                        delay(300)
                    }
                    onFilesUpdate(emptyList())
                }
            }
        )
    }
}
