package ma.zer0ne.ocr.model.groq

import kotlinx.serialization.Serializable

@Serializable
data class Choice(
    val message: MessageResponse
)