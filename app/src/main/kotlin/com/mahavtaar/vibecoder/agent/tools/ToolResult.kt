package com.mahavtaar.vibecoder.agent.tools

import kotlinx.serialization.json.JsonObject

sealed class ToolResult {
    data class Success(val output: String) : ToolResult()
    data class Error(val message: String) : ToolResult()
    data class Confirmation(val message: String, val pendingArgs: JsonObject) : ToolResult()
}
