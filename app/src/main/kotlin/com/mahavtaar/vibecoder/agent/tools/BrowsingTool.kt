package com.mahavtaar.vibecoder.agent.tools

import android.graphics.Bitmap
import android.util.Base64
import com.mahavtaar.vibecoder.browser.BrowsingAgent
import com.mahavtaar.vibecoder.browser.BrowsingRateLimiter
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

abstract class BaseBrowsingTool(
    protected val browsingAgent: BrowsingAgent,
    protected val rateLimiter: BrowsingRateLimiter
) : AgentTool

class BrowseUrlTool(browsingAgent: BrowsingAgent, rateLimiter: BrowsingRateLimiter) : BaseBrowsingTool(browsingAgent, rateLimiter) {
    override val name = "browse_url"
    override val description = "Open URL, return rendered text + HTML"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("url") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("url"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        rateLimiter.acquire()
        val url = args["url"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing url")
        try {
            val state = browsingAgent.navigate(url)
            val output = "URL: ${state.url}\nTitle: ${state.title}\nContent snippet (first 1000 chars):\n${state.visibleText.take(1000)}"
            ToolResult.Success(output)
        } catch (e: Exception) {
            ToolResult.Error("Failed to browse URL: ${e.message}")
        }
    }
}

class ClickElementTool(browsingAgent: BrowsingAgent, rateLimiter: BrowsingRateLimiter) : BaseBrowsingTool(browsingAgent, rateLimiter) {
    override val name = "click_element"
    override val description = "Click DOM element by CSS selector"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("selector") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("selector"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        rateLimiter.acquire()
        val selector = args["selector"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing selector")
        try {
            val success = browsingAgent.clickElement(selector)
            if (success) ToolResult.Success("Clicked element '$selector'")
            else ToolResult.Error("Element '$selector' not found or could not be clicked")
        } catch (e: Exception) {
            ToolResult.Error("Failed to click element: ${e.message}")
        }
    }
}

class FillInputTool(browsingAgent: BrowsingAgent, rateLimiter: BrowsingRateLimiter) : BaseBrowsingTool(browsingAgent, rateLimiter) {
    override val name = "fill_input"
    override val description = "Fill form input"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("selector") {
                put("type", "string")
            }
            putJsonObject("value") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("selector"), kotlinx.serialization.json.JsonPrimitive("value"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        rateLimiter.acquire()
        val selector = args["selector"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing selector")
        val value = args["value"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing value")
        try {
            val success = browsingAgent.fillInput(selector, value)
            if (success) ToolResult.Success("Filled input '$selector' with '$value'")
            else ToolResult.Error("Input '$selector' not found or could not be filled")
        } catch (e: Exception) {
            ToolResult.Error("Failed to fill input: ${e.message}")
        }
    }
}

class ExtractTextTool(browsingAgent: BrowsingAgent, rateLimiter: BrowsingRateLimiter) : BaseBrowsingTool(browsingAgent, rateLimiter) {
    override val name = "extract_text"
    override val description = "Extract visible page text"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("selector") {
                put("type", "string")
                put("description", "Optional CSS selector to extract specific text")
            }
        }
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        rateLimiter.acquire()
        val selector = args["selector"]?.jsonPrimitive?.content
        try {
            val text = browsingAgent.extractText(selector)
            ToolResult.Success(text)
        } catch (e: Exception) {
            ToolResult.Error("Failed to extract text: ${e.message}")
        }
    }
}

class TakeScreenshotTool(browsingAgent: BrowsingAgent, rateLimiter: BrowsingRateLimiter) : BaseBrowsingTool(browsingAgent, rateLimiter) {
    override val name = "take_screenshot"
    override val description = "Capture current page as base64 PNG"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {}
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        rateLimiter.acquire()
        try {
            val bitmap = browsingAgent.captureScreenshot() ?: return@withContext ToolResult.Error("Screenshot capture not available in this mode")

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.DEFAULT)

            ToolResult.Success(base64)
        } catch (e: Exception) {
            ToolResult.Error("Failed to capture screenshot: ${e.message}")
        }
    }
}

