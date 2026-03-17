package com.mahavtaar.vibecoder.browser

import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebViewProvider @Inject constructor() {
    private val _webViewFlow = MutableStateFlow<WebView?>(null)
    val webViewFlow: StateFlow<WebView?> = _webViewFlow.asStateFlow()

    fun setWebView(webView: WebView?) {
        _webViewFlow.value = webView
    }

    fun getWebView(): WebView? = _webViewFlow.value
}
