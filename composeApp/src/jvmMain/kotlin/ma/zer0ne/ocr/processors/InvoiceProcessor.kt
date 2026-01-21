package ma.zer0ne.ocr.processors

// InvoiceProcessor.kt
import kotlinx.coroutines.*
import ma.zer0ne.ocr.config.ApiKeyManager
import ma.zer0ne.ocr.converters.PdfToImageConverter
import ma.zer0ne.ocr.model.groq.InvoiceData
import ma.zer0ne.ocr.services.GroqApiService
import ma.zer0ne.ocr.utils.Logger
import java.io.File

/**
 * InvoiceProcessor with smart API key rotation and rate limit handling
 * - Automatically rotates through multiple API keys
 * - Handles rate limits (429) by switching to another key
 * - Retries with different keys on failure
 */
class InvoiceProcessor {
    private var singleKeyService: GroqApiService? = null
    private var apiKeyManager: ApiKeyManager? = null
    private val pdfConverter = PdfToImageConverter()

    // Track the current API key being used (for marking as rate-limited)
    private var currentApiKey: String? = null

    // Maximum retries with different keys
    private val MAX_RETRIES = 3

    /**
     * Initialize with API keys file (RECOMMENDED)
     * Enables key rotation and rate limit handling
     */
    constructor(apiKeysFilePath: String, useKeyRotation: Boolean) {
        if (useKeyRotation) {
            apiKeyManager = ApiKeyManager(apiKeysFilePath)
            if (apiKeyManager!!.hasKeys()) {
                Logger.log("InvoiceProcessor initialized with ${apiKeyManager!!.getTotalKeysCount()} API keys (rotation enabled)")
            } else {
                Logger.error("No API keys found in $apiKeysFilePath")
            }
        }
    }

    /**
     * Initialize with single API key (backward compatible)
     */
    constructor(groqApiKey: String) {
        singleKeyService = GroqApiService(groqApiKey)
        currentApiKey = groqApiKey
        Logger.log("InvoiceProcessor initialized with single API key")
    }

    // Temporary directory for PDF conversions
    private val tempDir = File(System.getProperty("java.io.tmpdir"), "invoice_ocr_temp").apply {
        mkdirs()
    }

    /**
     * Split a PDF file into individual image files (one per page)
     * Returns list of image files without processing them
     */
    suspend fun splitPdfToImages(pdfFile: File): List<File> = withContext(Dispatchers.IO) {
        Logger.log("Splitting PDF to images: ${pdfFile.name}")

        // Create a unique temp directory for this PDF
        val pdfTempDir = File(tempDir, "pdf_${System.currentTimeMillis()}")
        pdfTempDir.mkdirs()

        val imageFiles = pdfConverter.convertPdfToImages(pdfFile, pdfTempDir)
        Logger.log("PDF split into ${imageFiles.size} page(s)")

        imageFiles
    }

    /**
     * Get a GroqApiService with a fresh API key
     */
    private fun getGroqService(): GroqApiService {
        return if (apiKeyManager != null) {
            val apiKey = apiKeyManager!!.getNextApiKey()
            if (apiKey.isNullOrBlank()) {
                throw Exception("No available API keys. All keys may be rate-limited.")
            }
            currentApiKey = apiKey
            GroqApiService(apiKey)
        } else {
            singleKeyService ?: throw Exception("No API key available")
        }
    }

    /**
     * Process invoice with automatic retry and rate limit handling
     * Retries up to MAX_RETRIES times with different API keys
     * If all retries fail, throws exception to skip to next file
     */
    private suspend fun processInvoiceWithRetry(imageFile: File): InvoiceData {
        var lastException: Exception? = null
        var retryCount = 0

        while (retryCount < MAX_RETRIES) {
            try {
                val service = getGroqService()
                val result = service.processInvoiceImage(imageFile)

                // Add small delay after successful request to avoid hitting rate limit
                delay(1000) // 1 second between requests

                return result
            } catch (e: Exception) {
                lastException = e
                retryCount++
                val errorMessage = e.message ?: ""

                Logger.log("[WARNING] Attempt $retryCount/$MAX_RETRIES failed: ${errorMessage.take(80)}...")

                // Check if it's a rate limit error (429)
                if (errorMessage.contains("429") || errorMessage.contains("rate_limit") || errorMessage.contains("Rate limit")) {
                    // Extract wait time from error message if available
                    val waitTimeMs = extractWaitTime(errorMessage)
                    Logger.log("[WARNING] Rate limit hit, waiting ${waitTimeMs}ms before retry...")
                    delay(waitTimeMs + 500)

                    // Mark current key as rate-limited
                    if (apiKeyManager != null && currentApiKey != null) {
                        apiKeyManager!!.markKeyAsRateLimited(currentApiKey!!)
                    }
                } else {
                    // For other errors, small delay before retry
                    delay(1000)
                }

                // If we still have retries left, continue
                if (retryCount < MAX_RETRIES) {
                    Logger.log("[INFO] Retrying with different API key...")
                    continue
                }
            }
        }

        // All retries exhausted - throw to skip this file
        Logger.error("Failed after $MAX_RETRIES attempts, skipping file: ${imageFile.name}")
        throw lastException ?: Exception("Failed after $MAX_RETRIES attempts")
    }

