package com.mahavtaar.vibecoder.agent

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

sealed class ParsedStep {
    data class Thinking(val thought: String) : ParsedStep()
    data class ToolCall(val thought: String, val toolName: String, val args: JsonObject) : ParsedStep()
    data class FinalAnswer(val thought: String, val answer: String) : ParsedStep()
    data class ParseError(val raw: String) : ParsedStep()
}

object ReActParser {
    fun parse(llmOutput: String): ParsedStep {
        val thoughtRegex = Regex("""THOUGHT:\s*(.*?)(?=\nACTION:|\nFINAL_ANSWER:|$)""", RegexOption.DOT_MATCHES_ALL)
        val actionRegex = Regex("""ACTION:\s*(\w+)""")
        val inputRegex = Regex("""ACTION_INPUT:\s*(\{.*?\})""", RegexOption.DOT_MATCHES_ALL)
        val answerRegex = Regex("""FINAL_ANSWER:\s*(.*?)$""", RegexOption.DOT_MATCHES_ALL)

        val thought = thoughtRegex.find(llmOutput)?.groupValues?.get(1)?.trim() ?: ""
        val finalAnswerMatch = answerRegex.find(llmOutput)

        if (finalAnswerMatch != null) {
            val answer = finalAnswerMatch.groupValues[1].trim()
            return ParsedStep.FinalAnswer(thought, answer)
        }

        val actionMatch = actionRegex.find(llmOutput)
        val inputMatch = inputRegex.find(llmOutput)

        if (actionMatch != null) {
            val toolName = actionMatch.groupValues[1].trim()
            val inputRaw = inputMatch?.groupValues?.get(1)?.trim()

            val jsonArgs = if (inputRaw != null) {
                try {
                    Json.parseToJsonElement(inputRaw).jsonObject
                } catch (e: Exception) {
                    return ParsedStep.ParseError("ACTION_INPUT is not a valid JSON object: $inputRaw")
                }
            } else {
                return ParsedStep.ParseError("ACTION_INPUT is missing")
            }

            return ParsedStep.ToolCall(thought, toolName, jsonArgs)
        }

        if (thought.isNotBlank()) {
            return ParsedStep.Thinking(thought)
        }

        return ParsedStep.ParseError("Could not parse ReAct format: $llmOutput")
    }
}
