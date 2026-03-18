package com.mahavtaar.vibecoder.terminal

import android.webkit.JavascriptInterface

interface TerminalJsBridge {
    @JavascriptInterface
    fun onUserInput(key: String)

    @JavascriptInterface
    fun onCommandSubmitted(command: String)
}
