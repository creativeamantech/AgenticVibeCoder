package com.mahavtaar.vibecoder.editor

data class FileTab(
    val id: String,
    val filePath: String,
    val fileName: String,
    val language: String,
    val content: String,
    val isDirty: Boolean = false,
    val cursorLine: Int = 1
)
