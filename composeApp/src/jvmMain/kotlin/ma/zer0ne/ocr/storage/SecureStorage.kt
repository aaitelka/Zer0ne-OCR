package ma.zer0ne.ocr.storage

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ma.zer0ne.ocr.utils.Logger
import java.io.File
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Secure encrypted storage for sensitive data like API keys.
 * Uses AES-256 encryption with PBKDF2 key derivation.
 */
object SecureStorage {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val IV_LENGTH = 16
    private const val SALT_LENGTH = 16
    private const val PBKDF2_ITERATIONS = 65536
    private const val PBKDF2_KEY_LENGTH = 256

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val storageDir = File(System.getProperty("user.home"), ".zer0ne-ocr")
    private val storageFile = File(storageDir, "secure_keys.dat")
    private val saltFile = File(storageDir, ".salt")

    private val devicePassword: CharArray by lazy {
        buildString {
            append(System.getProperty("user.name", ""))
            append(System.getProperty("os.name", ""))
            append(System.getProperty("user.home", ""))
            try {
                val networkInterfaces = java.net.NetworkInterface.getNetworkInterfaces()
                while (networkInterfaces.hasMoreElements()) {
                    val ni = networkInterfaces.nextElement()
                    val mac = ni.hardwareAddress
                    if (mac != null && mac.isNotEmpty()) {
                        append(mac.joinToString("") { "%02x".format(it) })
                        break
                    }
                }
            } catch (_: Exception) {}
        }.toCharArray()
    }

    init { if (!storageDir.exists()) storageDir.mkdirs() }

    private fun getOrCreateSalt(): ByteArray {
        return if (saltFile.exists()) {
            Base64.getDecoder().decode(saltFile.readText())
        } else {
            val salt = ByteArray(SALT_LENGTH)
            SecureRandom().nextBytes(salt)
            saltFile.writeText(Base64.getEncoder().encodeToString(salt))
            try { Runtime.getRuntime().exec(arrayOf("chmod", "600", saltFile.absolutePath)) } catch (_: Exception) {}
            salt
        }
    }

    private fun deriveKey(salt: ByteArray): SecretKeySpec {
        val keySpec = PBEKeySpec(devicePassword, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = keyFactory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun encrypt(data: String): String {
        val salt = getOrCreateSalt()
        val key = deriveKey(salt)
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val combined = iv + encrypted
        return Base64.getEncoder().encodeToString(combined)
    }

    private fun decrypt(encryptedData: String): String {
        val salt = getOrCreateSalt()
        val key = deriveKey(salt)
        val combined = Base64.getDecoder().decode(encryptedData)
        val iv = combined.copyOfRange(0, IV_LENGTH)
        val encrypted = combined.copyOfRange(IV_LENGTH, combined.size)
        val ivSpec = IvParameterSpec(iv)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    fun saveApiKeys(keys: List<String>) {
        try {
            val data = ApiKeysData(keys = keys, lastUpdated = System.currentTimeMillis())
            val jsonData = json.encodeToString(data)
            val encrypted = encrypt(jsonData)
            storageFile.writeText(encrypted)
            try { Runtime.getRuntime().exec(arrayOf("chmod", "600", storageFile.absolutePath)) } catch (_: Exception) {}
            Logger.log("API keys saved securely (${keys.size} keys)")
        } catch (e: Exception) {
            Logger.error("Failed to save API keys: ${e.message}", e)
            throw e
        }
    }

    fun loadApiKeys(): List<String> {
        return try {
            if (!storageFile.exists()) {
                Logger.log("No secure storage file found")
                emptyList()
            } else {
                val encrypted = storageFile.readText()
                val jsonData = decrypt(encrypted)
                val data = json.decodeFromString<ApiKeysData>(jsonData)
                Logger.log("Loaded ${data.keys.size} API keys from secure storage")
                data.keys
            }
        } catch (e: Exception) {
            Logger.error("Failed to load API keys: ${e.message}", e)
            emptyList()
        }
    }

    fun addApiKey(key: String): Boolean {
        return try {
            val currentKeys = loadApiKeys().toMutableList()
            if (!currentKeys.contains(key)) {
                currentKeys.add(key)
                saveApiKeys(currentKeys)
                true
            } else {
                Logger.log("API key already exists")
                false
            }
        } catch (e: Exception) {
            Logger.error("Failed to add API key: ${e.message}", e)
            false
        }
    }

    fun removeApiKey(key: String): Boolean {
        return try {
            val currentKeys = loadApiKeys().toMutableList()
            if (currentKeys.remove(key)) {
                saveApiKeys(currentKeys)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Logger.error("Failed to remove API key: ${e.message}", e)
            false
        }
    }

    fun hasApiKeys(): Boolean = loadApiKeys().isNotEmpty()
    fun getApiKeyCount(): Int = loadApiKeys().size

    fun clearAllKeys() {
        try {
            if (storageFile.exists()) storageFile.delete()
            Logger.log("All API keys cleared")
        } catch (e: Exception) {
            Logger.error("Failed to clear API keys: ${e.message}", e)
        }
    }

    fun migrateFromPlainTextFile(file: File): Boolean {
        return try {
            if (file.exists()) {
                val keys = file.readLines().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }
                if (keys.isNotEmpty()) {
                    saveApiKeys(keys)
                    Logger.log("Migrated ${keys.size} API keys from plain text to secure storage")
                    true
                } else false
            } else false
        } catch (e: Exception) {
            Logger.error("Failed to migrate API keys: ${e.message}", e)
            false
        }
    }
}

@Serializable
private data class ApiKeysData(
    val keys: List<String>,
    val lastUpdated: Long
)
