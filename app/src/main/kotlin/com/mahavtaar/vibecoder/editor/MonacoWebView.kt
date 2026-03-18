package com.mahavtaar.vibecoder.editor

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MonacoWebView(
    modifier: Modifier = Modifier,
    bridge: EditorJsBridge,
    onWebViewCreated: (WebViewWrapper) -> Unit
) {
    val webViewWrapper = remember { WebViewWrapper() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                }

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        webViewWrapper.isReady = true
                    }
                }

                addJavascriptInterface(bridge, "Android")
                loadUrl("file:///android_asset/monaco/monaco_editor.html")
                webViewWrapper.webView = this
            }
        },
        update = {
            onWebViewCreated(webViewWrapper)
        }
    )
}

class WebViewWrapper {
    var webView: WebView? = null
    var isReady: Boolean = false

    suspend fun setContent(code: String, lang: String) = withContext(Dispatchers.Main) {
        if (!isReady) return@withContext
        val escapedCode = code.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
        webView?.evaluateJavascript("window.setContent(\"$escapedCode\", \"$lang\");", null)
    }

    suspend fun getContent(): String = withContext(Dispatchers.Main) {
        if (!isReady) return@withContext ""
        val deferred = CompletableDeferred<String>()
        webView?.evaluateJavascript("window.getContent();") { result ->
            deferred.complete(result?.trim('"')?.replace("\\n", "\n")?.replace("\\\"", "\"") ?: "")
        }
        deferred.await()
    }

    suspend fun showGhostText(text: String, line: Int) = withContext(Dispatchers.Main) {
        if (!isReady) return@withContext
        val escapedText = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
        webView?.evaluateJavascript("window.showGhostText(\"$escapedText\", $line);", null)
    }

    suspend fun acceptGhostText() = withContext(Dispatchers.Main) {
        if (!isReady) return@withContext
        webView?.evaluateJavascript("window.acceptGhostText();", null)
    }

    suspend fun dismissGhostText() = withContext(Dispatchers.Main) {
        if (!isReady) return@withContext
        webView?.evaluateJavascript("window.dismissGhostText();", null)
    }

    suspend fun applyDiff(old: String, new: String) = withContext(Dispatchers.Main) {
        if (!isReady) return@withContext
        val eOld = old.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
        val eNew = new.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
        webView?.evaluateJavascript("window.applyDiff(\"$eOld\", \"$eNew\");", null)
    }

    suspend fun exitDiff() = withContext(Dispatchers.Main) {
        if (!isReady) return@withContext
        webView?.evaluateJavascript("window.exitDiff();", null)
    }
}
