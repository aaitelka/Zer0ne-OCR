package ma.zer0ne.ocr.model.groq

import kotlinx.serialization.Serializable

@Serializable
data class GroqResponse(
    val choices: List<Choice>
)