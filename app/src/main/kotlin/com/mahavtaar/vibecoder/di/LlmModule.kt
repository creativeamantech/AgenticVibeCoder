package com.mahavtaar.vibecoder.di

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mahavtaar.vibecoder.llm.LlamaCppEngine
import com.mahavtaar.vibecoder.llm.LlamaEngine
import com.mahavtaar.vibecoder.llm.ModelDownloader
import com.mahavtaar.vibecoder.llm.OllamaEngine
import com.mahavtaar.vibecoder.ui.settings.appDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LlmModule {

    @OptIn(DelicateCoroutinesApi::class)
    @Provides
    @Singleton
    @Named("inferenceDispatcher")
    fun provideInferenceDispatcher(): CoroutineDispatcher {
        return newSingleThreadContext("llama-inference")
    }

    @Provides
    @Singleton
    fun provideLlamaEngine(
        @ApplicationContext context: Context,
        @Named("inferenceDispatcher") inferenceDispatcher: CoroutineDispatcher
    ): LlamaEngine {
        // We initialize the delegate engine wrapper to safely read the DataStore dynamically
        // at load time, preventing runBlocking ANRs at app startup while fulfilling the dynamic requirement.
        return object : LlamaEngine {
            private var activeEngine: LlamaEngine? = null

            override val isLoaded: Boolean
                get() = activeEngine?.isLoaded ?: false

            override val modelInfo: com.mahavtaar.vibecoder.llm.ModelInfo?
                get() = activeEngine?.modelInfo

            override suspend fun loadModel(modelPath: String, contextSize: Int): Boolean {
                val enginePrefKey = androidx.datastore.preferences.core.stringPreferencesKey("llm_engine")
                val engineType = context.appDataStore.data.first()[enginePrefKey] ?: "ollama"

                activeEngine?.unloadModel() // Unload previous if exists

                activeEngine = when (engineType) {
                    "llama.cpp" -> LlamaCppEngine(inferenceDispatcher)
                    "ollama" -> OllamaEngine(host = "192.168.1.100", port = 11434, modelName = "llama3")
                    else -> OllamaEngine(host = "192.168.1.100", port = 11434, modelName = "llama3")
                }

                return activeEngine?.loadModel(modelPath, contextSize) ?: false
            }

            override fun generate(prompt: String, onToken: (String) -> Unit): kotlinx.coroutines.flow.Flow<String> {
                return activeEngine?.generate(prompt, onToken) ?: kotlinx.coroutines.flow.flow { emit("Error: Engine not loaded") }
            }

            override suspend fun stop() {
                activeEngine?.stop()
            }

            override fun unloadModel() {
                activeEngine?.unloadModel()
                activeEngine = null
            }
        }
    }

    @Provides
    @Singleton
    fun provideModelDownloader(@ApplicationContext context: Context): ModelDownloader {
        return ModelDownloader(context)
    }
}
