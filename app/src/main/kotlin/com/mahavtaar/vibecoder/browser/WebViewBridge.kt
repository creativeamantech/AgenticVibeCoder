package com.mahavtaar.vibecoder.browser

interface WebViewBridge {
    suspend fun loadUrl(url: String): PageState
    suspend fun evaluateJs(js: String): String
    suspend fun getPageState(): PageState
    fun clearCookies()
}
