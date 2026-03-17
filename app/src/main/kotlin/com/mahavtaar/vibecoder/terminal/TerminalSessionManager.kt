package com.mahavtaar.vibecoder.terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TerminalSessionManager {
    private val _sessions = MutableStateFlow<List<TerminalSession>>(emptyList())
    val sessions: StateFlow<List<TerminalSession>> = _sessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    fun createSession(workingDir: String): TerminalSession? {
        if (_sessions.value.size >= 5) return null

        val newSession = TerminalSession(workingDir)
        _sessions.update { it + newSession }
        _activeSessionId.value = newSession.id
        return newSession
    }

    fun closeSession(id: String) {
        val currentSessions = _sessions.value.toMutableList()
        val index = currentSessions.indexOfFirst { it.id == id }

        if (index != -1) {
            val sessionToClose = currentSessions[index]
            sessionToClose.kill()
            currentSessions.removeAt(index)
            _sessions.value = currentSessions

            if (_activeSessionId.value == id) {
                _activeSessionId.value = currentSessions.lastOrNull()?.id
            }
        }
    }

    fun switchSession(id: String) {
        if (_sessions.value.any { it.id == id }) {
            _activeSessionId.value = id
        }
    }

    fun getActiveSession(): TerminalSession? {
        return _sessions.value.find { it.id == _activeSessionId.value }
    }

    fun destroyAll() {
        _sessions.value.forEach { it.kill() }
        _sessions.value = emptyList()
        _activeSessionId.value = null
    }
}
