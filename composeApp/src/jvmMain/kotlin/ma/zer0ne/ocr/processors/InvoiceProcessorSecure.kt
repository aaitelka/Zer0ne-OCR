package ma.zer0ne.ocr.processors

import kotlinx.coroutines.*
import ma.zer0ne.ocr.config.SecureApiKeyManager
import ma.zer0ne.ocr.converters.PdfToImageConverter
import ma.zer0ne.ocr.model.groq.InvoiceData
import ma.zer0ne.ocr.services.GroqApiService
import ma.zer0ne.ocr.utils.Logger
import java.io.File

/**
 * InvoiceProcessor that uses SecureApiKeyManager for encrypted API key storage.
 * Features:
 * - Uses AES-256 encrypted API keys
 * - Smart key rotation with random selection
 * - Automatic rate limit handling and key switching
 * - Retries with different keys on failure
 */
class InvoiceProcessorSecure(
    private val keyManager: SecureApiKeyManager
) : IInvoiceProcessor {
    private val pdfConverter = PdfToImageConverter()

    // Track the current API key being used
    private var currentApiKey: String? = null

    // Maximum retries with different keys
    private val MAX_RETRIES = 3

    // Temporary directory for PDF conversions
    private val tempDir = File(System.getProperty("java.io.tmpdir"), "invoice_ocr_temp").apply {
        mkdirs()
    }

    /**
     * Split a PDF file into individual image files
     */
    override suspend fun splitPdfToImages(pdfFile: File): List<File> = withContext(Dispatchers.IO) {
        Logger.log("Splitting PDF to images: ${pdfFile.name}")

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
        val apiKey = keyManager.getNextApiKey()
        if (apiKey == null) {
            throw IllegalStateException("No API keys available")
        }
        currentApiKey = apiKey
        return GroqApiService(apiKey)
    }

    /**
     * Process a single invoice file (implements interface)
     */
    override suspend fun processInvoice(
        file: File,
        onProgress: suspend (String) -> Unit
    ): InvoiceData = withContext(Dispatchers.IO) {
        onProgress("Processing ${file.name}...")

        val result = processInvoiceImage(file)
        result.getOrElse { e ->
            onProgress("Error: ${e.message}")
            throw e
        }
    }

    /**
     * Process a single invoice image with automatic retry and key rotation
     */
    private suspend fun processInvoiceImage(imageFile: File): Result<InvoiceData> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        for (attempt in 1..MAX_RETRIES) {
            try {
                val service = getGroqService()
                val invoiceData = service.processInvoiceImage(imageFile)

                Logger.log("Successfully processed: ${imageFile.name}")
                return@withContext Result.success(invoiceData)

            } catch (e: Exception) {
                lastException = e
                val errorMessage = e.message ?: "Unknown error"

                // Check if it's a rate limit error (429)
                if (errorMessage.contains("429") || errorMessage.contains("rate limit", ignoreCase = true)) {
                    Logger.log("[WARNING] Rate limit hit, switching API key (attempt $attempt/$MAX_RETRIES)")
                    currentApiKey?.let { keyManager.markKeyAsRateLimited(it) }

                    if (attempt < MAX_RETRIES && keyManager.getAvailableKeysCount() > 0) {
                        delay(500) // Brief delay before retry
                        continue
                    }
                }

                // Check for other API errors
                if (errorMessage.contains("401") || errorMessage.contains("403")) {
                    Logger.log("[ERROR] API key invalid or unauthorized")
                    currentApiKey?.let { keyManager.markKeyAsFailed(it) }

                    if (attempt < MAX_RETRIES && keyManager.getAvailableKeysCount() > 0) {
                        continue
                    }
                }

                Logger.error("Error processing ${imageFile.name} (attempt $attempt): $errorMessage", e)

                if (attempt < MAX_RETRIES) {
                    delay(1000L * attempt) // Exponential backoff
                }
            }
        }

        Result.failure(lastException ?: Exception("Failed to process invoice after $MAX_RETRIES attempts"))
    }

    /**
     * Process multiple invoice files
     */
    suspend fun processInvoices(
        imageFiles: List<File>,
        onProgress: (Int, Int, InvoiceData?) -> Unit
    ): List<InvoiceData> = withContext(Dispatchers.IO) {
        val results = mutableListOf<InvoiceData>()

        imageFiles.forEachIndexed { index, file ->
            val result = processInvoiceImage(file)
            result.onSuccess { invoiceData ->
                results.add(invoiceData)
                onProgress(index + 1, imageFiles.size, invoiceData)
            }.onFailure {
                onProgress(index + 1, imageFiles.size, null)
            }

            // Brief delay between requests to avoid rate limits
            if (index < imageFiles.size - 1) {
                delay(200)
            }
        }

        results
    }

    /**
     * Check if API keys are available
     */
    fun hasApiKeys(): Boolean = keyManager.hasKeys()

    /**
     * Get the number of available API keys
     */
    fun getAvailableKeysCount(): Int = keyManager.getAvailableKeysCount()

    /**
     * Cleanup temporary files
     */
    override fun cleanup() {
        try {
            tempDir.deleteRecursively()
            pdfConverter.shutdown()
            Logger.log("Cleaned up temporary files")
        } catch (e: Exception) {
            Logger.error("Error during cleanup: ${e.message}", e)
        }
    }
}
