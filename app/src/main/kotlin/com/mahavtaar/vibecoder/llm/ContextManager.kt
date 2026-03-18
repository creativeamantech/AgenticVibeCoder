package com.mahavtaar.vibecoder.llm

class ContextManager {

    companion object {
        const val AVG_CHARS_PER_TOKEN = 4
    }

    /**
     * Trims a list of strings (e.g., messages) so that the estimated token count
     * fits within [maxTokens]. Removes items from the front (oldest).
     */
    fun trimToFit(messages: List<String>, maxTokens: Int): List<String> {
        val reversed = messages.reversed()
        val kept = mutableListOf<String>()
        var currentTokens = 0

        for (msg in reversed) {
            val estimatedTokens = msg.length / AVG_CHARS_PER_TOKEN
            if (currentTokens + estimatedTokens > maxTokens) {
                break
            }
            kept.add(msg)
            currentTokens += estimatedTokens
        }

        return kept.reversed()
    }
}
