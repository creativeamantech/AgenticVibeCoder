package com.mahavtaar.vibecoder.llm

data class ModelInfo(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val contextLength: Int,
    val quantization: String
)
