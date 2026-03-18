package com.mahavtaar.vibecoder.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MlcEngine : LlamaEngine {
    override val isLoaded: Boolean
        get() = false

    override val modelInfo: ModelInfo?
        get() = null

    override suspend fun loadModel(modelPath: String, contextSize: Int): Boolean {
        // TODO: Phase 2 - Implement MLC-LLM engine integration
        return false
    }

    override fun generate(prompt: String, onToken: (String) -> Unit): Flow<String> = flow {
        // TODO: Phase 2 - Implement MLC-LLM generation
    }

    override suspend fun stop() {
        // TODO: Phase 2 - Implement stop for MLC-LLM
    }

    override fun unloadModel() {
        // TODO: Phase 2 - Implement unload for MLC-LLM
    }
}
