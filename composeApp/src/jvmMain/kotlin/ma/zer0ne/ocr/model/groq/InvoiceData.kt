package ma.zer0ne.ocr.model.groq

import kotlinx.serialization.Serializable
import ma.zer0ne.ocr.services.EuropeanDoubleSerializer

@Serializable
data class InvoiceData(
    val invoiceNumber: String,
    val date: String,
    val vendor: String,
    @Serializable(with = EuropeanDoubleSerializer::class)
    val amount: Double,
    @Serializable(with = EuropeanDoubleSerializer::class)
    val totalHT: Double
)