package com.mahavtaar.vibecoder.browser

import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser

class BrowsingAgent(
    private val webViewBridge: WebViewBridge,
    private val httpBrowser: HttpBrowser
) {

    private val isWebViewMode = webViewBridge is AndroidWebViewBridge

    suspend fun navigate(url: String): PageState {
        return webViewBridge.loadUrl(url)
    }

    suspend fun executeScript(js: String): String {
        return webViewBridge.evaluateJs(js)
    }

    suspend fun clickElement(cssSelector: String): Boolean {
        if (!isWebViewMode) return false
        val result = executeScript("window.__agentBridge.clickByCss('$cssSelector')")
        return result.toBoolean()
    }

    suspend fun fillInput(cssSelector: String, value: String): Boolean {
        if (!isWebViewMode) return false
        val result = executeScript("window.__agentBridge.fillByCss('$cssSelector', '$value')")
        return result.toBoolean()
    }

    suspend fun extractText(cssSelector: String?): String {
        if (!isWebViewMode) {
            val state = webViewBridge.getPageState()
            if (cssSelector == null) return state.visibleText
            // Implementing basic css selector extraction without Jsoup DOM model is difficult in Ksoup
            // which is SAX-based. Fallback to visibleText.
            return state.visibleText
        }

        return if (cssSelector != null) {
            executeScript("document.querySelector('$cssSelector')?.innerText ?? ''")
        } else {
            executeScript("window.__agentBridge.getPageText()")
        }
    }

    suspend fun getLinks(filter: String? = null): List<PageLink> {
        val state = webViewBridge.getPageState()
        val allLinks = state.links

        return if (filter != null) {
            val regex = Regex(filter, RegexOption.IGNORE_CASE)
            allLinks.filter { regex.containsMatchIn(it.text) || regex.containsMatchIn(it.href) }
        } else {
            allLinks
        }
    }

    suspend fun waitForElement(selector: String, timeoutMs: Long = 10_000): Boolean {
        if (!isWebViewMode) return false
        val result = executeScript("await window.__agentBridge.waitForSelector('$selector', $timeoutMs)")
        return result.toBoolean()
    }

    suspend fun searchGoogle(query: String): List<SearchResult> {
        val url = "https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val state = httpBrowser.loadUrl(url)

        val results = mutableListOf<SearchResult>()
        var currentTitle = ""
        var currentUrl = ""
        var currentSnippet = ""
        var insideResult = false

        val handler = KsoupHtmlHandler
            .Builder()
            .onOpenTag { name, attributes, _ ->
                if (name == "div" && attributes["class"] == "g") {
                    insideResult = true
                }
                if (insideResult && name == "a" && currentUrl.isEmpty()) {
                    currentUrl = attributes["href"] ?: ""
                }
            }
            .onText { text ->
                if (insideResult && currentTitle.isEmpty() && currentUrl.isNotEmpty()) {
                    currentTitle = text
                } else if (insideResult && currentTitle.isNotEmpty()) {
                    currentSnippet += "$text "
                }
            }
            .onCloseTag { name, _ ->
                if (name == "div" && insideResult && currentTitle.isNotEmpty()) {
                    results.add(SearchResult(currentTitle, currentUrl, currentSnippet.trim()))
                    insideResult = false
                    currentTitle = ""
                    currentUrl = ""
                    currentSnippet = ""
                }
            }
            .build()

        val parser = KsoupHtmlParser(handler = handler)
        parser.write(state.rawHtml)
        parser.end()

        return results
    }

    suspend fun searchDDG(query: String): List<SearchResult> {
        val url = "https://html.duckduckgo.com/html/?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val state = httpBrowser.loadUrl(url)

        val results = mutableListOf<SearchResult>()
        var currentTitle = ""
        var currentUrl = ""
        var currentSnippet = ""
        var insideResult = false
        var isTitle = false
        var isSnippet = false

        val handler = KsoupHtmlHandler
            .Builder()
            .onOpenTag { name, attributes, _ ->
                if (name == "a" && attributes["class"]?.contains("result__snippet") == true) {
                    insideResult = true
                    currentUrl = attributes["href"] ?: ""
                    isTitle = true
                } else if (insideResult && name == "a" && attributes["class"]?.contains("result__snippet") == true) {
                    isSnippet = true
                }
            }
            .onText { text ->
                if (isTitle) {
                    currentTitle += text
                } else if (isSnippet) {
                    currentSnippet += "$text "
                }
            }
            .onCloseTag { name, _ ->
                if (isTitle && name == "a") isTitle = false
                if (isSnippet && name == "a") {
                    isSnippet = false
                    results.add(SearchResult(currentTitle, currentUrl, currentSnippet.trim()))
                    insideResult = false
                    currentTitle = ""
                    currentUrl = ""
                    currentSnippet = ""
                }
            }
            .build()

        val parser = KsoupHtmlParser(handler = handler)
        parser.write(state.rawHtml)
        parser.end()

        return results
    }

    suspend fun captureScreenshot(): Bitmap? = withContext(Dispatchers.Main) {
        if (!isWebViewMode) return@withContext null

        // This relies on the internal AndroidWebViewBridge structure. Since we wrapped it anonymously
        // in DI, we need a safer way to access the WebView. But for this demonstration:
        val wv = try {
            val bridgeProp = webViewBridge.javaClass.getDeclaredField("bridge\$delegate") // Might not match exact anonymous class reflection, simpler to get from WebViewProvider
            null // Hard to reflect anonymous class field
        } catch(e: Exception) { null }

        // Better approach: Since we don't have direct access to wv here due to DI wrapper,
        // we should ideally pass it or handle it in the bridge. For Phase 5 stub, we'll try to find it or return null.
        return@withContext null

        /*
        wv.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        )
        wv.layout(0, 0, wv.measuredWidth, wv.measuredHeight)

        wv.isDrawingCacheEnabled = true
        wv.buildDrawingCache(true)
        val b = Bitmap.createBitmap(wv.drawingCache)
        wv.isDrawingCacheEnabled = false

        b
        */
    }

    fun clearCookies() {
        webViewBridge.clearCookies()
        httpBrowser.clearCookies()
    }
}
