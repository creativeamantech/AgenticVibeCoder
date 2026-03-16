package com.mahavtaar.vibecoder.agent.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File

abstract class BaseFileTool(protected val workingDir: File) : AgentTool {

    protected fun resolvePath(path: String): File? {
        val file = File(path)
        val resolvedFile = if (file.isAbsolute) file else File(workingDir, path)
        val canonicalPath = resolvedFile.canonicalPath
        return if (canonicalPath.startsWith(workingDir.canonicalPath)) {
            File(canonicalPath)
        } else {
            null // Prevent escaping the sandbox
        }
    }
}

class ReadFileTool(workingDir: File) : BaseFileTool(workingDir) {
    override val name = "read_file"
    override val description = "Read entire file content"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
                put("description", "Path to the file")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("path"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val path = args["path"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing path")
        val file = resolvePath(path) ?: return@withContext ToolResult.Error("Access denied or invalid path: $path")

        if (file.exists() && file.isFile) {
            try {
                ToolResult.Success(file.readText())
            } catch (e: Exception) {
                ToolResult.Error("Failed to read file: ${e.message}")
            }
        } else {
            ToolResult.Error("File not found: $path")
        }
    }
}

class WriteFileTool(workingDir: File) : BaseFileTool(workingDir) {
    override val name = "write_file"
    override val description = "Write/overwrite file"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
            }
            putJsonObject("content") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("path"), kotlinx.serialization.json.JsonPrimitive("content"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val path = args["path"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing path")
        val content = args["content"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing content")
        val file = resolvePath(path) ?: return@withContext ToolResult.Error("Access denied or invalid path")

        try {
            file.parentFile?.mkdirs()
            file.writeText(content)
            ToolResult.Success("File written successfully at $path")
        } catch (e: Exception) {
            ToolResult.Error("Failed to write file: ${e.message}")
        }
    }
}

class AppendFileTool(workingDir: File) : BaseFileTool(workingDir) {
    override val name = "append_file"
    override val description = "Append content to an existing file"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
            }
            putJsonObject("content") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("path"), kotlinx.serialization.json.JsonPrimitive("content"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val path = args["path"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing path")
        val content = args["content"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing content")
        val file = resolvePath(path) ?: return@withContext ToolResult.Error("Access denied or invalid path")

        if (file.exists() && file.isFile) {
            try {
                file.appendText(content)
                ToolResult.Success("Content appended to $path")
            } catch (e: Exception) {
                ToolResult.Error("Failed to append file: ${e.message}")
            }
        } else {
            ToolResult.Error("File not found or not a file: $path")
        }
    }
}

class ListDirTool(workingDir: File) : BaseFileTool(workingDir) {
    override val name = "list_dir"
    override val description = "List directory contents"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
            }
            putJsonObject("recursive") {
                put("type", "boolean")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("path"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val path = args["path"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing path")
        val recursive = args["recursive"]?.jsonPrimitive?.content?.toBoolean() ?: false
        val dir = resolvePath(path) ?: return@withContext ToolResult.Error("Access denied or invalid path")

        if (dir.exists() && dir.isDirectory) {
            try {
                val files = if (recursive) {
                    dir.walkTopDown().map { it.relativeTo(workingDir).path }.toList()
                } else {
                    dir.listFiles()?.map { it.relativeTo(workingDir).path }?.toList() ?: emptyList()
                }
                ToolResult.Success(files.joinToString("\n"))
            } catch (e: Exception) {
                ToolResult.Error("Failed to list directory: ${e.message}")
            }
        } else {
            ToolResult.Error("Directory not found: $path")
        }
    }
}

class CreateDirTool(workingDir: File) : BaseFileTool(workingDir) {
    override val name = "create_dir"
    override val description = "Create directory"
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
        val dir = resolvePath(path) ?: return@withContext ToolResult.Error("Access denied or invalid path")

        if (dir.mkdirs()) {
            ToolResult.Success("Directory created at $path")
        } else if (dir.exists() && dir.isDirectory) {
             ToolResult.Success("Directory already exists at $path")
        } else {
            ToolResult.Error("Failed to create directory at $path")
        }
    }
}

class DeleteFileTool(workingDir: File) : BaseFileTool(workingDir) {
    override val name = "delete_file"
    override val description = "Delete file or empty directory"
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

        if (file.exists()) {
             if (file.deleteRecursively()) {
                 ToolResult.Success("Deleted $path")
             } else {
                 ToolResult.Error("Failed to delete $path")
             }
        } else {
             ToolResult.Error("File or directory not found: $path")
        }
    }
}

class MoveFileTool(workingDir: File) : BaseFileTool(workingDir) {
    override val name = "move_file"
    override val description = "Move/rename file"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("from") {
                put("type", "string")
            }
            putJsonObject("to") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("from"), kotlinx.serialization.json.JsonPrimitive("to"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val fromPath = args["from"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing from path")
        val toPath = args["to"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing to path")

        val source = resolvePath(fromPath) ?: return@withContext ToolResult.Error("Access denied to source path")
        val dest = resolvePath(toPath) ?: return@withContext ToolResult.Error("Access denied to destination path")

        if (!source.exists()) return@withContext ToolResult.Error("Source not found: $fromPath")

        dest.parentFile?.mkdirs()
        if (source.renameTo(dest)) {
            ToolResult.Success("Moved $fromPath to $toPath")
        } else {
            ToolResult.Error("Failed to move file")
        }
    }
}

class SearchInFilesTool(workingDir: File) : BaseFileTool(workingDir) {
    override val name = "search_in_files"
    override val description = "Grep-like search within directory"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
            }
            putJsonObject("query") {
                put("type", "string")
            }
            putJsonObject("ext") {
                put("type", "string")
                put("description", "Optional file extension filter")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("path"), kotlinx.serialization.json.JsonPrimitive("query"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val path = args["path"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing path")
        val query = args["query"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing query")
        val ext = args["ext"]?.jsonPrimitive?.content

        val dir = resolvePath(path) ?: return@withContext ToolResult.Error("Access denied or invalid path")

        if (dir.exists() && dir.isDirectory) {
            try {
                val results = mutableListOf<String>()
                dir.walkTopDown().forEach { file ->
                    if (file.isFile && (ext == null || file.extension == ext)) {
                        val lines = file.readLines()
                        lines.forEachIndexed { index, line ->
                            if (line.contains(query, ignoreCase = true)) {
                                results.add("${file.relativeTo(workingDir).path}:${index + 1}: $line")
                            }
                        }
                    }
                }
                ToolResult.Success(if (results.isEmpty()) "No matches found" else results.joinToString("\n"))
            } catch (e: Exception) {
                ToolResult.Error("Failed to search: ${e.message}")
            }
        } else {
            ToolResult.Error("Directory not found or not a directory: $path")
        }
    }
}

class PatchFileTool(workingDir: File) : BaseFileTool(workingDir) {
    override val name = "patch_file"
    override val description = "Find-and-replace in file"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("path") {
                put("type", "string")
            }
            putJsonObject("old") {
                put("type", "string")
            }
            putJsonObject("new") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("path"), kotlinx.serialization.json.JsonPrimitive("old"), kotlinx.serialization.json.JsonPrimitive("new"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val path = args["path"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing path")
        val oldStr = args["old"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing old string")
        val newStr = args["new"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing new string")

        val file = resolvePath(path) ?: return@withContext ToolResult.Error("Access denied or invalid path")

        if (file.exists() && file.isFile) {
             try {
                 val content = file.readText()
                 if (!content.contains(oldStr)) {
                     return@withContext ToolResult.Error("Target string not found in file")
                 }
                 val patched = content.replace(oldStr, newStr)
                 file.writeText(patched)
                 ToolResult.Success("File patched successfully at $path")
             } catch (e: Exception) {
                 ToolResult.Error("Failed to patch file: ${e.message}")
             }
        } else {
            ToolResult.Error("File not found: $path")
        }
    }
}
