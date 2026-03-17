package com.mahavtaar.vibecoder.ui.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.vibecoder.agent.AgentContext
import com.mahavtaar.vibecoder.agent.AgentEvent
import com.mahavtaar.vibecoder.agent.AgentOrchestrator
import com.mahavtaar.vibecoder.agent.tools.ToolResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val orchestrator: AgentOrchestrator
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    private var currentJob: Job? = null
    private var confirmationDeferred: CompletableDeferred<Boolean>? = null

    fun startTask(task: String, context: AgentContext) {
        if (_uiState.value.isRunning) return

        _uiState.update {
            it.copy(
                isRunning = true,
                steps = emptyList(),
                awaitingConfirmation = null,
                finalAnswer = null
            )
        }

        currentJob = viewModelScope.launch {

            // Define the suspend function that orchestrator will call when it needs confirmation
            val requestConfirmation: suspend (AgentEvent.ConfirmationRequired) -> Boolean = { event ->
                _uiState.update {
                    it.copy(awaitingConfirmation = ConfirmationRequest(event.message, event.toolName, event.pendingArgs))
                }
                addStep(AgentStepUi.ConfirmationCard(event.message, event.toolName, event.pendingArgs))

                confirmationDeferred = CompletableDeferred()
                val approved = confirmationDeferred?.await() ?: false

                _uiState.update { it.copy(awaitingConfirmation = null) }
                approved
            }

            orchestrator.runTask(task, context, requestConfirmation).collect { event ->
                when (event) {
                    is AgentEvent.Thought -> {
                        addStep(AgentStepUi.ThoughtBubble(event.text))
                    }
                    is AgentEvent.ActionStarted -> {
                        addStep(AgentStepUi.ActionCard(event.toolName, event.args, ActionStatus.RUNNING))
                    }
                    is AgentEvent.ActionCompleted -> {
                        updateLastActionStatus(event.toolName, if (event.result is ToolResult.Success) ActionStatus.SUCCESS else ActionStatus.ERROR)
                        val resultText = when (val res = event.result) {
                            is ToolResult.Success -> res.output
                            is ToolResult.Error -> res.message
                            is ToolResult.Confirmation -> "Confirmation required: ${res.message}"
                        }
                        addStep(AgentStepUi.ObservationCard(event.toolName, resultText))
                    }
                    is AgentEvent.ConfirmationRequired -> {
                        // Normally handled inside the suspend requestConfirmation lambda, but if emitted
                        // explicitly by orchestrator without awaiting, we just show it.
                        // (With the updated orchestrator, it suspends on requestConfirmation directly).
                    }
                    is AgentEvent.FinalAnswer -> {
                        _uiState.update { it.copy(finalAnswer = event.text, isRunning = false) }
                        addStep(AgentStepUi.FinalAnswerCard(event.text))
                    }
                    is AgentEvent.StepLimitReached -> {
                        _uiState.update { it.copy(isRunning = false) }
                        addStep(AgentStepUi.FinalAnswerCard("Step limit reached without final answer."))
                    }
                    is AgentEvent.Error -> {
                        _uiState.update { it.copy(isRunning = false) }
                        addStep(AgentStepUi.ObservationCard("System", "Error: ${event.message}"))
                    }
                }
            }
        }
    }

    fun confirmAction(approved: Boolean) {
        confirmationDeferred?.complete(approved)
        confirmationDeferred = null
    }

    fun stopAgent() {
        currentJob?.cancel()
        _uiState.update { it.copy(isRunning = false, awaitingConfirmation = null) }
        addStep(AgentStepUi.ObservationCard("System", "Agent stopped by user."))
    }

    fun toggleObservationExpansion(index: Int) {
        val steps = _uiState.value.steps.toMutableList()
        val step = steps.getOrNull(index)
        if (step is AgentStepUi.ObservationCard) {
            steps[index] = step.copy(isExpanded = !step.isExpanded)
            _uiState.update { it.copy(steps = steps) }
        }
    }

    private fun addStep(step: AgentStepUi) {
        _uiState.update { it.copy(steps = it.steps + step) }
    }

    private fun updateLastActionStatus(toolName: String, status: ActionStatus) {
        val steps = _uiState.value.steps.toMutableList()
        val lastActionIndex = steps.indexOfLast { it is AgentStepUi.ActionCard && it.toolName == toolName }
        if (lastActionIndex != -1) {
            val lastAction = steps[lastActionIndex] as AgentStepUi.ActionCard
            steps[lastActionIndex] = lastAction.copy(status = status)
            _uiState.update { it.copy(steps = steps) }
        }
    }
}