    /**
     * Extract wait time from rate limit error message
     * Example: "Please try again in 640ms" -> 640
     */
    private fun extractWaitTime(errorMessage: String): Long {
        // Try to find patterns like "in 640ms", "in 1.5s", etc.
        val msPattern = Regex("""in\s+(\d+)\s*ms""", RegexOption.IGNORE_CASE)
        val secPattern = Regex("""in\s+([\d.]+)\s*s(?:ec)?""", RegexOption.IGNORE_CASE)

        msPattern.find(errorMessage)?.let {
            return it.groupValues[1].toLongOrNull() ?: 2000L
        }

        secPattern.find(errorMessage)?.let {
            val seconds = it.groupValues[1].toDoubleOrNull() ?: 2.0
            return (seconds * 1000).toLong()
        }

        // Default wait time if we can't parse the message
        return 2000L
    }

    /**
     * Process a single invoice file (PDF or image)
     * @param file The invoice file to process
     * @param onProgress Callback for progress updates
     * @return InvoiceData extracted from the file
     */
    suspend fun processInvoice(
        file: File,
        onProgress: suspend (String) -> Unit = {}
    ): InvoiceData = withContext(Dispatchers.IO) {
        try {
            onProgress("Processing ${file.name}...")

            when (file.extension.lowercase()) {
                "pdf" -> processPdfInvoice(file, onProgress)
                "jpg", "jpeg", "png" -> processImageInvoice(file, onProgress)
                else -> throw IllegalArgumentException("Unsupported file type: ${file.extension}")
            }
        } catch (e: Exception) {
            onProgress("Error: ${e.message}")
            throw e
        }
    }

    /**
     * Process multiple invoice files in parallel
     * @param files List of invoice files to process
     * @param onFileProgress Callback for individual file progress
     * @param onFileComplete Callback when a file is completed
     * @return List of extracted InvoiceData
     */
    suspend fun processInvoices(
        files: List<File>,
        onFileProgress: suspend (File, String) -> Unit = { _, _ -> },
        onFileComplete: suspend (File, Result<InvoiceData>) -> Unit = { _, _ -> }
    ): List<Result<InvoiceData>> = coroutineScope {
        files.map { file ->
            async {
                try {
                    val result = processInvoice(file) { progress ->
                        onFileProgress(file, progress)
                    }
                    onFileComplete(file, Result.success(result))
                    Result.success(result)
                } catch (e: Exception) {
                    onFileComplete(file, Result.failure(e))
                    Result.failure(e)
                }
            }
        }.awaitAll()
    }

    /**
     * Process a PDF invoice (converts to images first, then processes all pages)
     * Returns a list of invoices extracted from all pages
     */
    suspend fun processPdfInvoiceMultiPage(
        pdfFile: File,
        onProgress: suspend (String) -> Unit
    ): List<InvoiceData> = withContext(Dispatchers.IO) {
        onProgress("Converting PDF to images...")

        // Create a unique temp directory for this PDF
        val pdfTempDir = File(tempDir, "pdf_${System.currentTimeMillis()}")
        pdfTempDir.mkdirs()

        try {
            // Convert PDF to images
            val imageFiles = pdfConverter.convertPdfToImages(pdfFile, pdfTempDir)

            if (imageFiles.isEmpty()) {
                throw Exception("No pages found in PDF")
            }

            val invoices = mutableListOf<InvoiceData>()

            // Process all pages
            for ((index, imageFile) in imageFiles.withIndex()) {
                onProgress("Processing page ${index + 1} of ${imageFiles.size}...")
                try {
                    val invoiceData = processInvoiceWithRetry(imageFile)
                    invoices.add(invoiceData)
                } catch (e: Exception) {
                    onProgress("Warning: Failed to process page ${index + 1}: ${e.message}")
                    Logger.log("[WARNING] Failed to process page ${index + 1}: ${e.message}")
                    // Continue with next page even if one fails
                }
            }

            if (invoices.isEmpty()) {
                throw Exception("No invoices could be extracted from PDF pages")
            }

            // Clean up temporary files
            pdfTempDir.deleteRecursively()

            invoices
        } catch (e: Exception) {
            // Clean up on error
            pdfTempDir.deleteRecursively()
            throw e
        }
    }

    /**
     * Process a PDF invoice (converts to images first, then processes first page)
     * @deprecated Use processPdfInvoiceMultiPage() for multi-page PDFs
     */
    private suspend fun processPdfInvoice(
        pdfFile: File,
        onProgress: suspend (String) -> Unit
    ): InvoiceData = withContext(Dispatchers.IO) {
        onProgress("Converting PDF to images...")

        // Create a unique temp directory for this PDF
        val pdfTempDir = File(tempDir, "pdf_${System.currentTimeMillis()}")
        pdfTempDir.mkdirs()

        try {
            // Convert PDF to images
            val imageFiles = pdfConverter.convertPdfToImages(pdfFile, pdfTempDir)

            if (imageFiles.isEmpty()) {
                throw Exception("No pages found in PDF")
            }

            onProgress("Processing page 1 of ${imageFiles.size}...")

            // Process only the first page (usually contains main invoice data)
            val invoiceData = processInvoiceWithRetry(imageFiles.first())

            // Clean up temporary files
            pdfTempDir.deleteRecursively()

            invoiceData
        } catch (e: Exception) {
            // Clean up on error
            pdfTempDir.deleteRecursively()
            throw e
        }
    }

    /**
     * Process an image invoice directly
     */
    private suspend fun processImageInvoice(
        imageFile: File,
        onProgress: suspend (String) -> Unit
    ): InvoiceData = withContext(Dispatchers.IO) {
        onProgress("Extracting invoice data...")
        processInvoiceWithRetry(imageFile)
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        singleKeyService?.close()
        tempDir.deleteRecursively()
    }
}