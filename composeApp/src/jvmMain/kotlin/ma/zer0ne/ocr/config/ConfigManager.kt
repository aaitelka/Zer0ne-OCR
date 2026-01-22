package ma.zer0ne.ocr.config

// ConfigManager.kt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
data class AppConfig(
    val lastExportPath: String = "",
    val autoExport: Boolean = false,
    val dpi: Int = 300
)

class ConfigManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Load configuration from file (excluding API key)
     */
    fun loadConfig(): AppConfig {
        // Optionally implement loading from ~/.zer0ne-ocr/app_config.json
        return AppConfig()
    }

    /**
     * Save configuration to file (excluding API key)
     */
    fun saveConfig(config: AppConfig) {
        // Optionally implement saving to ~/.zer0ne-ocr/app_config.json
    }

    /**
     * Update last export path
     */
    fun updateLastExportPath(path: String) {
        // Optionally implement updating last export path in ~/.zer0ne-ocr/app_config.json
    }

    /**
     * Remove all API key logic from config
     */
    fun hasApiKey(): Boolean = false
    fun getApiKey(): String? = null
    fun updateApiKey(apiKey: String) { /* no-op */ }
}