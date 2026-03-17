package com.mahavtaar.vibecoder.llm

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import javax.inject.Inject

class LlamaCppEngine @Inject constructor(
    private val dispatcher: CoroutineDispatcher
) : LlamaEngine {

    override var isLoaded: Boolean = false
        private set

    override var modelInfo: ModelInfo? = null
        private set

    private var modelPtr: Long = 0
    private var ctxPtr: Long = 0

    override suspend fun loadModel(modelPath: String, contextSize: Int): Boolean = withContext(dispatcher) {
        try {
            val file = File(modelPath)
            if (!file.exists()) return@withContext false

            val nGpuLayers = 0 // CPU for now unless exposed to UI settings
            modelPtr = LlamaJni.loadModel(file.absolutePath, contextSize, nGpuLayers)
            if (modelPtr == 0L) return@withContext false

            // Default physical cores / 2 threads
            val nThreads = maxOf(1, Runtime.getRuntime().availableProcessors() / 2)
            ctxPtr = LlamaJni.createContext(modelPtr, contextSize, nThreads)
            if (ctxPtr == 0L) {
                LlamaJni.freeModel(modelPtr)
                modelPtr = 0
                return@withContext false
            }

            val infoJson = LlamaJni.getModelInfo(modelPtr)
            val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(infoJson).jsonObject
            val nParams = json["n_params"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
            val nCtxTrain = json["n_ctx_train"]?.jsonPrimitive?.content?.toIntOrNull() ?: contextSize
            val nVocab = json["n_vocab"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0

            isLoaded = true
            modelInfo = ModelInfo(
                name = file.name,
                path = file.absolutePath,
                sizeBytes = file.length(),
                contextLength = nCtxTrain,
                quantization = "unknown (GGUF)"
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun generate(prompt: String, onToken: (String) -> Unit): Flow<String> {
        val channel = Channel<String>(Channel.UNLIMITED)

        kotlinx.coroutines.GlobalScope.launch(dispatcher) {
            try {
                if (!isLoaded || ctxPtr == 0L) {
                    channel.close(IllegalStateException("Model not loaded"))
                    return@launch
                }

                val tokenIds = LlamaJni.tokenize(ctxPtr, prompt, true)
                if (tokenIds.isEmpty()) {
                    channel.close(IllegalStateException("Failed to tokenize prompt"))
                    return@launch
                }

                LlamaJni.generate(
                    ctxPtr = ctxPtr,
                    tokenIds = tokenIds,
                    maxNewTokens = 2048, // Can be dynamic
                    temperature = 0.2f,
                    topP = 0.95f,
                    callback = { token ->
                        channel.trySend(token)
                        onToken(token)
                    }
                )
            } catch (e: Exception) {
                // Ignore silent failures for flow cancellation
            } finally {
                channel.close()
            }
        }
        return channel.receiveAsFlow().flowOn(dispatcher)
    }

    override suspend fun stop() = withContext(dispatcher) {
        if (isLoaded) {
            LlamaJni.stopGeneration()
        }
    }

    override fun unloadModel() {
        kotlinx.coroutines.GlobalScope.launch(dispatcher) {
            if (ctxPtr != 0L) LlamaJni.freeContext(ctxPtr)
            if (modelPtr != 0L) LlamaJni.freeModel(modelPtr)

            ctxPtr = 0
            modelPtr = 0
            isLoaded = false
            modelInfo = null
        }
    }
}
