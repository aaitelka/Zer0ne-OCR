package ma.zer0ne.ocr.model.groq

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class GroqMessage(
    val role: String,
    val content: List<JsonElement>? = null
)