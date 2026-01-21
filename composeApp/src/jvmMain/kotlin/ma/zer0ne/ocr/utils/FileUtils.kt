package ma.zer0ne.ocr.utils

import java.awt.Desktop
import java.io.File

// Supported file extensions
val SUPPORTED_EXTENSIONS = listOf("jpg", "jpeg", "png", "pdf")

/**
 * Open folder in file manager
 */
fun openFolderInExplorer(folderPath: String) {
    try {
        val folder = File(folderPath)
        if (folder.exists() && folder.isDirectory) {
            val desktop = Desktop.getDesktop()
            desktop.open(folder)
        }
    } catch (e: Exception) {
        Logger.error("Failed to open folder: ${e.message}", e)
    }
}

/**
 * Recursively get all supported files from a list of files/folders
 * Handles both individual files and directories
 */
fun getAllSupportedFiles(items: List<File>): List<File> {
    val allFiles = mutableListOf<File>()
    items.forEach { item ->
        if (item.isDirectory) {
            item.listFiles()?.forEach { file ->
                allFiles.addAll(getAllSupportedFiles(listOf(file)))
            }
        } else if (item.extension.lowercase() in SUPPORTED_EXTENSIONS) {
            allFiles.add(item)
        }
    }
    return allFiles.distinctBy { it.absolutePath }.sortedBy { it.name }
}
