package com.mahavtaar.vibecoder.ui.terminal

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.mahavtaar.vibecoder.terminal.TerminalJsBridge
import com.mahavtaar.vibecoder.terminal.TerminalSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun XtermWebView(
    modifier: Modifier = Modifier,
    bridge: TerminalJsBridge,
    activeSession: TerminalSession?
) {
    val webViewWrapper = remember { TerminalWebViewWrapper() }

    LaunchedEffect(activeSession) {
        if (activeSession != null) {
            // Clear terminal when switching sessions
            webViewWrapper.clear()

            // Subscribe to active session output
            activeSession.output.collect { text ->
                webViewWrapper.writeOutput(text)
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = false
                }

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        webViewWrapper.isReady = true

                        // Initial Prompt
                        if (activeSession != null) {
                            webViewWrapper.launchWritePrompt(activeSession.cwd.value)
                        }
                    }
                }

                addJavascriptInterface(bridge, "Android")
                loadUrl("file:///android_asset/xterm/xterm_terminal.html")
                webViewWrapper.webView = this
            }
        },
        update = {
            if (bridge is TerminalViewModel) {
                bridge.setWebViewWrapper(webViewWrapper)
            }
        }
    )
}

class TerminalWebViewWrapper {
    var webView: WebView? = null
    var isReady: Boolean = false

    suspend fun writeOutput(text: String) = withContext(Dispatchers.Main) {
        if (!isReady) return@withContext
        val escapedText = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
        webView?.evaluateJavascript("window.writeToTerminal(\"$escapedText\");", null)
    }

    suspend fun writePrompt(cwd: String) = withContext(Dispatchers.Main) {
        if (!isReady) return@withContext
        val escapedCwd = cwd.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
        webView?.evaluateJavascript("window.writePrompt(\"$escapedCwd\");", null)
    }

    fun launchWritePrompt(cwd: String) {
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
            writePrompt(cwd)
        }
    }

    suspend fun clear() = withContext(Dispatchers.Main) {
        if (!isReady) return@withContext
        webView?.evaluateJavascript("window.clearTerminal();", null)
    }
}
