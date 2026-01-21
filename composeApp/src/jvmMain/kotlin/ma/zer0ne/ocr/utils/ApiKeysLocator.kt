package ma.zer0ne.ocr.utils

import java.io.File

/**
 * Utility to find the API keys file in various standard locations.
 *
 * For Windows installed applications, the file can be placed in:
 * 1. Same directory as the executable (installation folder)
 * 2. User's home directory: C:\Users\{username}\.zer0ne-ocr\api_keys.txt
 * 3. AppData folder: C:\Users\{username}\AppData\Local\Zer0ne-OCR\api_keys.txt
 * 4. Current working directory
 * 5. Desktop folder
 *
 * For Linux/macOS:
 * 1. User's home directory: ~/.zer0ne-ocr/api_keys.txt
 * 2. Current working directory
 * 3. Same directory as the JAR/executable
 */
object ApiKeysLocator {

    private const val API_KEYS_FILENAME = "api_keys.txt"
    private const val APP_FOLDER_NAME = ".zer0ne-ocr"

    /**
     * Find the API keys file from various possible locations.
     * Returns the first file that exists, or null if not found.
     */
    fun findApiKeysFile(): File? {
        val possiblePaths = getPossiblePaths()

        for (path in possiblePaths) {
            Logger.log("Checking for API keys at: ${path.absolutePath}")
            if (path.exists() && path.isFile) {
                Logger.log("Found API keys file at: ${path.absolutePath}")
                return path
            }
        }

        Logger.log("API keys file not found in any standard location")
        return null
    }

    /**
     * Get the recommended path for creating a new API keys file.
     * Creates the parent directory if it doesn't exist.
     */
    fun getRecommendedPath(): File {
        val userHome = System.getProperty("user.home")
        val appDir = File(userHome, APP_FOLDER_NAME)

        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        return File(appDir, API_KEYS_FILENAME)
    }

    /**
     * Get all possible paths where the API keys file might be located.
     */
    private fun getPossiblePaths(): List<File> {
        val paths = mutableListOf<File>()
        val userHome = System.getProperty("user.home")
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")

        // 1. User's app config directory (highest priority)
        paths.add(File(userHome, "$APP_FOLDER_NAME/$API_KEYS_FILENAME"))

        // 2. Current working directory
        paths.add(File(API_KEYS_FILENAME))

        if (isWindows) {
            // 3. Windows AppData Local folder
            val appData = System.getenv("LOCALAPPDATA")
            if (appData != null) {
                paths.add(File(appData, "Zer0ne-OCR/$API_KEYS_FILENAME"))
            }

            // 4. Windows AppData Roaming folder
            val appDataRoaming = System.getenv("APPDATA")
            if (appDataRoaming != null) {
                paths.add(File(appDataRoaming, "Zer0ne-OCR/$API_KEYS_FILENAME"))
            }
        }

        // 5. Same directory as the running JAR/class
        try {
            val jarLocation = ApiKeysLocator::class.java.protectionDomain.codeSource.location
            val jarDir = File(jarLocation.toURI()).parentFile
            if (jarDir != null) {
                paths.add(File(jarDir, API_KEYS_FILENAME))
            }
        } catch (e: Exception) {
            // Ignore if we can't get the JAR location
        }

        // 6. Desktop folder (convenient for users)
        val desktop = File(userHome, "Desktop")
        if (desktop.exists()) {
            paths.add(File(desktop, API_KEYS_FILENAME))
        }

        return paths
    }

    /**
     * Get a human-readable string of all locations where the API keys file is searched.
     */
    fun getSearchLocationsInfo(): String {
        val userHome = System.getProperty("user.home")
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")

        val sb = StringBuilder()
        sb.appendLine("API keys file is searched in the following locations (in order):")
        sb.appendLine()
        sb.appendLine("1. ${File(userHome, "$APP_FOLDER_NAME/$API_KEYS_FILENAME").absolutePath}")
        sb.appendLine("2. Current working directory: ${File(API_KEYS_FILENAME).absolutePath}")

        if (isWindows) {
            val appData = System.getenv("LOCALAPPDATA")
            if (appData != null) {
                sb.appendLine("3. ${File(appData, "Zer0ne-OCR/$API_KEYS_FILENAME").absolutePath}")
            }
            val appDataRoaming = System.getenv("APPDATA")
            if (appDataRoaming != null) {
                sb.appendLine("4. ${File(appDataRoaming, "Zer0ne-OCR/$API_KEYS_FILENAME").absolutePath}")
            }
        }

        sb.appendLine()
        sb.appendLine("Recommended location: ${getRecommendedPath().absolutePath}")

        return sb.toString()
    }
}
