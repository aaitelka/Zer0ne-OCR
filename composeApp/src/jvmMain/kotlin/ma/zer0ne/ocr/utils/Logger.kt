package ma.zer0ne.ocr.utils

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Logger {
    private val logsDir = File(System.getProperty("user.home"), ".invoice_ocr/logs")
    private val logFile = File(logsDir, "app.log")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    init {
        // Create logs directory if it doesn't exist
        if (!logsDir.exists()) {
            logsDir.mkdirs()
        }
    }

    fun log(message: String, throwable: Throwable? = null) {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        val logMessage = "[$timestamp] $message"
        
        println(logMessage)
        
        try {
            logFile.appendText("$logMessage\n")
            if (throwable != null) {
                logFile.appendText("${throwable.stackTraceToString()}\n")
            }
        } catch (e: Exception) {
            System.err.println("Failed to write to log file: ${e.message}")
        }
    }

    fun error(message: String, throwable: Throwable? = null) {
        val errorMessage = "[ERROR] $message"
        log(errorMessage, throwable)
    }

    fun getLogFilePath(): String = logFile.absolutePath
}
