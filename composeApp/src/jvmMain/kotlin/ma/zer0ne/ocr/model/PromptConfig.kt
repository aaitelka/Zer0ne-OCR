package ma.zer0ne.ocr.model

import androidx.compose.runtime.Stable

/**
 * Represents the prompt configuration for the Groq API
 */
@Stable
data class PromptConfig(
    val useCustomPrompt: Boolean = false,
    val selectedFields: Set<String> = setOf("Invoice Number", "Total Amount"),
    val customPrompt: String = "",
    val availableFields: List<String> = listOf(
        "Invoice Number",
        "Date",
        "Vendor Name",
        "Total Amount",
        "Tax Amount",
        "Line Items",
        "Description",
        "Account Code"
    )
)

/**
 * Enum to represent prompt mode
 */
enum class PromptMode {
    FIELDS_WITH_STATIC,  // Use selected fields mixed with static prompt
    CUSTOM               // Use raw custom prompt
}
