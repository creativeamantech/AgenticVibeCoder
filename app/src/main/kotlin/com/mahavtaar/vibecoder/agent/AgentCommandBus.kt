package com.mahavtaar.vibecoder.agent

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class AgentCommand {
    object Stop : AgentCommand()
    data class Confirm(val approved: Boolean) : AgentCommand()
}

@Singleton
class AgentCommandBus @Inject constructor() {
    private val _commands = MutableSharedFlow<AgentCommand>(extraBufferCapacity = 1)
    val commands = _commands.asSharedFlow()

    suspend fun sendCommand(command: AgentCommand) {
        _commands.emit(command)
    }
}
