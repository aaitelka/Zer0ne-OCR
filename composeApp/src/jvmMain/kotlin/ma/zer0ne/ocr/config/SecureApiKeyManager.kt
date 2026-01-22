package ma.zer0ne.ocr.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ma.zer0ne.ocr.storage.SecureStorage
import ma.zer0ne.ocr.utils.ApiKeysLocator
import ma.zer0ne.ocr.utils.Logger
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Manages multiple Groq API keys with secure encrypted storage.
 * Features:
 * - AES-256 encrypted storage for API keys
 * - Smart rotation with random selection to distribute load
 * - Rate limit tracking and automatic cooldown
 * - Migration from plain text files to secure storage
 */
class SecureApiKeyManager {
    private var apiKeys: List<String> = emptyList()
    private val _apiKeysFlow = MutableStateFlow<List<String>>(emptyList())
    val apiKeysFlow: StateFlow<List<String>> get() = _apiKeysFlow.asStateFlow()

    // Track rate-limited keys with their cooldown expiry time
    private val rateLimitedKeys = ConcurrentHashMap<String, Long>()

    // Track usage count per key
    private val keyUsageCount = ConcurrentHashMap<String, Int>()

    // Cooldown period for rate-limited keys (in milliseconds)
    private val RATE_LIMIT_COOLDOWN_MS = 60_000L // 1 minute cooldown

    // Current key index for round-robin fallback
    private var currentKeyIndex = 0

    init {
        loadApiKeys()
    }

    /**
     * Load API keys from secure storage
     * Falls back to plain text file migration if secure storage is empty
     */
    private fun loadApiKeys() {
        try {
            // First try to load from secure storage
            apiKeys = SecureStorage.loadApiKeys()
            _apiKeysFlow.value = getMaskedKeys()

            // If no keys in secure storage, try to migrate from plain text file
            if (apiKeys.isEmpty()) {
                Logger.log("No keys in secure storage, checking for plain text file to migrate...")
                val plainTextFile = ApiKeysLocator.findApiKeysFile()
                if (plainTextFile != null) {
                    Logger.log("Found plain text API keys file, migrating to secure storage...")
                    if (SecureStorage.migrateFromPlainTextFile(plainTextFile)) {
                        apiKeys = SecureStorage.loadApiKeys()
                        Logger.log("Successfully migrated ${apiKeys.size} keys to secure storage")
                    }
                }
            }

            if (apiKeys.isEmpty()) {
                Logger.log("No API keys available. Please add keys in Settings.")
                _apiKeysFlow.value = emptyList()
            } else {
                Logger.log("Loaded {apiKeys.size} API key(s) from secure storage")
                // Initialize usage counts
                apiKeys.forEach { keyUsageCount[it] = 0 }
                _apiKeysFlow.value = getMaskedKeys()
            }
        } catch (e: Exception) {
            Logger.error("Failed to load API keys: ${e.message}", e)
            apiKeys = emptyList()
            _apiKeysFlow.value = emptyList()
        }
    }

    /**
     * Reload keys from secure storage
     */
    fun reloadKeys() {
        loadApiKeys()
    }

    /**
     * Add a new API key
     */
    fun addKey(key: String): Boolean {
        val trimmedKey = key.trim()
        if (trimmedKey.isBlank()) return false

        val success = SecureStorage.addApiKey(trimmedKey)
        if (success) {
            reloadKeys()
            _apiKeysFlow.value = getMaskedKeys()
        }
        return success
    }

    /**
     * Remove an API key
     */
    fun removeKey(key: String): Boolean {
        val success = SecureStorage.removeApiKey(key)
        if (success) {
            reloadKeys()
            _apiKeysFlow.value = getMaskedKeys()
        }
        return success
    }

    /**
     * Get all stored keys (masked for display)
     */
    fun getMaskedKeys(): List<String> {
        return apiKeys.map { maskKey(it) }
    }

    /**
     * Get all stored keys (full, for internal use)
     */
    fun getAllKeys(): List<String> = apiKeys.toList()

