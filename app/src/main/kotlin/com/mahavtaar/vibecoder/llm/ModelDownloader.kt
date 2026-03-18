package com.mahavtaar.vibecoder.llm

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percent: Float
)

class ModelDownloader(private val context: Context) {

    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = 10000
            socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
        }
    }

    fun downloadModel(url: String, fileName: String): Flow<DownloadProgress> = flow {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        val targetFile = File(modelsDir, fileName)

        try {
            client.prepareGet(url).execute { httpResponse ->
                if (!httpResponse.status.isSuccess()) {
                    throw IllegalStateException("Download failed with status: ${httpResponse.status}")
                }

                val totalBytes = httpResponse.contentLength() ?: -1L
                val channel: ByteReadChannel = httpResponse.bodyAsChannel()

                var bytesCopied = 0L
                FileOutputStream(targetFile).use { output ->
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(8192)
                        while (!packet.isEmpty) {
                            val bytes = packet.readBytes()
                            output.write(bytes)
                            bytesCopied += bytes.size
                            val percent = if (totalBytes > 0) {
                                (bytesCopied.toFloat() / totalBytes) * 100f
                            } else 0f
                            emit(DownloadProgress(bytesCopied, totalBytes, percent))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
            throw e
        }
    }.flowOn(Dispatchers.IO)
}
