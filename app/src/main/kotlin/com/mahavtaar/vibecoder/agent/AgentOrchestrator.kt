package com.mahavtaar.vibecoder.agent

import com.mahavtaar.vibecoder.agent.memory.AgentMemory
import com.mahavtaar.vibecoder.agent.tools.ToolRegistry
import com.mahavtaar.vibecoder.agent.tools.ToolResult
import com.mahavtaar.vibecoder.data.db.AuditLog
import com.mahavtaar.vibecoder.data.db.AuditLogDao
import com.mahavtaar.vibecoder.llm.ContextManager
import com.mahavtaar.vibecoder.llm.LlamaEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

sealed class AgentEvent {
    data class Thought(val text: String) : AgentEvent()
    data class ActionStarted(val toolName: String, val args: JsonObject) : AgentEvent()
    data class ActionCompleted(val toolName: String, val result: ToolResult) : AgentEvent()
    data class FinalAnswer(val text: String) : AgentEvent()
    data class ConfirmationRequired(val message: String, val pendingArgs: JsonObject, val toolName: String) : AgentEvent()
    object StepLimitReached : AgentEvent()
    data class Error(val message: String) : AgentEvent()
}

class AgentOrchestrator @Inject constructor(
    private val llamaEngine: LlamaEngine,
    private val toolRegistry: ToolRegistry,
    private val memory: AgentMemory,
    private val auditLogDao: AuditLogDao
) {
    private val contextManager = ContextManager()

    suspend fun runTask(
        task: String,
        context: AgentContext,
        requestConfirmation: suspend (AgentEvent.ConfirmationRequired) -> Boolean
    ): Flow<AgentEvent> = flow {
        if (!llamaEngine.isLoaded) {
            emit(AgentEvent.Error("LLM Engine is not loaded"))
            return@flow
        }

        val memorySnapshot = memory.listAll().joinToString("\n") { "${it.key}: ${it.value}" }
        val systemPrompt = AgentSystemPrompt.build(
            toolDescriptions = toolRegistry.getAllDescriptions(),
            context = context,
            memorySnapshot = memorySnapshot
        )

        val conversation = mutableListOf(
            "System: $systemPrompt",
            "User: $task"
        )

        var stepCount = 0
        var parseErrorCount = 0

        while (stepCount < context.maxSteps) {
            stepCount++

            // Trim context to fit token limit (using 8192 as example default)
            val trimmedContext = contextManager.trimToFit(conversation, 6000)
            val fullPrompt = trimmedContext.joinToString("\n\n")

            val outputBuilder = StringBuilder()
            try {
                llamaEngine.generate(fullPrompt).collect { token ->
                    outputBuilder.append(token)
                }
            } catch (e: Exception) {
                emit(AgentEvent.Error("LLM Generation failed: ${e.message}"))
                return@flow
            }

            val llmOutput = outputBuilder.toString()
            conversation.add("Agent: $llmOutput")

            when (val parsed = ReActParser.parse(llmOutput)) {
                is ParsedStep.Thinking -> {
                    emit(AgentEvent.Thought(parsed.thought))
                    logAudit(context.sessionId, stepCount, "THOUGHT", outputText = parsed.thought)
                    conversation.add("System: Please provide an ACTION or FINAL_ANSWER.")
                }
                is ParsedStep.ToolCall -> {
                    parseErrorCount = 0 // Reset on success
                    emit(AgentEvent.Thought(parsed.thought))
                    emit(AgentEvent.ActionStarted(parsed.toolName, parsed.args))
                    logAudit(context.sessionId, stepCount, "ACTION_STARTED", toolName = parsed.toolName, inputJson = parsed.args.toString(), outputText = parsed.thought)

                    val tool = toolRegistry.getTool(parsed.toolName)
                    if (tool == null) {
                        val errorResult = ToolResult.Error("Tool not found: ${parsed.toolName}")
                        emit(AgentEvent.ActionCompleted(parsed.toolName, errorResult))
                        conversation.add("Observation: Tool not found. Please pick from the provided list.")
                        logAudit(context.sessionId, stepCount, "ACTION_ERROR", toolName = parsed.toolName, outputText = "Tool not found")
                        continue
                    }

                    // Pre-execution confirmation check based on settings
                    val needsPreExecutionConfirmation = (parsed.toolName.contains("shell") && !context.autoConfirmShell) ||
                                                        (parsed.toolName.contains("write") && !context.autoConfirmWrite)

                    if (needsPreExecutionConfirmation) {
                        val event = AgentEvent.ConfirmationRequired("Are you sure you want to run ${parsed.toolName}?", parsed.args, parsed.toolName)
                        emit(event)
                        val approved = requestConfirmation(event)
                        if (!approved) {
                            val rejectResult = ToolResult.Error("Action rejected by user")
                            emit(AgentEvent.ActionCompleted(parsed.toolName, rejectResult))
                            conversation.add("Observation: User rejected the action.")
                            logAudit(context.sessionId, stepCount, "ACTION_REJECTED", toolName = parsed.toolName)
                            continue
                        }
                    }

                    // Execute Tool
                    val result = tool.execute(parsed.args)
                    emit(AgentEvent.ActionCompleted(parsed.toolName, result))

                    val obsText = when (result) {
                        is ToolResult.Success -> result.output
                        is ToolResult.Error -> result.message
                        is ToolResult.Confirmation -> {
                            // The tool itself requires confirmation mid-execution
                            val event = AgentEvent.ConfirmationRequired(result.message, result.pendingArgs, parsed.toolName)
                            emit(event)
                            val approved = requestConfirmation(event)
                            if (approved) {
                                "User approved the confirmation: ${result.message}. You may proceed."
                            } else {
                                "User rejected the confirmation: ${result.message}."
                            }
                        }
                    }

                    conversation.add("Observation: $obsText")
                    logAudit(context.sessionId, stepCount, "ACTION_COMPLETED", toolName = parsed.toolName, outputText = obsText)
                }
                is ParsedStep.FinalAnswer -> {
                    emit(AgentEvent.Thought(parsed.thought))
                    emit(AgentEvent.FinalAnswer(parsed.answer))
                    logAudit(context.sessionId, stepCount, "FINAL_ANSWER", outputText = parsed.answer)
                    return@flow
                }
                is ParsedStep.ParseError -> {
                    emit(AgentEvent.Error(parsed.raw))
                    logAudit(context.sessionId, stepCount, "ERROR", outputText = "Parse Error: ${parsed.raw}")
                    parseErrorCount++

                    if (parseErrorCount >= 3) {
                        emit(AgentEvent.Error("Failed to parse ReAct format 3 times consecutively. Aborting."))
                        return@flow
                    }

                    conversation.add("System: Format error. You must strictly follow the THOUGHT, ACTION, ACTION_INPUT format.")
                }
            }
        }

        emit(AgentEvent.StepLimitReached)
        logAudit(context.sessionId, stepCount, "ERROR", outputText = "Step limit reached")
    }

    private suspend fun logAudit(
        sessionId: String,
        stepNumber: Int,
        eventType: String,
        toolName: String? = null,
        inputJson: String? = null,
        outputText: String? = null
    ) {
        auditLogDao.insert(
            AuditLog(
                sessionId = sessionId,
                stepNumber = stepNumber,
                eventType = eventType,
                toolName = toolName,
                inputJson = inputJson,
                outputText = outputText
            )
        )
    }
}