    /**
     * Mask an API key for safe display
     */
    private fun maskKey(key: String): String {
        return if (key.length > 12) {
            "${key.take(8)}...${key.takeLast(4)}"
        } else {
            "${key.take(4)}..."
        }
    }

    /**
     * Get the next available API key using smart selection:
     * 1. Filter out rate-limited keys (still in cooldown)
     * 2. Select randomly from available keys to distribute load
     * 3. If all keys are rate-limited, return the one with shortest cooldown
     */
    fun getNextApiKey(): String? {
        if (apiKeys.isEmpty()) {
            Logger.error("No API keys available")
            return null
        }

        // Clean up expired rate limits
        val currentTime = System.currentTimeMillis()
        rateLimitedKeys.entries.removeIf { it.value < currentTime }

        // Get available keys (not rate-limited)
        val availableKeys = apiKeys.filter { it !in rateLimitedKeys.keys }

        val selectedKey = if (availableKeys.isNotEmpty()) {
            // Randomly select from available keys to distribute load
            val randomIndex = Random.nextInt(availableKeys.size)
            availableKeys[randomIndex]
        } else {
            // All keys are rate-limited, find the one with shortest remaining cooldown
            Logger.log("[WARNING] All API keys are rate-limited, selecting one with shortest cooldown")
            val keyWithShortestCooldown = rateLimitedKeys.minByOrNull { it.value }?.key
            if (keyWithShortestCooldown != null) {
                // Remove from rate-limited so it can be tried
                rateLimitedKeys.remove(keyWithShortestCooldown)
                keyWithShortestCooldown
            } else {
                // Fallback to round-robin
                apiKeys[currentKeyIndex++ % apiKeys.size]
            }
        }

        // Track usage
        keyUsageCount[selectedKey] = (keyUsageCount[selectedKey] ?: 0) + 1

        Logger.log("Using API key: ${maskKey(selectedKey)} (usage: ${keyUsageCount[selectedKey]})")
        return selectedKey
    }

    /**
     * Mark an API key as rate-limited
     */
    fun markKeyAsRateLimited(apiKey: String) {
        val cooldownExpiry = System.currentTimeMillis() + RATE_LIMIT_COOLDOWN_MS
        rateLimitedKeys[apiKey] = cooldownExpiry
        Logger.log("[WARNING] API key rate-limited: ${maskKey(apiKey)}")
    }

    /**
     * Mark an API key as failed (permanent failure)
     */
    fun markKeyAsFailed(apiKey: String) {
        val cooldownExpiry = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
        rateLimitedKeys[apiKey] = cooldownExpiry
        Logger.log("[ERROR] API key marked as failed: ${maskKey(apiKey)}")
    }

    /**
     * Get count of currently available keys
     */
    fun getAvailableKeysCount(): Int {
        val currentTime = System.currentTimeMillis()
        return apiKeys.count { it !in rateLimitedKeys.keys || rateLimitedKeys[it]!! < currentTime }
    }

    /**
     * Get total number of keys
     */
    fun getTotalKeysCount(): Int = apiKeys.size

    /**
     * Reset all rate limits and usage tracking
     */
    fun resetAll() {
        rateLimitedKeys.clear()
        keyUsageCount.clear()
        apiKeys.forEach { keyUsageCount[it] = 0 }
        currentKeyIndex = 0
        Logger.log("API key manager reset")
    }

    /**
     * Check if we have any API keys
     */
    fun hasKeys(): Boolean = apiKeys.isNotEmpty()

    /**
     * Clear all stored keys
     */
    fun clearAllKeys() {
        SecureStorage.clearAllKeys()
        apiKeys = emptyList()
        rateLimitedKeys.clear()
        keyUsageCount.clear()
        _apiKeysFlow.value = emptyList()
    }

    companion object {
        // Singleton instance
        @Volatile
        private var instance: SecureApiKeyManager? = null

        fun getInstance(): SecureApiKeyManager {
            return instance ?: synchronized(this) {
                instance ?: SecureApiKeyManager().also { instance = it }
            }
        }
    }
}
