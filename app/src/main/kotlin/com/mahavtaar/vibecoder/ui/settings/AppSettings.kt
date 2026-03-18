package com.mahavtaar.vibecoder.ui.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.appDataStore by preferencesDataStore(name = "vibe_settings")

@Singleton
class AppSettings @Inject constructor(@ApplicationContext private val context: Context) {

    // Keys
    private val LLM_ENGINE = stringPreferencesKey("llm_engine")
    private val OLLAMA_HOST = stringPreferencesKey("ollama_host")
    private val OLLAMA_PORT = intPreferencesKey("ollama_port")
    private val SELECTED_MODEL = stringPreferencesKey("selected_model")
    private val CONTEXT_SIZE = intPreferencesKey("context_size")
    private val TEMPERATURE = floatPreferencesKey("temperature")
    private val MAX_TOKENS = intPreferencesKey("max_tokens")
    private val GPU_LAYERS = intPreferencesKey("gpu_layers")
    private val AGENT_MAX_STEPS = intPreferencesKey("agent_max_steps")
    private val AUTO_CONFIRM_SHELL = booleanPreferencesKey("auto_confirm_shell")
    private val AUTO_CONFIRM_WRITE = booleanPreferencesKey("auto_confirm_write")
    private val DRY_RUN_MODE = booleanPreferencesKey("dry_run_mode")
    private val INLINE_SUGGESTIONS = booleanPreferencesKey("inline_suggestions")
    private val EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size")
    private val AUTO_SAVE = booleanPreferencesKey("auto_save")
    private val PROJECTS_DIR = stringPreferencesKey("projects_dir")
    private val MODELS_DIR = stringPreferencesKey("models_dir")

    // Flows
    val llmEngine: Flow<String> = context.appDataStore.data.map { it[LLM_ENGINE] ?: "ollama" }
    val ollamaHost: Flow<String> = context.appDataStore.data.map { it[OLLAMA_HOST] ?: "192.168.1.100" }
    val ollamaPort: Flow<Int> = context.appDataStore.data.map { it[OLLAMA_PORT] ?: 11434 }
    val selectedModelPath: Flow<String> = context.appDataStore.data.map { it[SELECTED_MODEL] ?: "" }
    val contextWindowSize: Flow<Int> = context.appDataStore.data.map { it[CONTEXT_SIZE] ?: 8192 }
    val temperature: Flow<Float> = context.appDataStore.data.map { it[TEMPERATURE] ?: 0.2f }
    val maxTokens: Flow<Int> = context.appDataStore.data.map { it[MAX_TOKENS] ?: 2048 }
    val gpuLayers: Flow<Int> = context.appDataStore.data.map { it[GPU_LAYERS] ?: 0 }
    val agentMaxSteps: Flow<Int> = context.appDataStore.data.map { it[AGENT_MAX_STEPS] ?: 20 }
    val autoConfirmShell: Flow<Boolean> = context.appDataStore.data.map { it[AUTO_CONFIRM_SHELL] ?: false }
    val autoConfirmWrite: Flow<Boolean> = context.appDataStore.data.map { it[AUTO_CONFIRM_WRITE] ?: true }
    val dryRunMode: Flow<Boolean> = context.appDataStore.data.map { it[DRY_RUN_MODE] ?: false }
    val inlineSuggestionsEnabled: Flow<Boolean> = context.appDataStore.data.map { it[INLINE_SUGGESTIONS] ?: true }
    val editorFontSize: Flow<Int> = context.appDataStore.data.map { it[EDITOR_FONT_SIZE] ?: 14 }
    val autoSaveEnabled: Flow<Boolean> = context.appDataStore.data.map { it[AUTO_SAVE] ?: true }
    val projectsDir: Flow<String> = context.appDataStore.data.map { it[PROJECTS_DIR] ?: "/sdcard/VibeCode/projects/" }
    val modelsDir: Flow<String> = context.appDataStore.data.map { it[MODELS_DIR] ?: "/sdcard/VibeCode/models/" }

    // Setters
    suspend fun setLlmEngine(value: String) { context.appDataStore.edit { it[LLM_ENGINE] = value } }
    suspend fun setOllamaHost(value: String) { context.appDataStore.edit { it[OLLAMA_HOST] = value } }
    suspend fun setOllamaPort(value: Int) { context.appDataStore.edit { it[OLLAMA_PORT] = value } }
    suspend fun setSelectedModelPath(value: String) { context.appDataStore.edit { it[SELECTED_MODEL] = value } }
    suspend fun setContextWindowSize(value: Int) { context.appDataStore.edit { it[CONTEXT_SIZE] = value } }
    suspend fun setTemperature(value: Float) { context.appDataStore.edit { it[TEMPERATURE] = value } }
    suspend fun setMaxTokens(value: Int) { context.appDataStore.edit { it[MAX_TOKENS] = value } }
    suspend fun setGpuLayers(value: Int) { context.appDataStore.edit { it[GPU_LAYERS] = value } }
    suspend fun setAgentMaxSteps(value: Int) { context.appDataStore.edit { it[AGENT_MAX_STEPS] = value } }
    suspend fun setAutoConfirmShell(value: Boolean) { context.appDataStore.edit { it[AUTO_CONFIRM_SHELL] = value } }
    suspend fun setAutoConfirmWrite(value: Boolean) { context.appDataStore.edit { it[AUTO_CONFIRM_WRITE] = value } }
    suspend fun setDryRunMode(value: Boolean) { context.appDataStore.edit { it[DRY_RUN_MODE] = value } }
    suspend fun setInlineSuggestionsEnabled(value: Boolean) { context.appDataStore.edit { it[INLINE_SUGGESTIONS] = value } }
    suspend fun setEditorFontSize(value: Int) { context.appDataStore.edit { it[EDITOR_FONT_SIZE] = value } }
    suspend fun setAutoSaveEnabled(value: Boolean) { context.appDataStore.edit { it[AUTO_SAVE] = value } }
    suspend fun setProjectsDir(value: String) { context.appDataStore.edit { it[PROJECTS_DIR] = value } }
    suspend fun setModelsDir(value: String) { context.appDataStore.edit { it[MODELS_DIR] = value } }
}
