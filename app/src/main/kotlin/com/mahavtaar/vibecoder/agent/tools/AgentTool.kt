package com.mahavtaar.vibecoder.agent.tools

import kotlinx.serialization.json.JsonObject

interface AgentTool {
    val name: String
    val description: String
    val parameters: JsonObject
    suspend fun execute(args: JsonObject): ToolResult
}
