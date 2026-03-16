package com.mahavtaar.vibecoder.agent.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.io.InputStreamReader

abstract class BaseShellTool(protected val workingDir: File) : AgentTool {

    protected fun sanitizeCommand(cmd: String): Boolean {
        val dangerousPatterns = listOf(";", "&&", "||", "rm -rf /")
        for (pattern in dangerousPatterns) {
            if (cmd.contains(pattern)) return false
        }
        return true
    }

    protected suspend fun executeCommand(cmd: List<String>, cwd: File?, timeoutMs: Long): ToolResult = withContext(Dispatchers.IO) {
        if (!sanitizeCommand(cmd.joinToString(" "))) {
            return@withContext ToolResult.Error("Command contains dangerous or prohibited characters")
        }

        val dir = cwd ?: workingDir
        if (!dir.exists() || !dir.isDirectory) {
            return@withContext ToolResult.Error("Invalid working directory: ${dir.path}")
        }

        try {
            val process = ProcessBuilder(cmd)
                .directory(dir)
                .redirectErrorStream(true)
                .start()

            val output = StringBuilder()
            val reader = InputStreamReader(process.inputStream)

            val completed = withTimeoutOrNull(timeoutMs) {
                var char = reader.read()
                while (char != -1) {
                    output.append(char.toChar())
                    char = reader.read()
                }
                process.waitFor()
            }

            if (completed == null) {
                process.destroyForcibly()
                return@withContext ToolResult.Error("Command execution timed out after $timeoutMs ms")
            }

            var resultStr = output.toString()
            if (resultStr.length > 2000) {
                resultStr = "... [truncated output] ...\n" + resultStr.takeLast(2000)
            }

            ToolResult.Success(resultStr)
        } catch (e: Exception) {
            ToolResult.Error("Failed to execute command: ${e.message}")
        }
    }
}

class RunShellTool(workingDir: File) : BaseShellTool(workingDir) {
    override val name = "run_shell"
    override val description = "Execute shell command"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("command") {
                put("type", "string")
            }
            putJsonObject("cwd") {
                put("type", "string")
                put("description", "Optional working directory relative to project root")
            }
            putJsonObject("timeout") {
                put("type", "integer")
                put("description", "Timeout in milliseconds")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("command"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val command = args["command"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing command")
        val cwdStr = args["cwd"]?.jsonPrimitive?.content
        val timeout = args["timeout"]?.jsonPrimitive?.content?.toLongOrNull() ?: 10000L

        val cwd = if (cwdStr != null) {
             val file = File(cwdStr)
             val resolvedFile = if (file.isAbsolute) file else File(workingDir, cwdStr)
             val canonicalPath = resolvedFile.canonicalPath
             if (canonicalPath.startsWith(workingDir.canonicalPath)) File(canonicalPath) else null
        } else workingDir

        if (cwd == null) return@withContext ToolResult.Error("Invalid working directory path: prevents escaping sandbox")

        // Use a standard shell wrapper for basic commands. Note: On Android shell might be limited to sh or requires Termux root
        val shellCmd = listOf("sh", "-c", command)
        executeCommand(shellCmd, cwd, timeout)
    }
}

class RunGradleTool(workingDir: File) : BaseShellTool(workingDir) {
    override val name = "run_gradle"
    override val description = "Run Gradle task"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("task") {
                put("type", "string")
            }
            putJsonObject("projectPath") {
                put("type", "string")
                put("description", "Path to project root (must contain gradlew)")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("task"), kotlinx.serialization.json.JsonPrimitive("projectPath"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val task = args["task"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing gradle task")
        val projectPath = args["projectPath"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing project path")

        val dir = File(workingDir, projectPath)
        if (!dir.exists() || !dir.isDirectory) return@withContext ToolResult.Error("Invalid project directory: $projectPath")

        val gradlew = File(dir, "gradlew")
        if (!gradlew.exists()) return@withContext ToolResult.Error("gradlew not found in $projectPath")

        val cmd = listOf("./gradlew", task)
        executeCommand(cmd, dir, 30000L) // 30s timeout
    }
}

class RunPythonTool(workingDir: File) : BaseShellTool(workingDir) {
    override val name = "run_python"
    override val description = "Run Python script via Termux"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("script") {
                put("type", "string")
                put("description", "The python script to execute")
            }
            putJsonObject("args") {
                put("type", "array")
                putJsonObject("items") {
                    put("type", "string")
                }
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("script"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val script = args["script"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing python script")
        val pArgs = args["args"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

        val cmd = mutableListOf("python", "-c", script)
        cmd.addAll(pArgs)

        executeCommand(cmd, workingDir, 10000L)
    }
}

class GetEnvTool(workingDir: File) : BaseShellTool(workingDir) {
    override val name = "get_env"
    override val description = "Get environment variable"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("variable") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("variable"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val variable = args["variable"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing variable name")
        val value = System.getenv(variable)

        if (value != null) {
            ToolResult.Success(value)
        } else {
            ToolResult.Error("Environment variable $variable not found")
        }
    }
}
