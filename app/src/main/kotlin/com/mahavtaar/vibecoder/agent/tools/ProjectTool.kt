package com.mahavtaar.vibecoder.agent.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File

abstract class BaseProjectTool(workingDir: File) : BaseShellTool(workingDir)

class ScaffoldProjectTool(workingDir: File) : BaseProjectTool(workingDir) {
    override val name = "scaffold_project"
    override val description = "Create project skeleton"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("type") {
                put("type", "string")
            }
            putJsonObject("name") {
                put("type", "string")
            }
            putJsonObject("path") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("type"), kotlinx.serialization.json.JsonPrimitive("name"), kotlinx.serialization.json.JsonPrimitive("path"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val type = args["type"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing type")
        val name = args["name"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing name")
        val pathStr = args["path"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing path")

        val targetDir = File(workingDir, pathStr)
        if (targetDir.exists() && targetDir.listFiles()?.isNotEmpty() == true) {
            return@withContext ToolResult.Error("Target directory $pathStr is not empty")
        }

        try {
            targetDir.mkdirs()

            when (type.lowercase()) {
                "android" -> {
                    // Very basic scaffolding
                    File(targetDir, "app/src/main/java").mkdirs()
                    File(targetDir, "app/src/main/res/layout").mkdirs()
                    File(targetDir, "build.gradle.kts").writeText("// Root build script for $name")
                    File(targetDir, "app/build.gradle.kts").writeText("// App build script for $name")
                    File(targetDir, "settings.gradle.kts").writeText("rootProject.name = \"$name\"\ninclude(\":app\")")
                    ToolResult.Success("Android project scaffolded at $pathStr")
                }
                "python" -> {
                    File(targetDir, "src").mkdirs()
                    File(targetDir, "tests").mkdirs()
                    File(targetDir, "requirements.txt").writeText("")
                    File(targetDir, "src/main.py").writeText("def main():\n    print(\"Hello $name\")\n\nif __name__ == '__main__':\n    main()")
                    ToolResult.Success("Python project scaffolded at $pathStr")
                }
                else -> ToolResult.Error("Unsupported project type: $type")
            }
        } catch (e: Exception) {
            ToolResult.Error("Failed to scaffold project: ${e.message}")
        }
    }
}

class GitCommandTool(workingDir: File) : BaseProjectTool(workingDir) {
    override val name = "git_command"
    override val description = "Run git command"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("args") {
                put("type", "string")
                put("description", "Arguments passed to git executable")
            }
            putJsonObject("repoPath") {
                put("type", "string")
                put("description", "Path to the repository relative to working directory")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("args"), kotlinx.serialization.json.JsonPrimitive("repoPath"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val gitArgs = args["args"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing args")
        val repoPathStr = args["repoPath"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing repoPath")

        val repoDir = File(workingDir, repoPathStr)
        if (!repoDir.exists() || !repoDir.isDirectory) {
            return@withContext ToolResult.Error("Repository path not found: $repoPathStr")
        }

        // Basic sanity check to prevent command injection
        if (!sanitizeCommand(gitArgs)) {
            return@withContext ToolResult.Error("Unsafe arguments in git command")
        }

        val cmdParts = mutableListOf("git")
        // Poor man's argument parser (splits by space, doesn't handle quotes properly, sufficient for basic agent tasks)
        cmdParts.addAll(gitArgs.split(Regex("\\s+")).filter { it.isNotBlank() })

        executeCommand(cmdParts, repoDir, 20000L) // 20s timeout
    }
}

class ReadGradleTool(workingDir: File) : BaseProjectTool(workingDir) {
    override val name = "read_gradle"
    override val description = "Parse build.gradle.kts"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("projectPath") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("projectPath"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val projectPath = args["projectPath"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing project path")
        val dir = File(workingDir, projectPath)

        val buildFile = File(dir, "build.gradle.kts")
        if (!buildFile.exists()) {
             // Fallback to Groovy
             val groovyBuildFile = File(dir, "build.gradle")
             if (groovyBuildFile.exists()) {
                 return@withContext ToolResult.Success(groovyBuildFile.readText())
             }
             return@withContext ToolResult.Error("No build.gradle or build.gradle.kts found in $projectPath")
        }

        ToolResult.Success(buildFile.readText())
    }
}

class AddDependencyTool(workingDir: File) : BaseProjectTool(workingDir) {
    override val name = "add_dependency"
    override val description = "Add Gradle dependency"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("projectPath") {
                put("type", "string")
            }
            putJsonObject("dep") {
                put("type", "string")
                put("description", "Dependency string (e.g. implementation(\"group:artifact:version\"))")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("projectPath"), kotlinx.serialization.json.JsonPrimitive("dep"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val projectPath = args["projectPath"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing project path")
        val dep = args["dep"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing dependency string")

        val dir = File(workingDir, projectPath)
        val buildFile = File(dir, "build.gradle.kts")

        if (!buildFile.exists() || !buildFile.isFile) return@withContext ToolResult.Error("build.gradle.kts not found")

        try {
            val content = buildFile.readLines().toMutableList()
            var depsBlockIndex = -1
            var insertIndex = -1

            // Find the dependencies block
            for (i in content.indices) {
                if (content[i].trim().startsWith("dependencies {")) {
                    depsBlockIndex = i
                    break
                }
            }

            if (depsBlockIndex == -1) {
                content.add("\ndependencies {")
                content.add("    $dep")
                content.add("}")
            } else {
                // Find closing brace
                var openBraces = 0
                for (i in depsBlockIndex until content.size) {
                    openBraces += content[i].count { it == '{' }
                    openBraces -= content[i].count { it == '}' }
                    if (openBraces == 0) {
                        insertIndex = i
                        break
                    }
                }
                if (insertIndex != -1) {
                    content.add(insertIndex, "    $dep")
                } else {
                    return@withContext ToolResult.Error("Malformed dependencies block in build.gradle.kts")
                }
            }

            buildFile.writeText(content.joinToString("\n"))
            ToolResult.Success("Dependency added successfully to $projectPath")
        } catch (e: Exception) {
            ToolResult.Error("Failed to add dependency: ${e.message}")
        }
    }
}
