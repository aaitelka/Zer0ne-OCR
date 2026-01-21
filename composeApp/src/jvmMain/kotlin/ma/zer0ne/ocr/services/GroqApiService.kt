package ma.zer0ne.ocr.services

// GroqApiService.kt
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ma.zer0ne.ocr.model.groq.GroqMessage
import ma.zer0ne.ocr.model.groq.GroqRequest
import ma.zer0ne.ocr.model.groq.GroqResponse
import ma.zer0ne.ocr.model.groq.InvoiceData
import ma.zer0ne.ocr.utils.Logger
import java.io.File
import java.util.*

/**
 * Custom serializer for Double that handles:
 * - European format (1.108,00)
 * - US format (1108.00)
 * - Numeric JSON values (integers and decimals)
 * - String JSON values
 */
object EuropeanDoubleSerializer : KSerializer<Double> {
    override val descriptor = PrimitiveSerialDescriptor("EuropeanDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double {
        return try {
            // Try to decode as a number first (handles JSON numbers)
            decoder.decodeDouble()
        } catch (e: Exception) {
            // Fall back to string decoding (handles JSON strings)
            try {
                val value = decoder.decodeString()
                parseEuropeanDouble(value)
            } catch (e2: Exception) {
                Logger.error("Failed to parse double value: ${e.message}, ${e2.message}")
                0.0
            }
        }
    }

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeDouble(value)
    }

    private fun parseEuropeanDouble(value: String): Double {
        // Remove whitespace
        val cleaned = value.trim()
        
        // Handle European format: 1.108,00 -> 1108.00
        // and US format: 1108.00 -> 1108.00
        return when {
            cleaned.contains(",") && cleaned.contains(".") -> {
                // Has both - European format with thousands separator
                // "1.108,00" -> remove . and replace , with .
                val normalized = cleaned.replace(".", "").replace(",", ".")
                normalized.toDoubleOrNull() ?: 0.0
            }
            cleaned.contains(",") -> {
                // Only comma - European decimal separator
                // "1108,00" -> replace , with .
                cleaned.replace(",", ".").toDoubleOrNull() ?: 0.0
            }
            else -> {
                // Standard format or no decimal
                // "1108.00" or "1108"
                cleaned.toDoubleOrNull() ?: 0.0
            }
        }
    }
}

class GroqApiService(private val apiKey: String) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }

    companion object {
        private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val MODEL = "meta-llama/llama-4-maverick-17b-128e-instruct" // Latest Llama 4 model
    }

    suspend fun processInvoiceImage(imageFile: File): InvoiceData {
        val base64Image = encodeImageToBase64(imageFile)
        val mimeType = when (imageFile.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "image/jpeg"
        }

        val request = GroqRequest(
            model = MODEL,
            messages = listOf(
                GroqMessage(
                    role = "user",
                    content = listOf(
                        buildJsonObject {
                            put("type", "text")
                            put(
                                "text", """
                                EXTRACT INVOICE DATA FOR EXCEL SPREADSHEET
                                
                                Extract EXACTLY these 5 fields from the invoice image:
                                
                                invoiceNumber|date|vendor|amount|totalHT
                                
                                Requirements:
                                - invoiceNumber: Invoice number/ID (string, required)
                                - date: Invoice date in DD-MM-YYYY format (string, required)
                                - vendor: Company/supplier name (string, required)
                                - amount: Total amount (number, required). Use totalHT if not visible
                                - totalHT: Total before tax (number, required). Use amount if HT not visible
                                
                                Return ONLY valid JSON with these exact field names, ready for Excel:
                                {"invoiceNumber":"VALUE","date":"DD-MM-YYYY","vendor":"VALUE","amount":0.00,"totalHT":0.00}
                                
                                CRITICAL: All 5 fields MUST be present. Do not omit any field.
                                Vendor names may include special characters.
                                Vendor names in most cases inside logo or header area.
                            """.trimIndent()
                            )
                        },
                        buildJsonObject {
                            put("type", "image_url")
                            put("image_url", buildJsonObject {
                                put("url", "data:$mimeType;base64,$base64Image")
                            })
                        }
                    )
                )
            ),
            temperature = 0.1,
            maxTokens = 500
        )

        return try {
            val response: HttpResponse = client.post(GROQ_API_URL) {
                header("Authorization", "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val responseBody = response.bodyAsText()
            Logger.log("Groq API Response: $responseBody")

            if (response.status.value >= 400) {
                val errorMsg = "Groq API error (${response.status.value}): $responseBody"
                Logger.error(errorMsg)
                throw Exception(errorMsg)
            }

            val json = Json { ignoreUnknownKeys = true }
            val groqResponse: GroqResponse = try {
                json.decodeFromString(responseBody)
            } catch (e: Exception) {
                Logger.error("Failed to parse response as GroqResponse: ${e.message}", e)
                throw Exception("Failed to parse API response: ${e.message}")
            }

            val content = groqResponse.choices.firstOrNull()?.message?.content ?: ""
            if (content.isBlank()) {
                Logger.error("No content in API response")
                throw Exception("No content in API response")
            }

            Logger.log("Successfully extracted invoice data from API")
            // Parse JSON from response
            parseInvoiceData(content)
        } catch (e: Exception) {
            Logger.error("Failed to process invoice: ${e.message}", e)
            throw Exception("Failed to process invoice: ${e.message}", e)
        }
    }

    private fun parseInvoiceData(jsonString: String): InvoiceData {
        // Extract JSON from mark-down code blocks if present
        val cleanJson = jsonString
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val json = Json { ignoreUnknownKeys = true }

        try {
            val data = json.decodeFromString<InvoiceData>(cleanJson)

            // Validate that all required fields are present and non-empty
            if (data.invoiceNumber.isBlank()) {
                throw Exception("invoiceNumber field is empty")
            }
            if (data.date.isBlank()) {
                throw Exception("date field is empty")
            }
            if (data.vendor.isBlank()) {
                throw Exception("vendor field is empty")
            }
            if (data.amount <= 0) {
                throw Exception("amount field is missing or invalid")
            }
            if (data.totalHT <= 0) {
                throw Exception("totalHT field is missing or invalid")
            }

            return data
        } catch (e: Exception) {
            Logger.error("JSON Parsing error: ${e.message}")
            Logger.log("Raw JSON: $cleanJson")
            throw Exception("Failed to parse invoice data: ${e.message}", e)
        }
    }

    private fun encodeImageToBase64(file: File): String {
        val bytes = file.readBytes()
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun close() {
        client.close()
    }
}
