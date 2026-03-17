package com.mahavtaar.vibecoder.editor

import com.mahavtaar.vibecoder.llm.LlamaEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InlineCompletion @Inject constructor(
    private val llamaEngine: LlamaEngine
) {
    private var debounceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    var isEnabled: Boolean = true

    private val _ghostText = MutableStateFlow<String?>(null)
    val ghostText: StateFlow<String?> = _ghostText.asStateFlow()

    fun triggerDebounced(content: String, cursorLine: Int, cursorCol: Int, webViewWrapper: WebViewWrapper) {
        if (!isEnabled || !llamaEngine.isLoaded) return

        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(500) // 500ms debounce

            try {
                // Calculate cursor absolute position
                var absolutePos = 0
                val lines = content.lines()
                for (i in 0 until (cursorLine - 1)) {
                    if (i < lines.size) absolutePos += lines[i].length + 1
                }
                absolutePos += cursorCol - 1

                val prefixStart = maxOf(0, absolutePos - 200)
                val suffixEnd = minOf(content.length, absolutePos + 50)

                val prefix = content.substring(prefixStart, absolutePos)
                val suffix = content.substring(absolutePos, suffixEnd)

                // Fill-in-the-Middle (FIM) prompt structure typical for models like Qwen Coder
                val prompt = "<|fim_prefix|>$prefix<|fim_suffix|>$suffix<|fim_middle|>"

                var suggestion = ""
                llamaEngine.generate(prompt).collect { token ->
                    suggestion += token
                    if (suggestion.length > 80) { // max_tokens approx limit
                        return@collect
                    }
                }

                if (suggestion.isNotBlank()) {
                    _ghostText.value = suggestion
                    webViewWrapper.showGhostText(suggestion, cursorLine)
                }
            } catch (e: Exception) {
                // Ignore silent generation failures for inline completions
            }
        }
    }

    fun accept(webViewWrapper: WebViewWrapper) {
        if (_ghostText.value != null) {
            scope.launch {
                webViewWrapper.acceptGhostText()
                _ghostText.value = null
            }
        }
    }

    fun dismiss(webViewWrapper: WebViewWrapper) {
        if (_ghostText.value != null) {
            scope.launch {
                webViewWrapper.dismissGhostText()
                _ghostText.value = null
            }
        }
    }
}
