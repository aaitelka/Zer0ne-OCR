package ma.zer0ne.ocr.processors

import kotlinx.coroutines.delay
import ma.zer0ne.ocr.model.FileStatus
import ma.zer0ne.ocr.converters.ExcelExporter
import ma.zer0ne.ocr.model.InvoiceFile
import ma.zer0ne.ocr.model.SnackbarMessage
import ma.zer0ne.ocr.model.groq.InvoiceData
import ma.zer0ne.ocr.utils.Logger
import java.io.File

/**
 * Handles the batch processing of invoice files
 */
class InvoiceBatchProcessor(
    private val processor: InvoiceProcessor,
    private val onUpdate: (List<InvoiceFile>) -> Unit,
    private val onError: (SnackbarMessage) -> Unit
) {
    private val completedInvoices = mutableListOf<InvoiceData>()
    private val exporter = ExcelExporter()

    /**
     * Process all invoice files
     * First splits PDFs into images, then processes each image
     */
    suspend fun processAll(files: List<InvoiceFile>): List<InvoiceData> {
        var mutableFiles = files.toMutableList()

        // First pass: Split PDFs into images
        val filesToProcess = splitPdfsToImages(mutableFiles) { updatedFiles ->
            mutableFiles = updatedFiles.toMutableList()
            onUpdate(mutableFiles.toList())
        }

        // Second pass: Process each image file
        processImageFiles(filesToProcess, mutableFiles)

        return completedInvoices
    }

    /**
     * Split all PDF files into images and return list of files to process
     */
    private suspend fun splitPdfsToImages(
        files: MutableList<InvoiceFile>,
        onFilesChanged: (List<InvoiceFile>) -> Unit
    ): List<InvoiceFile> {
        val filesToProcess = mutableListOf<InvoiceFile>()

        for (file in files.toList()) {
            val invoiceFile = File(file.path)

            if (invoiceFile.extension.equals("pdf", ignoreCase = true)) {
                // Update status to show we're splitting PDF
                val pdfIndex = files.indexOfFirst { it.id == file.id }
                if (pdfIndex >= 0) {
                    files[pdfIndex] = file.copy(status = FileStatus.Processing)
                    onFilesChanged(files.toList())

                    // Small delay to allow UI to update before starting PDF split
                    delay(100)
                }

                try {
                    // Split PDF to images
                    val imageFiles = processor.splitPdfToImages(invoiceFile)

                    if (imageFiles.isNotEmpty()) {
                        // Create InvoiceFile entries for each page
                        val pageFiles = imageFiles.mapIndexed { index, imageFile ->
                            InvoiceFile(
                                id = "${file.id}_page_${index + 1}",
                                name = "${invoiceFile.nameWithoutExtension}_page_${index + 1}.png",
                                path = imageFile.absolutePath,
                                status = FileStatus.Pending
                            )
                        }

                        // Remove the PDF and add the page images
                        files.removeAll { it.id == file.id }
                        files.addAll(pageFiles)
                        onFilesChanged(files)

                        // Add page files to process list
                        filesToProcess.addAll(pageFiles)

                        // Small delay for visual effect
                        delay(300)
                    }
                } catch (e: Exception) {
                    Logger.error("Failed to split PDF: ${e.message}")
                    val idx = files.indexOfFirst { it.id == file.id }
                    if (idx >= 0) {
                        files[idx] = file.copy(status = FileStatus.Error, error = e.message)
                        onFilesChanged(files)
                    }
                }
            } else {
                // Regular image file
                filesToProcess.add(file)
            }
        }

        return filesToProcess
    }

    /**
     * Process each image file through OCR
     */
    private suspend fun processImageFiles(
        filesToProcess: List<InvoiceFile>,
        mutableFiles: MutableList<InvoiceFile>
    ) {
        for (file in filesToProcess) {
            val fileIndex = mutableFiles.indexOfFirst { it.id == file.id }
            if (fileIndex < 0) continue

            // Update status to processing
            mutableFiles[fileIndex] = mutableFiles[fileIndex].copy(status = FileStatus.Processing)
            onUpdate(mutableFiles.toList())

            try {
                val invoiceFile = File(file.path)
                val invoiceData = processor.processInvoice(invoiceFile)
                completedInvoices.add(invoiceData)
                mutableFiles[fileIndex] = mutableFiles[fileIndex].copy(
                    status = FileStatus.Completed,
                    data = invoiceData
                )
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error"
                mutableFiles[fileIndex] = mutableFiles[fileIndex].copy(
                    status = FileStatus.Error,
                    error = errorMsg
                )
            }

            onUpdate(mutableFiles.toList())

            // Add delay between files to respect rate limits
            delay(1500)
        }
    }

    /**
     * Process only image files (no PDF conversion)
     * Used when PDFs have already been converted to images
     */
    suspend fun processImagesOnly(files: List<InvoiceFile>): List<InvoiceData> {
        val mutableFiles = files.toMutableList()
        processImageFiles(files, mutableFiles)
        return completedInvoices
    }

    /**
     * Export completed invoices to Excel
     */
    fun exportToExcel(): File? {
        if (completedInvoices.isEmpty()) return null

        return try {
            val excelFile = exporter.exportToExcel(completedInvoices)
            Logger.log("Excel file automatically created: ${excelFile.absolutePath}")
            onError(
                SnackbarMessage(
                    "✅ All ${completedInvoices.size} invoices exported!",
                    folderPath = excelFile.parentFile?.absolutePath ?: ""
                )
            )
            excelFile
        } catch (e: Exception) {
            Logger.error("Failed to auto-export Excel: ${e.message}", e)
            onError(SnackbarMessage("Failed to export Excel file: ${e.message}"))
            null
        }
    }
}

