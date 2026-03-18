package com.mahavtaar.vibecoder.ui.filetree

import java.io.File

sealed class FileTreeNode {
    abstract val name: String
    abstract val path: String
    abstract val isDirectory: Boolean

    data class Directory(
        override val name: String,
        override val path: String,
        val children: List<FileTreeNode>,
        val isExpanded: Boolean = false
    ) : FileTreeNode() {
        override val isDirectory = true
    }

    data class FileNode(
        override val name: String,
        override val path: String,
        val language: String,
        val sizeBytes: Long
    ) : FileTreeNode() {
        override val isDirectory = false
    }
}
