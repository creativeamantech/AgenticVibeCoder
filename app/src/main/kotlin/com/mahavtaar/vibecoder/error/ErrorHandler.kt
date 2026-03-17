package com.mahavtaar.vibecoder.error

sealed class AppError(open val message: String) {
    data class LlmError(override val message: String) : AppError(message)
    data class ToolError(val toolName: String, override val message: String) : AppError("Tool $toolName failed: $message")
    data class BrowserError(val url: String, override val message: String) : AppError("Browser error on $url: $message")
    data class FileError(val path: String, override val message: String) : AppError("File error on $path: $message")
    data class NetworkError(override val message: String) : AppError(message)
    data class UnknownError(val throwable: Throwable) : AppError(throwable.localizedMessage ?: "Unknown error")
}

fun Throwable.toAppError(context: String): AppError {
    return when (this) {
        is java.io.IOException -> AppError.NetworkError("$context: ${this.localizedMessage}")
        is java.lang.IllegalArgumentException -> AppError.ToolError(context, this.localizedMessage ?: "Invalid arguments")
        else -> AppError.UnknownError(this)
    }
}
