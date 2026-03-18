package com.mahavtaar.vibecoder.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val tabs: List<FileTab> = emptyList(),
    val activeTab: FileTab? = null,
    val cursorLine: Int = 1,
    val cursorCol: Int = 1,
    val isGhostTextVisible: Boolean = false,
    val isSaving: Boolean = false,
    val isDiffMode: Boolean = false,
    val diffOriginal: String? = null,
    val diffModified: String? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val tabManager: FileTabManager,
    private val inlineCompletion: InlineCompletion
) : ViewModel(), EditorJsBridge {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var webViewWrapper: WebViewWrapper? = null

    init {
        viewModelScope.launch {
            tabManager.openTabs.collect { tabs ->
                _uiState.update { it.copy(tabs = tabs) }
                updateActiveTabState()
            }
        }
        viewModelScope.launch {
            tabManager.activeTabId.collect { id ->
                updateActiveTabState()
                val activeTab = tabManager.openTabs.value.find { it.id == id }
                if (activeTab != null && webViewWrapper != null) {
                    webViewWrapper?.setContent(activeTab.content, activeTab.language)
                }
            }
        }
        viewModelScope.launch {
            inlineCompletion.ghostText.collect { text ->
                _uiState.update { it.copy(isGhostTextVisible = text != null) }
            }
        }
    }

    fun setWebViewWrapper(wrapper: WebViewWrapper) {
        this.webViewWrapper = wrapper
        val activeTab = _uiState.value.activeTab
        if (activeTab != null) {
            viewModelScope.launch {
                wrapper.setContent(activeTab.content, activeTab.language)
            }
        }
    }

    private fun updateActiveTabState() {
        val activeId = tabManager.activeTabId.value
        val activeTab = tabManager.openTabs.value.find { it.id == activeId }
        _uiState.update { it.copy(activeTab = activeTab) }
    }

    fun openFile(path: String) {
        viewModelScope.launch {
            tabManager.openFile(path)
        }
    }

    fun saveCurrentFile() {
        val activeId = _uiState.value.activeTab?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            tabManager.saveTab(activeId)
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun closeTab(id: String) {
        tabManager.closeTab(id)
    }

    fun switchTab(id: String) {
        tabManager.switchTab(id)
    }

    @android.webkit.JavascriptInterface
    override fun onContentChanged(content: String) {
        val activeId = _uiState.value.activeTab?.id ?: return
        tabManager.markDirty(activeId, content)

        if (webViewWrapper != null) {
            val state = _uiState.value
            inlineCompletion.triggerDebounced(content, state.cursorLine, state.cursorCol, webViewWrapper!!)
        }
    }

    @android.webkit.JavascriptInterface
    override fun onCursorMoved(line: Int, col: Int) {
        val activeId = _uiState.value.activeTab?.id ?: return
        tabManager.updateCursor(activeId, line)
        _uiState.update { it.copy(cursorLine = line, cursorCol = col) }

        if (webViewWrapper != null) {
            inlineCompletion.dismiss(webViewWrapper!!)
        }
    }

    @android.webkit.JavascriptInterface
    override fun onSaveRequested() {
        saveCurrentFile()
    }

    @android.webkit.JavascriptInterface
    override fun onContextMenuAction(action: String, selectedText: String) {
        // TODO: Phase 4/6 integration - Send this task to the AgentOrchestrator
        // For example: if action == "Explain", create task "Explain this code: \n$selectedText"
        println("Context Menu Action: $action on text: $selectedText")
    }

    fun applyAgentDiff(originalCode: String, modifiedCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDiffMode = true, diffOriginal = originalCode, diffModified = modifiedCode) }
            webViewWrapper?.applyDiff(originalCode, modifiedCode)
        }
    }

    fun exitAgentDiff(accept: Boolean) {
        viewModelScope.launch {
            val modified = _uiState.value.diffModified
            webViewWrapper?.exitDiff()
            _uiState.update { it.copy(isDiffMode = false, diffOriginal = null, diffModified = null) }

            if (accept && modified != null) {
                val activeId = _uiState.value.activeTab?.id
                if (activeId != null) {
                    tabManager.markDirty(activeId, modified)
                    webViewWrapper?.setContent(modified, _uiState.value.activeTab?.language ?: "plaintext")
                }
            } else {
                // Revert to original content if rejected
                val original = _uiState.value.activeTab?.content ?: ""
                webViewWrapper?.setContent(original, _uiState.value.activeTab?.language ?: "plaintext")
            }
        }
    }

    fun acceptGhostText() {
        if (webViewWrapper != null) {
            inlineCompletion.accept(webViewWrapper!!)
        }
    }
}
