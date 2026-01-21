package ma.zer0ne.ocr.config

// ConfigManager.kt
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
data class AppConfig(
    val groqApiKey: String = "",
    val lastExportPath: String = "",
    val autoExport: Boolean = false,
    val dpi: Int = 300
)

class ConfigManager {

    private val configDir = File(System.getProperty("user.home"), ".invoice_ocr")
    private val configFile = File(configDir, "config.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        // Create config directory if it doesn't exist
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
    }

    /**
     * Load configuration from file
     */
    fun loadConfig(): AppConfig {
        return try {
            if (configFile.exists()) {
                val jsonString = configFile.readText()
                json.decodeFromString<AppConfig>(jsonString)
            } else {
                // Return default config if file doesn't exist
                AppConfig()
            }
        } catch (e: Exception) {
            println("Error loading config: ${e.message}")
            AppConfig()
        }
    }

    /**
     * Save configuration to file
     */
    fun saveConfig(config: AppConfig) {
        try {
            val jsonString = json.encodeToString(config)
            configFile.writeText(jsonString)
        } catch (e: Exception) {
            println("Error saving config: ${e.message}")
        }
    }

    /**
     * Update API key
     */
    fun updateApiKey(apiKey: String) {
        val config = loadConfig()
        saveConfig(config.copy(groqApiKey = apiKey))
    }

    /**
     * Update last export path
     */
    fun updateLastExportPath(path: String) {
        val config = loadConfig()
        saveConfig(config.copy(lastExportPath = path))
    }

    /**
     * Check if API key is configured
     */
    fun hasApiKey(): Boolean {
        return loadConfig().groqApiKey.isNotBlank()
    }

    /**
     * Get API key
     */
    fun getApiKey(): String? {
        val key = loadConfig().groqApiKey
        return if (key.isBlank()) null else key
    }
}