package ma.zer0ne.ocr.config

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ma.zer0ne.ocr.model.PromptConfig

/**
 * Manages prompt configuration settings
 */
@Stable
class PromptConfigManager private constructor() {
    private val _promptConfigFlow = MutableStateFlow(
        PromptConfig(
            useCustomPrompt = false,
            selectedFields = setOf("Invoice Number", "Total Amount"),
            customPrompt = ""
        )
    )

    val promptConfigFlow: StateFlow<PromptConfig> = _promptConfigFlow.asStateFlow()

    fun updateUseCustomPrompt(useCustom: Boolean) {
        val current = _promptConfigFlow.value
        _promptConfigFlow.value = current.copy(useCustomPrompt = useCustom)
    }

    fun updateSelectedFields(fields: Set<String>) {
        val current = _promptConfigFlow.value
        _promptConfigFlow.value = current.copy(selectedFields = fields)
    }

    fun updateCustomPrompt(prompt: String) {
        val current = _promptConfigFlow.value
        _promptConfigFlow.value = current.copy(customPrompt = prompt)
    }

    fun getPromptConfig(): PromptConfig = _promptConfigFlow.value

    fun buildPrompt(): String {
        val config = _promptConfigFlow.value
        return if (config.useCustomPrompt) {
            config.customPrompt
        } else {
            buildFieldsWithStaticPrompt(config.selectedFields)
        }
    }

    private fun buildFieldsWithStaticPrompt(selectedFields: Set<String>): String {
        val fieldsStr = selectedFields.joinToString(", ")
        return """
            Extract the following invoice information: $fieldsStr
            
            Provide the information in a structured format.
            Return only the extracted data without any additional text.
        """.trimIndent()
    }

    companion object {
        @Volatile
        private var instance: PromptConfigManager? = null

        fun getInstance(): PromptConfigManager {
            return instance ?: synchronized(this) {
                instance ?: PromptConfigManager().also { instance = it }
            }
        }
    }
}
