package ma.zer0ne.ocr.model

import ma.zer0ne.ocr.model.groq.InvoiceData

data class InvoiceFile(
    val id: String,
    val name: String,
    val path: String,
    var status: FileStatus = FileStatus.Pending,
    var data: InvoiceData? = null,
    var error: String? = null
)
