package com.mahavtaar.vibecoder.ui.models

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.vibecoder.llm.LlamaEngine
import com.mahavtaar.vibecoder.llm.ModelDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

val Context.dataStore by preferencesDataStore(name = "settings")

data class ModelManagerState(
    val isModelLoaded: Boolean = false,
    val currentModelName: String? = null,
    val downloadProgress: Float = 0f,
    val isDownloading: Boolean = false,
    val availableModels: List<String> = listOf("Qwen2.5-Coder-7B", "DeepSeek-Coder-V2-Lite", "Phi-4-mini", "Llama-3.2-3B"),
    val currentEngine: String = "Ollama"
)

@HiltViewModel
class ModelManagerViewModel @Inject constructor(
    private val llamaEngine: LlamaEngine,
    private val modelDownloader: ModelDownloader,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelManagerState())
    val uiState: StateFlow<ModelManagerState> = _uiState.asStateFlow()

    private val enginePrefKey = stringPreferencesKey("engine_preference")

    init {
        viewModelScope.launch {
            val engine = context.dataStore.data.first()[enginePrefKey] ?: "Ollama"
            _uiState.update { it.copy(currentEngine = engine) }
        }
    }

    fun setEngine(engine: String) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[enginePrefKey] = engine
            }
            _uiState.update { it.copy(currentEngine = engine) }
            // Note: In a real app, changing the engine might require re-initializing the LlamaEngine instance
            // or restarting the app since it's injected as a Singleton.
        }
    }

    fun loadModel(modelPath: String) {
        viewModelScope.launch {
            val success = llamaEngine.loadModel(modelPath)
            if (success) {
                _uiState.update {
                    it.copy(isModelLoaded = true, currentModelName = modelPath)
                }
            }
        }
    }

    fun unloadModel() {
        llamaEngine.unloadModel()
        _uiState.update {
            it.copy(isModelLoaded = false, currentModelName = null)
        }
    }

    fun downloadModel(url: String, fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadProgress = 0f) }
            try {
                modelDownloader.downloadModel(url, fileName).collect { progress ->
                    _uiState.update { it.copy(downloadProgress = progress.percent / 100f) }
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _uiState.update { it.copy(isDownloading = false, downloadProgress = 0f) }
            }
        }
    }
}
