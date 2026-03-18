package com.mahavtaar.vibecoder.ui.agent

import kotlinx.serialization.json.JsonObject

sealed class AgentStepUi {
    data class ThoughtBubble(val text: String) : AgentStepUi()
    data class ActionCard(val toolName: String, val argsJson: JsonObject, val status: ActionStatus) : AgentStepUi()
    data class ObservationCard(val toolName: String, val result: String, val isExpanded: Boolean = false) : AgentStepUi()
    data class FinalAnswerCard(val text: String) : AgentStepUi()
    data class ConfirmationCard(val message: String, val toolName: String, val pendingArgs: JsonObject) : AgentStepUi()
}

enum class ActionStatus {
    RUNNING, SUCCESS, ERROR
}

data class ConfirmationRequest(
    val message: String,
    val toolName: String,
    val args: JsonObject
)

data class AgentUiState(
    val isRunning: Boolean = false,
    val steps: List<AgentStepUi> = emptyList(),
    val awaitingConfirmation: ConfirmationRequest? = null,
    val finalAnswer: String? = null
)
