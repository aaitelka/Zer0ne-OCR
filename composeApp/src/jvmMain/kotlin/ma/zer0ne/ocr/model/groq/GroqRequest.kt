package ma.zer0ne.ocr.model.groq

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Double = 0.1,
    @SerialName("max_tokens")
    val maxTokens: Int? = null
)