package com.mahavtaar.vibecoder.ui.filetree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FileTreeViewModel @Inject constructor(
    private val workingDir: File
) : ViewModel() {

    private val _treeRoot = MutableStateFlow<FileTreeNode.Directory?>(null)
    val treeRoot: StateFlow<FileTreeNode.Directory?> = _treeRoot.asStateFlow()

    private val expandedPaths = mutableSetOf<String>()

    init {
        loadDirectory(workingDir.absolutePath)
    }

    fun loadDirectory(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val rootFile = File(path)
            if (!rootFile.exists() || !rootFile.isDirectory) {
                rootFile.mkdirs()
            }

            // Auto-expand root
            expandedPaths.add(rootFile.absolutePath)

            val newRoot = buildNode(rootFile) as? FileTreeNode.Directory
            withContext(Dispatchers.Main) {
                _treeRoot.value = newRoot
            }
        }
    }

    private fun buildNode(file: File): FileTreeNode {
        return if (file.isDirectory) {
            val isExpanded = expandedPaths.contains(file.absolutePath)
            val children = if (isExpanded) {
                file.listFiles()
                    ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                    ?.map { buildNode(it) } ?: emptyList()
            } else {
                emptyList()
            }
            FileTreeNode.Directory(
                name = file.name,
                path = file.absolutePath,
                children = children,
                isExpanded = isExpanded
            )
        } else {
            FileTreeNode.FileNode(
                name = file.name,
                path = file.absolutePath,
                language = detectLanguage(file.extension),
                sizeBytes = file.length()
            )
        }
    }

    fun toggleExpand(node: FileTreeNode.Directory) {
        if (node.isExpanded) {
            expandedPaths.remove(node.path)
        } else {
            expandedPaths.add(node.path)
        }
        refreshTree()
    }

    fun createFile(parentPath: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(parentPath, name)
            if (!file.exists()) {
                file.createNewFile()
                expandedPaths.add(parentPath) // Ensure parent is expanded
                refreshTree()
            }
        }
    }

    fun createFolder(parentPath: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(parentPath, name)
            if (!dir.exists()) {
                dir.mkdirs()
                expandedPaths.add(parentPath)
                refreshTree()
            }
        }
    }

    fun deleteNode(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(path)
            if (file.exists()) {
                file.deleteRecursively()
                expandedPaths.remove(path)
                refreshTree()
            }
        }
    }

    fun renameNode(path: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(path)
            if (file.exists()) {
                val newFile = File(file.parentFile, newName)
                file.renameTo(newFile)
                if (file.isDirectory && expandedPaths.contains(path)) {
                    expandedPaths.remove(path)
                    expandedPaths.add(newFile.absolutePath)
                }
                refreshTree()
            }
        }
    }

    private fun refreshTree() {
        _treeRoot.value?.let { loadDirectory(it.path) }
    }

    private fun detectLanguage(ext: String): String {
        return when (ext.lowercase()) {
            "kt", "kts" -> "kotlin"
            "py" -> "python"
            "js" -> "javascript"
            "ts" -> "typescript"
            "html" -> "html"
            "css" -> "css"
            "sh", "bash" -> "shell"
            "json" -> "json"
            "md" -> "markdown"
            "xml" -> "xml"
            "gradle" -> "groovy"
            "java" -> "java"
            "c", "cpp", "h", "hpp" -> "cpp"
            else -> "plaintext"
        }
    }
}
