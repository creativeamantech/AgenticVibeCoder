package com.mahavtaar.vibecoder.llm

import kotlinx.coroutines.flow.Flow

interface LlamaEngine {
    val isLoaded: Boolean
    val modelInfo: ModelInfo?

    suspend fun loadModel(modelPath: String, contextSize: Int = 8192): Boolean
    fun generate(prompt: String, onToken: (String) -> Unit = {}): Flow<String>
    suspend fun stop()
    fun unloadModel()
}
