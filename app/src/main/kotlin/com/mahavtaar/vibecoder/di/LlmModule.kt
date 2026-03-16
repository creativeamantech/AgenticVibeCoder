package com.mahavtaar.vibecoder.di

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mahavtaar.vibecoder.llm.LlamaCppEngine
import com.mahavtaar.vibecoder.llm.LlamaEngine
import com.mahavtaar.vibecoder.llm.ModelDownloader
import com.mahavtaar.vibecoder.llm.OllamaEngine
import com.mahavtaar.vibecoder.ui.models.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LlmModule {

    @Provides
    @Singleton
    fun provideLlamaEngine(@ApplicationContext context: Context): LlamaEngine {
        // Ideally, this should not use runBlocking, but for a simple DataStore read
        // it's acceptable if the value is needed synchronously at app startup.
        // A better approach would be to have a factory that dynamically creates the engine
        // when requested, or initialize the DataStore value beforehand.
        // For Phase 2, we stick to Ollama as default.

        var engineType = "Ollama"
        try {
             engineType = runBlocking {
                val enginePrefKey = stringPreferencesKey("engine_preference")
                context.dataStore.data.first()[enginePrefKey] ?: "Ollama"
            }
        } catch (e: Exception) {
            // Fallback
        }

        return when (engineType) {
            "LlamaCpp" -> LlamaCppEngine()
            // TODO: Phase 2 - Read host and port from DataStore
            "Ollama" -> OllamaEngine(host = "192.168.1.100", port = 11434, modelName = "llama3")
            else -> OllamaEngine(host = "192.168.1.100", port = 11434, modelName = "llama3")
        }
    }

    @Provides
    @Singleton
    fun provideModelDownloader(@ApplicationContext context: Context): ModelDownloader {
        return ModelDownloader(context)
    }
}
