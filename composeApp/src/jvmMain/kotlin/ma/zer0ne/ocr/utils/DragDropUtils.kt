package ma.zer0ne.ocr.utils

import androidx.compose.ui.awt.ComposeWindow
import ma.zer0ne.ocr.model.InvoiceFile
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.io.File

/**
 * Setup drag and drop functionality for a window
 */
fun setupDragAndDrop(window: ComposeWindow, onFilesDropped: (List<File>) -> Unit) {
    val dropTarget = object : DropTarget() {
        override fun drop(event: DropTargetDropEvent) {
            try {
                event.acceptDrop(DnDConstants.ACTION_COPY)
                val transferable = event.transferable

                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @Suppress("UNCHECKED_CAST")
                    val droppedItems = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                    val allFiles = getAllSupportedFiles(droppedItems)
                    if (allFiles.isNotEmpty()) {
                        onFilesDropped(allFiles)
                    }
                }
                event.dropComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                event.dropComplete(false)
            }
        }
    }

    window.contentPane.dropTarget = dropTarget
}

/**
 * Convert File list to InvoiceFile list
 */
fun filesToInvoiceFiles(files: List<File>): List<InvoiceFile> {
    return files.map { file ->
        InvoiceFile(
            id = "${System.currentTimeMillis()}_${file.hashCode()}",
            name = file.name,
            path = file.absolutePath
        )
    }
}
