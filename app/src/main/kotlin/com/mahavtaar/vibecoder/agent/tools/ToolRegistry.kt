package com.mahavtaar.vibecoder.agent.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(
    private val tools: Set<@JvmSuppressWildcards AgentTool>
) {
    private val toolMap = tools.associateBy { it.name }

    fun getTool(name: String): AgentTool? = toolMap[name]

    fun getAllDescriptions(): String {
        val toolList = tools.map { tool ->
            buildJsonObject {
                put("name", tool.name)
                put("description", tool.description)
                put("parameters", tool.parameters)
            }
        }

        // Wrap the list in a top-level JsonArray
        val jsonArray = JsonArray(toolList)
        return Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), jsonArray)
    }
}
