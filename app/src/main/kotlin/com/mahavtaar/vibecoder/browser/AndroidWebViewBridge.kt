package com.mahavtaar.vibecoder.browser

import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStreamReader

class AndroidWebViewBridge(private val context: Context, val webView: WebView) : WebViewBridge {

    private val json = Json { ignoreUnknownKeys = true }
    private var agentBridgeScript: String = ""

    init {
        try {
            val inputStream = context.assets.open("agent_bridge.js")
            val reader = InputStreamReader(inputStream)
            agentBridgeScript = reader.readText()
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.193 Mobile Safari/537.36"
        }
    }

    override suspend fun loadUrl(url: String): PageState = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<PageState>()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript(agentBridgeScript, null)
                // Small delay to let JS execution finish and DOM settle
                view?.postDelayed({
                    val stateJob = kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
                        try {
                            deferred.complete(getPageState())
                        } catch (e: Exception) {
                            deferred.completeExceptionally(e)
                        }
                    }
                }, 500)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    deferred.completeExceptionally(Exception("WebView Error: ${error?.description}"))
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                // Cancel by default to prevent Man-In-The-Middle attacks
                handler?.cancel()
            }
        }

        webView.loadUrl(url)
        deferred.await()
    }

    override suspend fun evaluateJs(js: String): String = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<String>()
        webView.evaluateJavascript(js) { result ->
            deferred.complete(result ?: "")
        }
        deferred.await()
    }

    override suspend fun getPageState(): PageState = withContext(Dispatchers.Main) {
        val url = webView.url ?: ""
        val title = webView.title ?: ""

        val rawHtml = evaluateJs("document.documentElement.outerHTML").trim('"').replace("\\n", "\n").replace("\\\"", "\"")
        val visibleText = evaluateJs("window.__agentBridge.getPageText()").trim('"').replace("\\n", "\n").replace("\\\"", "\"")

        val linksJsonString = evaluateJs("JSON.stringify(window.__agentBridge.getLinks())")
        val links = parseLinksFromJson(linksJsonString)

        val inputsJsonString = evaluateJs("JSON.stringify(window.__agentBridge.getInputs())")
        val inputs = parseInputsFromJson(inputsJsonString)

        PageState(
            url = url,
            title = title,
            rawHtml = rawHtml,
            visibleText = visibleText,
            links = links,
            inputs = inputs
        )
    }

    override fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    private fun parseLinksFromJson(jsonString: String): List<PageLink> {
        if (jsonString.isBlank() || jsonString == "null") return emptyList()
        try {
            val unescaped = jsonString.trim('"').replace("\\\"", "\"")
            val jsonArray = json.parseToJsonElement(unescaped).jsonArray
            return jsonArray.map { element ->
                val obj = element.jsonObject
                PageLink(
                    text = obj["text"]?.jsonPrimitive?.content ?: "",
                    href = obj["href"]?.jsonPrimitive?.content ?: "",
                    id = obj["id"]?.jsonPrimitive?.content ?: ""
                )
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun parseInputsFromJson(jsonString: String): List<PageInput> {
        if (jsonString.isBlank() || jsonString == "null") return emptyList()
        try {
            val unescaped = jsonString.trim('"').replace("\\\"", "\"")
            val jsonArray = json.parseToJsonElement(unescaped).jsonArray
            return jsonArray.map { element ->
                val obj = element.jsonObject
                PageInput(
                    type = obj["type"]?.jsonPrimitive?.content ?: "",
                    name = obj["name"]?.jsonPrimitive?.content ?: "",
                    id = obj["id"]?.jsonPrimitive?.content ?: "",
                    placeholder = obj["placeholder"]?.jsonPrimitive?.content ?: "",
                    value = obj["value"]?.jsonPrimitive?.content ?: ""
                )
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }
}
