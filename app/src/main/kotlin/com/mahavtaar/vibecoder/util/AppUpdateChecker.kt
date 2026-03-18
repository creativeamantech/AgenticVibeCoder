package com.mahavtaar.vibecoder.util

import com.mahavtaar.vibecoder.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AppUpdateChecker {

    suspend fun checkForUpdates(): String? = withContext(Dispatchers.IO) {
        try {
            val client = HttpClient(Android) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 10000
                    connectTimeoutMillis = 10000
                }
            }

            val response = client.get("https://api.github.com/repos/mahavtaar/vibecoder/releases/latest")
            if (response.status.isSuccess()) {
                val jsonString = response.bodyAsText()
                val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString).jsonObject
                val latestTag = json["tag_name"]?.jsonPrimitive?.content

                if (latestTag != null && latestTag != BuildConfig.VERSION_NAME) {
                    return@withContext latestTag
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
