package com.mahavtaar.vibecoder.editor

import android.webkit.JavascriptInterface

interface EditorJsBridge {
    @JavascriptInterface
    fun onContentChanged(content: String)

    @JavascriptInterface
    fun onCursorMoved(line: Int, col: Int)

    @JavascriptInterface
    fun onSaveRequested()

    @JavascriptInterface
    fun onContextMenuAction(action: String, selectedText: String)
}
