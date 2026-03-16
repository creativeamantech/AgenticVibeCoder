package com.mahavtaar.vibecoder.agent.tools

import com.mahavtaar.vibecoder.llm.LlamaEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File

abstract class BaseCodeTool(protected val workingDir: File) : BaseFileTool(workingDir)

class AnalyzeCodeTool(workingDir: File) : BaseCodeTool(workingDir) {
    override val name = "analyze_code"
    override val description = "Analyze code structure"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
            }
            putJsonObject("language") {
                put("type", "string")
                put("description", "Optional language hint (kotlin, python, etc.)")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("path"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val path = args["path"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing path")
        val file = resolvePath(path) ?: return@withContext ToolResult.Error("Access denied or invalid path")

        if (!file.exists() || !file.isFile) return@withContext ToolResult.Error("File not found or not a file")

        try {
            val content = file.readText()
            val lines = content.lines()
            val language = args["language"]?.jsonPrimitive?.content ?: file.extension.lowercase()

            val analysis = StringBuilder()
            analysis.append("File: $path\n")
            analysis.append("Language: $language\n")
            analysis.append("Lines: ${lines.size}\n")
            analysis.append("Top-level entities:\n")

            when (language) {
                "kt", "kotlin" -> {
                    val classRegex = Regex("""^(?:abstract\s+|open\s+|data\s+)?(?:class|interface|object)\s+(\w+)""")
                    val funRegex = Regex("""^fun\s+(\w+)""")
                    lines.forEach { line ->
                        classRegex.find(line.trim())?.let { analysis.append("  - Class/Interface/Object: ${it.groupValues[1]}\n") }
                        funRegex.find(line.trim())?.let { analysis.append("  - Function: ${it.groupValues[1]}\n") }
                    }
                }
                "py", "python" -> {
                    val classRegex = Regex("""^class\s+(\w+)(?:\(.*\))?:""")
                    val funRegex = Regex("""^def\s+(\w+)\s*\(""")
                    lines.forEach { line ->
                        classRegex.find(line)?.let { analysis.append("  - Class: ${it.groupValues[1]}\n") }
                        funRegex.find(line)?.let { analysis.append("  - Function: ${it.groupValues[1]}\n") }
                    }
                }
                else -> analysis.append("  (Syntax analysis not supported for this language)")
            }

            ToolResult.Success(analysis.toString())
        } catch (e: Exception) {
            ToolResult.Error("Failed to analyze code: ${e.message}")
        }
    }
}

class FindErrorsTool(workingDir: File) : BaseCodeTool(workingDir) {
    override val name = "find_errors"
    override val description = "Find syntax/lint errors"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("path"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val path = args["path"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing path")
        val file = resolvePath(path) ?: return@withContext ToolResult.Error("Access denied or invalid path")

        if (!file.exists() || !file.isFile) return@withContext ToolResult.Error("File not found or not a file")

        try {
            val content = file.readText()
            val lines = content.lines()
            val ext = file.extension.lowercase()
            val errors = mutableListOf<String>()

            when (ext) {
                "kt", "kotlin" -> {
                    // Very basic regex-based "linting" for demonstration
                    lines.forEachIndexed { index, line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("var ") && !trimmed.contains("=") && !trimmed.contains(":")) {
                            errors.add("Line ${index + 1}: Uninitialized var lacking explicit type")
                        }
                    }
                    if (content.count { it == '{' } != content.count { it == '}' }) {
                        errors.add("Mismatched braces {}")
                    }
                }
                "py", "python" -> {
                    lines.forEachIndexed { index, line ->
                        if (line.contains("print ") && !line.contains("print(")) {
                            errors.add("Line ${index + 1}: Python 2 print statement found")
                        }
                    }
                }
            }

            if (errors.isEmpty()) {
                ToolResult.Success("No basic errors found")
            } else {
                ToolResult.Success("Errors found:\n" + errors.joinToString("\n"))
            }
        } catch (e: Exception) {
            ToolResult.Error("Failed to check for errors: ${e.message}")
        }
    }
}

class GenerateCodeTool(
    workingDir: File,
    private val llamaEngine: LlamaEngine
) : BaseCodeTool(workingDir) {
    override val name = "generate_code"
    override val description = "Generate code from spec"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("spec") {
                put("type", "string")
            }
            putJsonObject("language") {
                put("type", "string")
            }
            putJsonObject("outputPath") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("spec"), kotlinx.serialization.json.JsonPrimitive("language"), kotlinx.serialization.json.JsonPrimitive("outputPath"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val spec = args["spec"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing spec")
        val language = args["language"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing language")
        val outputPath = args["outputPath"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing outputPath")

        val file = resolvePath(outputPath) ?: return@withContext ToolResult.Error("Access denied or invalid path")

        try {
            // TODO: Phase 4 - Full LLM generation loop
            if (!llamaEngine.isLoaded) {
                 return@withContext ToolResult.Error("LlamaEngine is not loaded. Cannot generate code.")
            }

            val prompt = "Write $language code for the following specification:\n$spec\nOnly output the code, no markdown or explanations."
            val codeBuilder = java.lang.StringBuilder()

            llamaEngine.generate(prompt).collect { token ->
                codeBuilder.append(token)
            }

            val generatedCode = codeBuilder.toString()
            file.parentFile?.mkdirs()
            file.writeText(generatedCode)

            ToolResult.Success("Generated code and saved to $outputPath")
        } catch (e: Exception) {
            ToolResult.Error("Failed to generate code: ${e.message}")
        }
    }
}

class ExplainCodeTool(
    workingDir: File,
    private val llamaEngine: LlamaEngine
) : BaseCodeTool(workingDir) {
    override val name = "explain_code"
    override val description = "Explain code section"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("path"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val path = args["path"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing path")
        val file = resolvePath(path) ?: return@withContext ToolResult.Error("Access denied or invalid path")

        if (!file.exists() || !file.isFile) return@withContext ToolResult.Error("File not found")

        try {
            val content = file.readText()

            // TODO: Phase 4 - Delegate explanation to LLM
            if (!llamaEngine.isLoaded) {
                 return@withContext ToolResult.Error("LlamaEngine is not loaded. Cannot explain code.")
            }

            val prompt = "Explain the following code:\n$content\n"
            val explanationBuilder = java.lang.StringBuilder()

            llamaEngine.generate(prompt).collect { token ->
                explanationBuilder.append(token)
            }

            ToolResult.Success(explanationBuilder.toString())
        } catch (e: Exception) {
            ToolResult.Error("Failed to explain code: ${e.message}")
        }
    }
}
