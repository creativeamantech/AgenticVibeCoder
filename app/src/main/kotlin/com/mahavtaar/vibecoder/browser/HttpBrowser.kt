package com.mahavtaar.vibecoder.browser

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser

class HttpBrowser : WebViewBridge {

    private val cookieStorage = AcceptAllCookiesStorage()

    private val client = HttpClient(Android) {
        install(HttpCookies) {
            storage = cookieStorage
        }
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = 10000
            socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
        }
    }

    private var currentState: PageState? = null

    override suspend fun loadUrl(url: String): PageState = withContext(Dispatchers.IO) {
        val response = client.get(url) {
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.193 Mobile Safari/537.36")
        }

        val rawHtml = response.bodyAsText()
        var title = ""
        val visibleTextBuilder = StringBuilder()
        val links = mutableListOf<PageLink>()
        val inputs = mutableListOf<PageInput>()

        var isTitle = false
        var isScriptOrStyle = false

        val handler = KsoupHtmlHandler
            .Builder()
            .onOpenTag { name, attributes, _ ->
                when (name.lowercase()) {
                    "title" -> isTitle = true
                    "script", "style", "svg" -> isScriptOrStyle = true
                    "a" -> {
                        val href = attributes["href"]
                        if (!href.isNullOrBlank()) {
                            links.add(PageLink(text = "", href = href, id = attributes["id"] ?: ""))
                        }
                    }
                    "input", "textarea", "select" -> {
                        inputs.add(
                            PageInput(
                                type = attributes["type"]?.ifBlank { name } ?: name,
                                name = attributes["name"] ?: "",
                                id = attributes["id"] ?: "",
                                placeholder = attributes["placeholder"] ?: "",
                                value = attributes["value"] ?: ""
                            )
                        )
                    }
                }
            }
            .onText { text ->
                if (isTitle) title += text
                if (!isScriptOrStyle && text.isNotBlank()) {
                    visibleTextBuilder.append(text).append(" ")
                }
            }
            .onCloseTag { name, _ ->
                when (name.lowercase()) {
                    "title" -> isTitle = false
                    "script", "style", "svg" -> isScriptOrStyle = false
                    "a" -> {
                        // Ksoup event-driven model makes extracting inner text of tags slightly complex
                        // For a real implementation, we'd maintain a stack of open elements.
                        // Here, we assign a placeholder or rely on regex/HTML parsing logic if needed.
                    }
                }
            }
            .build()

        val parser = KsoupHtmlParser(handler = handler)
        parser.write(rawHtml)
        parser.end()

        val state = PageState(
            url = response.request.url.toString(),
            title = title.trim(),
            rawHtml = rawHtml,
            visibleText = visibleTextBuilder.toString().replace(Regex("\\s+"), " ").trim(),
            links = links,
            inputs = inputs
        )

        currentState = state
        return@withContext state
    }

    override suspend fun evaluateJs(js: String): String {
        return "HTTP_MODE_NO_JS"
    }

    override suspend fun getPageState(): PageState {
        return currentState ?: PageState("", "", "", "", emptyList(), emptyList())
    }

    override fun clearCookies() {
        // AcceptAllCookiesStorage doesn't provide clear
    }
}
