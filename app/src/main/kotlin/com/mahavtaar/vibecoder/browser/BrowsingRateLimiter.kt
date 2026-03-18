package com.mahavtaar.vibecoder.browser

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BrowsingRateLimiter(private val maxRequestsPerMinute: Int = 20) {

    private val mutex = Mutex()
    private val timestamps = mutableListOf<Long>()

    suspend fun acquire() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val windowStart = now - 60_000

            // Remove timestamps older than 1 minute
            timestamps.removeAll { it < windowStart }

            if (timestamps.size >= maxRequestsPerMinute) {
                // We've hit the limit. Wait until the oldest token falls out of the window.
                val oldest = timestamps.first()
                val waitTime = oldest + 60_000 - now
                if (waitTime > 0) {
                    delay(waitTime)
                }

                // Remove the oldest token since we just waited for it to expire
                timestamps.removeAt(0)
            }

            // Add new token timestamp
            timestamps.add(System.currentTimeMillis())
        }
    }
}
