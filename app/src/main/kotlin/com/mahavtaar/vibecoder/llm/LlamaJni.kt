package com.mahavtaar.vibecoder.llm

object LlamaJni {
    init {
        System.loadLibrary("llama_jni")
    }

    external fun loadModel(modelPath: String, contextSize: Int, nGpuLayers: Int): Long
    external fun createContext(modelPtr: Long, contextSize: Int, nThreads: Int): Long
    external fun tokenize(ctxPtr: Long, text: String, addBos: Boolean): IntArray
    external fun generate(ctxPtr: Long, tokenIds: IntArray, maxNewTokens: Int, temperature: Float, topP: Float, callback: TokenCallback): String
    external fun stopGeneration()
    external fun freeContext(ctxPtr: Long)
    external fun freeModel(modelPtr: Long)
    external fun getModelInfo(modelPtr: Long): String
    external fun getBatchSize(): Int
}

fun interface TokenCallback {
    fun onToken(token: String)
}
