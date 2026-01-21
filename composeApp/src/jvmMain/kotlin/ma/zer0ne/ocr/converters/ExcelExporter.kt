package ma.zer0ne.ocr.converters

import ma.zer0ne.ocr.model.groq.InvoiceData
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ExcelExporter {

    /**
     * Export invoice data to Excel file using Apache POI
     */
    fun exportToExcel(
        invoices: List<InvoiceData>,
        outputFile: File? = null
    ): File {
        if (invoices.isEmpty()) {
            throw IllegalArgumentException("No invoice data to export")
        }

        val excelFile = outputFile ?: generateDefaultFileName()
        excelFile.parentFile?.mkdirs()

        // Create workbook and sheet
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Invoices")

        // Create header row
        val headerRow = sheet.createRow(0)
        val headers = listOf("Invoice Number", "Date", "Vendor", "Amount", "Total HT")
        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).setCellValue(header)
        }

        // Add data rows
        invoices.forEachIndexed { rowIndex, invoice ->
            val row = sheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(invoice.invoiceNumber)
            row.createCell(1).setCellValue(invoice.date)
            row.createCell(2).setCellValue(invoice.vendor)
            row.createCell(3).setCellValue(invoice.amount)
            row.createCell(4).setCellValue(invoice.totalHT)
        }

        // Auto-size columns
        for (i in 0..4) {
            sheet.autoSizeColumn(i)
        }

        // Write to file
        workbook.use { wb ->
            excelFile.outputStream().use { output ->
                wb.write(output)
            }
        }

        println("Exported ${invoices.size} invoices to: ${excelFile.absolutePath}")
        return excelFile
    }

    /**
     * Export with summary statistics
     */
    fun exportWithSummary(
        invoices: List<InvoiceData>,
        outputFile: File? = null
    ): File {
        if (invoices.isEmpty()) {
            throw IllegalArgumentException("No invoice data to export")
        }

        val excelFile = outputFile ?: generateDefaultFileName()
        excelFile.parentFile?.mkdirs()

        val workbook = XSSFWorkbook()
        
        // Details sheet
        val detailsSheet = workbook.createSheet("Details")
        val headerRow = detailsSheet.createRow(0)
        val headers = listOf("Invoice Number", "Date", "Vendor", "Amount", "Total HT")
        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).setCellValue(header)
        }

        invoices.forEachIndexed { rowIndex, invoice ->
            val row = detailsSheet.createRow(rowIndex + 1)
            row.createCell(0).setCellValue(invoice.invoiceNumber)
            row.createCell(1).setCellValue(invoice.date)
            row.createCell(2).setCellValue(invoice.vendor)
            row.createCell(3).setCellValue(invoice.amount)
            row.createCell(4).setCellValue(invoice.totalHT)
        }

        for (i in 0..4) {
            detailsSheet.autoSizeColumn(i)
        }

        // Summary sheet
        val summarySheet = workbook.createSheet("Summary")
        summarySheet.createRow(0).createCell(0).setCellValue("Total Invoices")
        summarySheet.createRow(1).createCell(0).setCellValue(invoices.size.toDouble())
        
        val totalAmount = invoices.sumOf { it.amount }
        summarySheet.createRow(3).createCell(0).setCellValue("Total Amount")
        summarySheet.createRow(4).createCell(0).setCellValue(totalAmount)

        // Write to file
        workbook.use { wb ->
            excelFile.outputStream().use { output ->
                wb.write(output)
            }
        }

        return excelFile
    }

    /**
     * Validate invoice data before export
     */
    fun validateInvoiceData(invoices: List<InvoiceData>): ValidationResult {
        val errors = mutableListOf<String>()

        invoices.forEachIndexed { index, invoice ->
            if (invoice.invoiceNumber.isBlank()) {
                errors.add("Invoice #${index + 1}: Missing invoice number")
            }
            if (invoice.vendor.isBlank()) {
                errors.add("Invoice #${index + 1}: Missing vendor name")
            }
            if (invoice.amount <= 0) {
                errors.add("Invoice #${index + 1}: Invalid amount")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Generate a default file name with timestamp in desktop/masdac-ocr-xlsx folder
     */
    private fun generateDefaultFileName(): File {
        val timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        )
        // Create masdac-ocr-xlsx folder on desktop
        val desktopDir = File(System.getProperty("user.home"), "Desktop")
        val exportDir = File(desktopDir, "masdac-ocr-xlsx")
        exportDir.mkdirs()
        return File(exportDir, "invoices_$timestamp.xlsx")
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)