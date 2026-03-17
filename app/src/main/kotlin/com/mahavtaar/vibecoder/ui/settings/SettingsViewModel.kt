package com.mahavtaar.vibecoder.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahavtaar.vibecoder.llm.OllamaEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val llmEngine: String = "ollama",
    val ollamaHost: String = "192.168.1.100",
    val ollamaPort: Int = 11434,
    val selectedModelPath: String = "",
    val contextWindowSize: Int = 8192,
    val temperature: Float = 0.2f,
    val maxTokens: Int = 2048,
    val gpuLayers: Int = 0,
    val agentMaxSteps: Int = 20,
    val autoConfirmShell: Boolean = false,
    val autoConfirmWrite: Boolean = true,
    val dryRunMode: Boolean = false,
    val inlineSuggestionsEnabled: Boolean = true,
    val editorFontSize: Int = 14,
    val autoSaveEnabled: Boolean = true,
    val projectsDir: String = "/sdcard/VibeCode/projects/",
    val modelsDir: String = "/sdcard/VibeCode/models/",
    val pingResult: Boolean? = null,
    val isPinging: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appSettings.llmEngine.collect { v -> _uiState.update { it.copy(llmEngine = v) } }
        }
        viewModelScope.launch {
            appSettings.ollamaHost.collect { v -> _uiState.update { it.copy(ollamaHost = v) } }
        }
        viewModelScope.launch {
            appSettings.ollamaPort.collect { v -> _uiState.update { it.copy(ollamaPort = v) } }
        }
        viewModelScope.launch {
            appSettings.selectedModelPath.collect { v -> _uiState.update { it.copy(selectedModelPath = v) } }
        }
        viewModelScope.launch {
            appSettings.contextWindowSize.collect { v -> _uiState.update { it.copy(contextWindowSize = v) } }
        }
        viewModelScope.launch {
            appSettings.temperature.collect { v -> _uiState.update { it.copy(temperature = v) } }
        }
        viewModelScope.launch {
            appSettings.maxTokens.collect { v -> _uiState.update { it.copy(maxTokens = v) } }
        }
        viewModelScope.launch {
            appSettings.gpuLayers.collect { v -> _uiState.update { it.copy(gpuLayers = v) } }
        }
        viewModelScope.launch {
            appSettings.agentMaxSteps.collect { v -> _uiState.update { it.copy(agentMaxSteps = v) } }
        }
        viewModelScope.launch {
            appSettings.autoConfirmShell.collect { v -> _uiState.update { it.copy(autoConfirmShell = v) } }
        }
        viewModelScope.launch {
            appSettings.autoConfirmWrite.collect { v -> _uiState.update { it.copy(autoConfirmWrite = v) } }
        }
        viewModelScope.launch {
            appSettings.dryRunMode.collect { v -> _uiState.update { it.copy(dryRunMode = v) } }
        }
        viewModelScope.launch {
            appSettings.inlineSuggestionsEnabled.collect { v -> _uiState.update { it.copy(inlineSuggestionsEnabled = v) } }
        }
        viewModelScope.launch {
            appSettings.editorFontSize.collect { v -> _uiState.update { it.copy(editorFontSize = v) } }
        }
        viewModelScope.launch {
            appSettings.autoSaveEnabled.collect { v -> _uiState.update { it.copy(autoSaveEnabled = v) } }
        }
        viewModelScope.launch {
            appSettings.projectsDir.collect { v -> _uiState.update { it.copy(projectsDir = v) } }
        }
        viewModelScope.launch {
            appSettings.modelsDir.collect { v -> _uiState.update { it.copy(modelsDir = v) } }
        }
    }

    fun updateLlmEngine(value: String) = viewModelScope.launch { appSettings.setLlmEngine(value) }
    fun updateOllamaHost(value: String) = viewModelScope.launch { appSettings.setOllamaHost(value) }
    fun updateOllamaPort(value: Int) = viewModelScope.launch { appSettings.setOllamaPort(value) }
    fun updateSelectedModelPath(value: String) = viewModelScope.launch { appSettings.setSelectedModelPath(value) }
    fun updateContextWindowSize(value: Int) = viewModelScope.launch { appSettings.setContextWindowSize(value) }
    fun updateTemperature(value: Float) = viewModelScope.launch { appSettings.setTemperature(value) }
    fun updateMaxTokens(value: Int) = viewModelScope.launch { appSettings.setMaxTokens(value) }
    fun updateGpuLayers(value: Int) = viewModelScope.launch { appSettings.setGpuLayers(value) }
    fun updateAgentMaxSteps(value: Int) = viewModelScope.launch { appSettings.setAgentMaxSteps(value) }
    fun updateAutoConfirmShell(value: Boolean) = viewModelScope.launch { appSettings.setAutoConfirmShell(value) }
    fun updateAutoConfirmWrite(value: Boolean) = viewModelScope.launch { appSettings.setAutoConfirmWrite(value) }
    fun updateDryRunMode(value: Boolean) = viewModelScope.launch { appSettings.setDryRunMode(value) }
    fun updateInlineSuggestionsEnabled(value: Boolean) = viewModelScope.launch { appSettings.setInlineSuggestionsEnabled(value) }
    fun updateEditorFontSize(value: Int) = viewModelScope.launch { appSettings.setEditorFontSize(value) }
    fun updateAutoSaveEnabled(value: Boolean) = viewModelScope.launch { appSettings.setAutoSaveEnabled(value) }
    fun updateProjectsDir(value: String) = viewModelScope.launch { appSettings.setProjectsDir(value) }
    fun updateModelsDir(value: String) = viewModelScope.launch { appSettings.setModelsDir(value) }

    fun pingOllama() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPinging = true, pingResult = null) }
            // Quick throwaway instance just for testing connection logic
            val engine = OllamaEngine(host = _uiState.value.ollamaHost, port = _uiState.value.ollamaPort)
            val result = engine.ping()
            _uiState.update { it.copy(isPinging = false, pingResult = result) }
        }
    }
}