class ScrollPageTool(browsingAgent: BrowsingAgent, rateLimiter: BrowsingRateLimiter) : BaseBrowsingTool(browsingAgent, rateLimiter) {
    override val name = "scroll_page"
    override val description = "Scroll up/down/left/right"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("direction") {
                put("type", "string")
            }
            putJsonObject("amount") {
                put("type", "integer")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("direction"), kotlinx.serialization.json.JsonPrimitive("amount"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        rateLimiter.acquire()
        val direction = args["direction"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing direction")
        val amount = args["amount"]?.jsonPrimitive?.content?.toIntOrNull() ?: return@withContext ToolResult.Error("Missing amount")

        try {
            val script = when (direction.lowercase()) {
                "up" -> "window.scrollBy(0, -$amount)"
                "down" -> "window.scrollBy(0, $amount)"
                "left" -> "window.scrollBy(-$amount, 0)"
                "right" -> "window.scrollBy($amount, 0)"
                else -> return@withContext ToolResult.Error("Invalid direction. Use up, down, left, right")
            }
            browsingAgent.executeScript(script)
            ToolResult.Success("Scrolled $direction by $amount pixels")
        } catch (e: Exception) {
            ToolResult.Error("Failed to scroll: ${e.message}")
        }
    }
}

class WaitForElementTool(browsingAgent: BrowsingAgent, rateLimiter: BrowsingRateLimiter) : BaseBrowsingTool(browsingAgent, rateLimiter) {
    override val name = "wait_for_element"
    override val description = "Wait for DOM element"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("selector") {
                put("type", "string")
            }
            putJsonObject("timeout") {
                put("type", "integer")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("selector"), kotlinx.serialization.json.JsonPrimitive("timeout"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        rateLimiter.acquire()
        val selector = args["selector"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing selector")
        val timeout = args["timeout"]?.jsonPrimitive?.content?.toLongOrNull() ?: 10000L
        try {
            val found = browsingAgent.waitForElement(selector, timeout)
            if (found) ToolResult.Success("Element '$selector' found")
            else ToolResult.Error("Element '$selector' not found after $timeout ms")
        } catch (e: Exception) {
            ToolResult.Error("Failed to wait for element: ${e.message}")
        }
    }
}

class GetPageLinksTool(browsingAgent: BrowsingAgent, rateLimiter: BrowsingRateLimiter) : BaseBrowsingTool(browsingAgent, rateLimiter) {
    override val name = "get_page_links"
    override val description = "Get all links on page"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("filter") {
                put("type", "string")
                put("description", "Optional regex filter")
            }
        }
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        rateLimiter.acquire()
        val filter = args["filter"]?.jsonPrimitive?.content
        try {
            val links = browsingAgent.getLinks(filter)
            val output = links.joinToString("\n") { "- [${it.text}](${it.href})" }
            ToolResult.Success(if (output.isEmpty()) "No links found" else output)
        } catch (e: Exception) {
            ToolResult.Error("Failed to get links: ${e.message}")
        }
    }
}

class WebSearchTool(browsingAgent: BrowsingAgent, rateLimiter: BrowsingRateLimiter) : BaseBrowsingTool(browsingAgent, rateLimiter) {
    override val name = "web_search"
    override val description = "Search and return results"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("query") {
                put("type", "string")
            }
            putJsonObject("engine") {
                put("type", "string")
                put("description", "Search engine to use (google or ddg), defaults to ddg")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("query"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        rateLimiter.acquire()
        val query = args["query"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing query")
        val engine = args["engine"]?.jsonPrimitive?.content ?: "ddg"

        try {
            val results = if (engine.lowercase() == "google") {
                browsingAgent.searchGoogle(query)
            } else {
                browsingAgent.searchDDG(query)
            }

            if (results.isEmpty()) {
                ToolResult.Success("No search results found.")
            } else {
                val output = results.joinToString("\n\n") { "Title: ${it.title}\nURL: ${it.url}\nSnippet: ${it.snippet}" }
                ToolResult.Success(output)
            }
        } catch (e: Exception) {
            ToolResult.Error("Failed to search web: ${e.message}")
        }
    }
}

class DownloadFileTool(
    browsingAgent: BrowsingAgent,
    rateLimiter: BrowsingRateLimiter,
    private val workingDir: File
) : BaseBrowsingTool(browsingAgent, rateLimiter) {
    override val name = "download_file"
    override val description = "Download file from URL"
    override val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("url") {
                put("type", "string")
            }
            putJsonObject("savePath") {
                put("type", "string")
            }
        }
        put("required", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("url"), kotlinx.serialization.json.JsonPrimitive("savePath"))))
    }

    override suspend fun execute(args: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        rateLimiter.acquire()
        val url = args["url"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing url")
        val savePathStr = args["savePath"]?.jsonPrimitive?.content ?: return@withContext ToolResult.Error("Missing savePath")

        val targetFile = File(workingDir, savePathStr)
        // Prevent sandbox escaping
        if (!targetFile.canonicalPath.startsWith(workingDir.canonicalPath)) {
            return@withContext ToolResult.Error("Invalid savePath, cannot escape sandbox")
        }

        try {
            val client = HttpClient(Android) {
                install(HttpTimeout) {
                    requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
                    connectTimeoutMillis = 10000
                    socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
                }
            }

            val response = client.get(url)
            if (!response.status.isSuccess()) {
                return@withContext ToolResult.Error("Download failed with status: ${response.status}")
            }

            val channel: ByteReadChannel = response.bodyAsChannel()
            targetFile.parentFile?.mkdirs()

            FileOutputStream(targetFile).use { output ->
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(8192)
                    while (!packet.isEmpty) {
                        output.write(packet.readBytes())
                    }
                }
            }

            client.close()
            ToolResult.Success("File downloaded to $savePathStr")
        } catch (e: Exception) {
            ToolResult.Error("Failed to download file: ${e.message}")
        }
    }
}
