package ma.zer0ne.ocr.converters

// PdfToImageConverter.kt
import kotlinx.coroutines.*
import ma.zer0ne.ocr.utils.Logger
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.Executors
import javax.imageio.ImageIO

class PdfToImageConverter {

    // Thread pool for parallel image conversion
    private val threadPool = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
    )
    private val dispatcher = threadPool.asCoroutineDispatcher()

    /**
     * Converts a PDF file to a list of image files (one per page) using parallel processing
     * @param pdfFile The input PDF file
     * @param outputDir The directory where images will be saved
     * @return List of created image files (sorted by page number)
     */
    fun convertPdfToImages(pdfFile: File, outputDir: File): List<File> {
        if (!pdfFile.exists() || !pdfFile.extension.equals("pdf", ignoreCase = true)) {
            throw IllegalArgumentException("Invalid PDF file: ${pdfFile.absolutePath}")
        }

        // Create output directory if it doesn't exist
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        return runBlocking {
            convertPdfToImagesAsync(pdfFile, outputDir)
        }
    }

    /**
     * Async version of PDF to images conversion using coroutines
     */
    suspend fun convertPdfToImagesAsync(pdfFile: File, outputDir: File): List<File> = coroutineScope {
        val document = Loader.loadPDF(pdfFile)

        try {
            val pageCount = document.numberOfPages
            Logger.log("Converting PDF with $pageCount pages using ${Runtime.getRuntime().availableProcessors()} threads")

            val startTime = System.currentTimeMillis()

            // Create jobs for each page
            val jobs = (0 until pageCount).map { pageIndex ->
                async(dispatcher) {
                    convertPageToImage(document, pageIndex, pdfFile.nameWithoutExtension, outputDir)
                }
            }

            // Wait for all pages to complete and collect results
            val imageFiles = jobs.awaitAll().filterNotNull().sortedBy {
                // Sort by page number extracted from filename
                val regex = Regex("""_page_(\d+)\.png$""")
                regex.find(it.name)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            }

            val elapsedTime = System.currentTimeMillis() - startTime
            Logger.log("PDF conversion completed: $pageCount pages in ${elapsedTime}ms (${elapsedTime / pageCount}ms per page)")

            imageFiles
        } finally {
            document.close()
        }
    }

    /**
     * Convert a single page to image (thread-safe)
     */
    private fun convertPageToImage(
        document: PDDocument,
        pageIndex: Int,
        baseName: String,
        outputDir: File
    ): File? {
        return try {
            // PDFRenderer is not thread-safe, create new instance per thread
            val pdfRenderer = PDFRenderer(document)

            // Render page at 300 DPI for good quality
            val image: BufferedImage = synchronized(document) {
                pdfRenderer.renderImageWithDPI(pageIndex, 300f)
            }

            // Create output file
            val outputFile = File(outputDir, "${baseName}_page_${pageIndex + 1}.png")

            // Save as PNG
            ImageIO.write(image, "PNG", outputFile)

            Logger.log("Converted page ${pageIndex + 1}: ${outputFile.name}")
            outputFile
        } catch (e: Exception) {
            Logger.error("Failed to convert page ${pageIndex + 1}: ${e.message}", e)
            null
        }
    }

    /**
     * Converts a single PDF page to an image
     * @param pdfFile The input PDF file
     * @param pageIndex The page index (0-based)
     * @param outputFile The output image file
     */
    fun convertSinglePage(pdfFile: File, pageIndex: Int, outputFile: File) {
        Loader.loadPDF(pdfFile).use { document ->
            if (pageIndex >= document.numberOfPages) {
                throw IllegalArgumentException("Page index $pageIndex out of bounds")
            }

            val pdfRenderer = PDFRenderer(document)
            val image: BufferedImage = pdfRenderer.renderImageWithDPI(pageIndex, 300f)

            ImageIO.write(image, "PNG", outputFile)
        }
    }

    /**
     * Cleanup resources
     */
    fun shutdown() {
        threadPool.shutdown()
    }
}