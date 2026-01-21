package ma.zer0ne.ocr.config

import ma.zer0ne.ocr.utils.Logger
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Manages multiple Groq API keys with smart rotation, rate limit handling, and fallback
 * - Uses random key selection to distribute load
 * - Tracks per-key usage and cooldown for rate-limited keys
 * - Automatically switches keys on rate limit (429) errors
 */
class ApiKeyManager(private val apiKeysFilePath: String) {

    private var apiKeys: List<String> = emptyList()

    // Track rate-limited keys with their cooldown expiry time
    private val rateLimitedKeys = ConcurrentHashMap<String, Long>()

    // Track usage count per key (resets when all keys are rate limited)
    private val keyUsageCount = ConcurrentHashMap<String, Int>()

    // Cooldown period for rate-limited keys (in milliseconds)
    private val RATE_LIMIT_COOLDOWN_MS = 60_000L // 1 minute cooldown

    // Current key index for round-robin fallback
    private var currentKeyIndex = 0

    init {
        loadApiKeys()
    }

    /**
     * Load API keys from the specified file
     */
    private fun loadApiKeys() {
        try {
            val file = File(apiKeysFilePath)
            if (!file.exists()) {
                Logger.error("API keys file not found: $apiKeysFilePath")
                apiKeys = emptyList()
                return
            }

            apiKeys = file.readLines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }

            if (apiKeys.isEmpty()) {
                Logger.error("No valid API keys found in file: $apiKeysFilePath")
            } else {
                Logger.log("Loaded ${apiKeys.size} API key(s)")
                // Initialize usage counts
                apiKeys.forEach { keyUsageCount[it] = 0 }
            }
        } catch (e: Exception) {
            Logger.error("Failed to load API keys from file: ${e.message}", e)
            apiKeys = emptyList()
        }
    }

    /**
     * Get the next available API key using smart selection:
     * 1. Filter out rate-limited keys (still in cooldown)
     * 2. Select randomly from available keys to distribute load
     * 3. If all keys are rate-limited, wait or return the one with shortest cooldown
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

        Logger.log("Using API key: ${selectedKey.take(15)}... (usage: ${keyUsageCount[selectedKey]})")
        return selectedKey
    }

    /**
     * Mark an API key as rate-limited
     * The key will be in cooldown for RATE_LIMIT_COOLDOWN_MS
     */
    fun markKeyAsRateLimited(apiKey: String) {
        val cooldownExpiry = System.currentTimeMillis() + RATE_LIMIT_COOLDOWN_MS
        rateLimitedKeys[apiKey] = cooldownExpiry
        Logger.log("[WARNING] API key rate-limited, cooldown until ${cooldownExpiry}: ${apiKey.take(15)}...")
    }

    /**
     * Mark an API key as failed (permanent failure, not rate limit)
     */
    fun markKeyAsFailed(apiKey: String) {
        // For permanent failures, set a very long cooldown (24 hours)
        val cooldownExpiry = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
        rateLimitedKeys[apiKey] = cooldownExpiry
        Logger.log("[ERROR] API key marked as failed: ${apiKey.take(15)}...")
    }

    /**
     * Get count of currently available (not rate-limited) keys
     */
    fun getAvailableKeysCount(): Int {
        val currentTime = System.currentTimeMillis()
        return apiKeys.count { it !in rateLimitedKeys.keys || rateLimitedKeys[it]!! < currentTime }
    }

    /**
     * Get total number of loaded keys
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
     * Check if we have any API keys loaded
     */
    fun hasKeys(): Boolean = apiKeys.isNotEmpty()
}
