package com.mahavtaar.vibecoder.llm

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = true
)

@Serializable
data class OllamaResponse(
    val model: String,
    val created_at: String,
    val response: String,
    val done: Boolean
)

class OllamaEngine(
    private val host: String = "192.168.1.100",
    private val port: Int = 11434,
    private val modelName: String = "llama3"
) : LlamaEngine {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = 10000
            socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
        }
    }

    override var isLoaded: Boolean = false
        private set

    override var modelInfo: ModelInfo? = null
        private set

    @Volatile
    private var isStopped = false

    override suspend fun loadModel(modelPath: String, contextSize: Int): Boolean {
        // Ollama manages its own models, so we just set state to loaded
        isLoaded = true
        modelInfo = ModelInfo(
            name = modelName,
            path = "http://$host:$port",
            sizeBytes = 0L,
            contextLength = contextSize,
            quantization = "unknown"
        )
        return true
    }

    override fun generate(prompt: String, onToken: (String) -> Unit): Flow<String> = flow {
        isStopped = false
        val url = "http://$host:$port/api/generate"
        val request = OllamaRequest(model = modelName, prompt = prompt, stream = true)

        try {
            client.preparePost(url) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.execute { response ->
                val channel: ByteReadChannel = response.bodyAsChannel()
                while (!channel.isClosedForRead && !isStopped) {
                    val line = channel.readUTF8Line()
                    if (line != null && line.isNotBlank()) {
                        try {
                            val parsed = json.decodeFromString<OllamaResponse>(line)
                            val token = parsed.response
                            onToken(token)
                            emit(token)
                            if (parsed.done) break
                        } catch (e: Exception) {
                            // Malformed JSON chunk, ignore or log
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit("\n[Error connecting to Ollama: ${e.message}]")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun stop() {
        isStopped = true
    }

    override fun unloadModel() {
        isLoaded = false
        modelInfo = null
    }
}
