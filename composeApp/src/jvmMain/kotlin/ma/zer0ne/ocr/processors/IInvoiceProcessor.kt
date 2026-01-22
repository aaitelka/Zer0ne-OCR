package ma.zer0ne.ocr.processors

import ma.zer0ne.ocr.model.groq.InvoiceData
import java.io.File

/**
 * Common interface for invoice processors
 */
interface IInvoiceProcessor {
    suspend fun splitPdfToImages(pdfFile: File): List<File>
    suspend fun processInvoice(file: File, onProgress: suspend (String) -> Unit = {}): InvoiceData
    fun cleanup()
}
