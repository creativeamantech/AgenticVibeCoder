package com.mahavtaar.vibecoder.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.vibecoder.terminal.TerminalJsBridge
import com.mahavtaar.vibecoder.terminal.TerminalSession
import com.mahavtaar.vibecoder.terminal.TerminalSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class TerminalSessionInfo(
    val id: String,
    val cwd: String,
    val title: String
)

data class TerminalUiState(
    val sessions: List<TerminalSessionInfo> = emptyList(),
    val activeSessionId: String? = null
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val sessionManager: TerminalSessionManager,
    private val workingDir: File
) : ViewModel(), TerminalJsBridge {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.sessions.collect { sessions ->
                updateUiState(sessions, sessionManager.activeSessionId.value)
            }
        }
        viewModelScope.launch {
            sessionManager.activeSessionId.collect { activeId ->
                updateUiState(sessionManager.sessions.value, activeId)
            }
        }

        // Auto-create first session if none exist
        if (sessionManager.sessions.value.isEmpty()) {
            createNewSession()
        }
    }

    private fun updateUiState(sessions: List<TerminalSession>, activeId: String?) {
        val infos = sessions.map { session ->
            TerminalSessionInfo(
                id = session.id,
                cwd = session.getCwd(),
                title = "sh (${session.getCwd().substringAfterLast('/')})"
            )
        }
        _uiState.update { it.copy(sessions = infos, activeSessionId = activeId) }
    }

    fun createNewSession() {
        sessionManager.createSession(workingDir.absolutePath)
    }

    fun closeSession(id: String) {
        sessionManager.closeSession(id)
    }

    fun switchSession(id: String) {
        sessionManager.switchSession(id)
    }

    fun executeCommand(cmd: String) {
        val activeSession = sessionManager.getActiveSession()
        if (activeSession != null) {
            viewModelScope.launch {
                activeSession.executeCommand(cmd)
            }
        }
    }

    private var webViewWrapper: TerminalWebViewWrapper? = null

    fun setWebViewWrapper(wrapper: TerminalWebViewWrapper) {
        this.webViewWrapper = wrapper
    }

    fun clearTerminal() {
        viewModelScope.launch {
            webViewWrapper?.clear()
        }
    }

    fun getActiveSession(): TerminalSession? {
        return sessionManager.getActiveSession()
    }

    @android.webkit.JavascriptInterface
    override fun onUserInput(key: String) {
        // Typically used for PTY. We use ProcessBuilder which doesn't accept char-by-char cleanly.
        // We handle input locally in JS and submit full commands.
    }

    @android.webkit.JavascriptInterface
    override fun onCommandSubmitted(command: String) {
        executeCommand(command)
    }
}
