package com.mahavtaar.vibecoder.agent.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryHashMap @Inject constructor() : HashMap<String, String>()

class RememberTool(private val memory: InMemoryHashMap) : AgentTool {
    override val name = "remember"
    override val description = "Store in working memory"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("key") {
                put("type", "string")
            }
            putJsonObject("value") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("key"), kotlinx.serialization.json.JsonPrimitive("value"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult {
        val key = args["key"]?.jsonPrimitive?.content ?: return ToolResult.Error("Missing key")
        val value = args["value"]?.jsonPrimitive?.content ?: return ToolResult.Error("Missing value")

        memory[key] = value
        return ToolResult.Success("Remembered '$key'")
    }
}

class RecallTool(private val memory: InMemoryHashMap) : AgentTool {
    override val name = "recall"
    override val description = "Retrieve from memory"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("key") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("key"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult {
        val key = args["key"]?.jsonPrimitive?.content ?: return ToolResult.Error("Missing key")
        val value = memory[key]

        return if (value != null) {
            ToolResult.Success(value)
        } else {
            ToolResult.Error("Key '$key' not found in memory")
        }
    }
}

class ListMemoryTool(private val memory: InMemoryHashMap) : AgentTool {
    override val name = "list_memory"
    override val description = "List all memory keys"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {}
    }

    override suspend fun execute(args: JsonObject): ToolResult {
        return if (memory.isEmpty()) {
            ToolResult.Success("Memory is empty")
        } else {
            ToolResult.Success("Keys: " + memory.keys.joinToString(", "))
        }
    }
}

class ForgetTool(private val memory: InMemoryHashMap) : AgentTool {
    override val name = "forget"
    override val description = "Delete memory key"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("key") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("key"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult {
        val key = args["key"]?.jsonPrimitive?.content ?: return ToolResult.Error("Missing key")

        return if (memory.remove(key) != null) {
            ToolResult.Success("Forgot '$key'")
        } else {
            ToolResult.Error("Key '$key' not found in memory")
        }
    }
}

class AddToScratchpadTool(private val memory: InMemoryHashMap) : AgentTool {
    override val name = "add_to_scratchpad"
    override val description = "Add content to agent scratchpad"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("content") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("content"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult {
        val content = args["content"]?.jsonPrimitive?.content ?: return ToolResult.Error("Missing content")
        val current = memory["_scratchpad"] ?: ""
        memory["_scratchpad"] = current + "\n" + content
        return ToolResult.Success("Added to scratchpad")
    }
}

class ClearScratchpadTool(private val memory: InMemoryHashMap) : AgentTool {
    override val name = "clear_scratchpad"
    override val description = "Clear scratchpad"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {}
    }

    override suspend fun execute(args: JsonObject): ToolResult {
        memory.remove("_scratchpad")
        return ToolResult.Success("Scratchpad cleared")
    }
}