/**
 * Convert PDFs to images only (first step)
 * Returns the updated file list with images instead of PDFs
 */
suspend fun convertPdfsToImages(
    files: List<InvoiceFile>,
    onUpdate: (List<InvoiceFile>) -> Unit,
    onError: (SnackbarMessage) -> Unit = {},
    onComplete: () -> Unit
) {
    val apiKeysFile = File("api_keys.txt")
    val processor = if (apiKeysFile.exists()) {
        InvoiceProcessor(apiKeysFile.absolutePath, useKeyRotation = true)
    } else {
        InvoiceProcessor("")
    }

    var mutableFiles = files.toMutableList()

    try {
        for (file in files.toList()) {
            val invoiceFile = File(file.path)

            if (invoiceFile.extension.equals("pdf", ignoreCase = true)) {
                // Update status to show we're splitting PDF
                val pdfIndex = mutableFiles.indexOfFirst { it.id == file.id }
                if (pdfIndex >= 0) {
                    mutableFiles[pdfIndex] = file.copy(status = FileStatus.Processing)
                    onUpdate(mutableFiles.toList())
                    delay(100)
                }

                try {
                    // Split PDF to images
                    val imageFiles = processor.splitPdfToImages(invoiceFile)

                    if (imageFiles.isNotEmpty()) {
                        // Create InvoiceFile entries for each page
                        val pageFiles = imageFiles.mapIndexed { index, imageFile ->
                            InvoiceFile(
                                id = "${file.id}_page_${index + 1}",
                                name = "${invoiceFile.nameWithoutExtension}_page_${index + 1}.png",
                                path = imageFile.absolutePath,
                                status = FileStatus.Pending
                            )
                        }

                        // Remove the PDF and add the page images
                        mutableFiles = mutableFiles.filter { it.id != file.id }.toMutableList()
                        mutableFiles.addAll(pageFiles)
                        onUpdate(mutableFiles.toList())

                        onError(SnackbarMessage("✓ ${file.name}: Split into ${imageFiles.size} image(s)"))
                        delay(300)
                    }
                } catch (e: Exception) {
                    Logger.error("Failed to split PDF: ${e.message}")
                    val idx = mutableFiles.indexOfFirst { it.id == file.id }
                    if (idx >= 0) {
                        mutableFiles[idx] = file.copy(status = FileStatus.Error, error = e.message)
                        onUpdate(mutableFiles.toList())
                    }
                }
            }
        }
    } finally {
        // Don't cleanup processor here - we need to keep the temp image files
        // for the user to review and process in the next step
        onComplete()
    }
}

/**
 * Process images to Excel (second step) - no PDF conversion
 */
suspend fun processImagesToExcel(
    files: List<InvoiceFile>,
    apiKey: String,
    onUpdate: (List<InvoiceFile>) -> Unit,
    onError: (SnackbarMessage) -> Unit = {},
    onComplete: () -> Unit
) {
    val apiKeysFile = File("api_keys.txt")
    val processor = if (apiKeysFile.exists()) {
        Logger.log("Using API keys file with rotation: ${apiKeysFile.absolutePath}")
        InvoiceProcessor(apiKeysFile.absolutePath, useKeyRotation = true)
    } else {
        Logger.log("API keys file not found, using single API key")
        InvoiceProcessor(apiKey)
    }

    try {
        val batchProcessor = InvoiceBatchProcessor(processor, onUpdate, onError)
        // Only process image files (skip PDF splitting since they're already images)
        batchProcessor.processImagesOnly(files)
        batchProcessor.exportToExcel()
    } finally {
        processor.cleanup()
        onComplete()
    }
}

/**
 * Main function to process all invoices (converts PDFs and processes images in one go)
 */
suspend fun processAllInvoices(
    files: List<InvoiceFile>,
    apiKey: String,
    onUpdate: (List<InvoiceFile>) -> Unit,
    onError: (SnackbarMessage) -> Unit = {},
    onComplete: () -> Unit
) {
    // Use API keys file with rotation for better rate limit handling
    val apiKeysFile = File("api_keys.txt")
    val processor = if (apiKeysFile.exists()) {
        Logger.log("Using API keys file with rotation: ${apiKeysFile.absolutePath}")
        InvoiceProcessor(apiKeysFile.absolutePath, useKeyRotation = true)
    } else {
        Logger.log("API keys file not found, using single API key")
        InvoiceProcessor(apiKey)
    }

    try {
        val batchProcessor = InvoiceBatchProcessor(processor, onUpdate, onError)
        batchProcessor.processAll(files)
        batchProcessor.exportToExcel()
    } finally {
        processor.cleanup()
        onComplete()
    }
}
