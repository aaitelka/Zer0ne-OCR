package ma.zer0ne.ocr.model

/**
 * Status of a file being processed
 */
enum class FileStatus {
    Pending,
    Processing,
    Completed,
    Error
}
