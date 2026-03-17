package com.mahavtaar.vibecoder.editor

import com.mahavtaar.vibecoder.agent.tools.FileTool
import com.mahavtaar.vibecoder.agent.tools.ReadFileTool
import com.mahavtaar.vibecoder.agent.tools.ToolResult
import com.mahavtaar.vibecoder.agent.tools.WriteFileTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileTabManager @Inject constructor(
    private val workingDir: File
) {
    private val _openTabs = MutableStateFlow<List<FileTab>>(emptyList())
    val openTabs: StateFlow<List<FileTab>> = _openTabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    private val readFileTool = ReadFileTool(workingDir)
    private val writeFileTool = WriteFileTool(workingDir)

    suspend fun openFile(path: String): FileTab? {
        val existingTab = _openTabs.value.find { it.filePath == path }
        if (existingTab != null) {
            _activeTabId.value = existingTab.id
            return existingTab
        }

        val result = readFileTool.execute(buildJsonObject { put("path", path) })

        val content = if (result is ToolResult.Success) result.output else ""
        val fileName = path.substringAfterLast('/')
        val ext = fileName.substringAfterLast('.', "")

        val lang = detectLanguage(ext)

        val newTab = FileTab(
            id = UUID.randomUUID().toString(),
            filePath = path,
            fileName = fileName,
            language = lang,
            content = content
        )

        _openTabs.value = _openTabs.value + newTab
        _activeTabId.value = newTab.id

        return newTab
    }

    fun closeTab(id: String) {
        val tabs = _openTabs.value.toMutableList()
        val index = tabs.indexOfFirst { it.id == id }
        if (index != -1) {
            tabs.removeAt(index)
            _openTabs.value = tabs
            if (_activeTabId.value == id) {
                _activeTabId.value = if (tabs.isNotEmpty()) tabs.last().id else null
            }
        }
    }

    fun markDirty(id: String, content: String) {
        val tabs = _openTabs.value.toMutableList()
        val index = tabs.indexOfFirst { it.id == id }
        if (index != -1) {
            val oldTab = tabs[index]
            if (oldTab.content != content) {
                tabs[index] = oldTab.copy(content = content, isDirty = true)
                _openTabs.value = tabs
            }
        }
    }

    fun updateCursor(id: String, line: Int) {
        val tabs = _openTabs.value.toMutableList()
        val index = tabs.indexOfFirst { it.id == id }
        if (index != -1) {
            tabs[index] = tabs[index].copy(cursorLine = line)
            _openTabs.value = tabs
        }
    }

    suspend fun saveTab(id: String) {
        val tab = _openTabs.value.find { it.id == id } ?: return
        if (tab.isDirty) {
            val result = writeFileTool.execute(buildJsonObject {
                put("path", tab.filePath)
                put("content", tab.content)
            })

            if (result is ToolResult.Success) {
                val tabs = _openTabs.value.toMutableList()
                val index = tabs.indexOfFirst { it.id == id }
                if (index != -1) {
                    tabs[index] = tab.copy(isDirty = false)
                    _openTabs.value = tabs
                }
            }
        }
    }

    fun switchTab(id: String) {
        if (_openTabs.value.any { it.id == id }) {
            _activeTabId.value = id
        }
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
