package com.mahavtaar.vibecoder.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahavtaar.vibecoder.ui.theme.AccentBlue
import com.mahavtaar.vibecoder.ui.theme.DarkBackground
import com.mahavtaar.vibecoder.ui.theme.DarkSurface
import com.mahavtaar.vibecoder.ui.theme.ErrorRed
import com.mahavtaar.vibecoder.ui.theme.SuccessGreen
import com.mahavtaar.vibecoder.ui.theme.TextPrimary
import com.mahavtaar.vibecoder.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("⚙ SETTINGS", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)

        // Section: LLM Engine
        SettingsSection(title = "LLM ENGINE") {
            // Engine selection (Simplified dropdown using Row of RadioButtons for brevity, standard compose Dropdown is complex to style cleanly in stubs)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Engine:", color = TextPrimary, modifier = Modifier.weight(1f))
                listOf("ollama", "llama.cpp", "mlc").forEach { engine ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = state.llmEngine == engine,
                            onClick = { viewModel.updateLlmEngine(engine) },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentBlue, unselectedColor = TextSecondary)
                        )
                        Text(engine, color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }

            SettingsSlider("Context Window", state.contextWindowSize.toFloat(), 1024f, 32768f) { viewModel.updateContextWindowSize(it.toInt()) }
            SettingsSlider("Temperature", state.temperature, 0f, 1f) { viewModel.updateTemperature(it) }
            SettingsSlider("Max Tokens per Response", state.maxTokens.toFloat(), 256f, 8192f) { viewModel.updateMaxTokens(it.toInt()) }
            SettingsSlider("GPU Layers (offload)", state.gpuLayers.toFloat(), 0f, 64f) { viewModel.updateGpuLayers(it.toInt()) }
        }

        // Section: Ollama Remote
        SettingsSection(title = "OLLAMA REMOTE") {
            OutlinedTextField(
                value = state.ollamaHost,
                onValueChange = { viewModel.updateOllamaHost(it) },
                label = { Text("Host", color = TextSecondary) },
                colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.ollamaPort.toString(),
                onValueChange = { viewModel.updateOllamaPort(it.toIntOrNull() ?: 11434) },
                label = { Text("Port", color = TextSecondary) },
                colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { viewModel.pingOllama() }, colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) {
                    Text(if (state.isPinging) "Pinging..." else "Test Connection", color = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                when (state.pingResult) {
                    true -> Text("✅ Success", color = SuccessGreen)
                    false -> Text("❌ Failed", color = ErrorRed)
                    null -> {}
                }
            }
        }

        // Section: Agent Behavior
        SettingsSection(title = "AGENT BEHAVIOR") {
            SettingsSlider("Max Steps", state.agentMaxSteps.toFloat(), 1f, 50f) { viewModel.updateAgentMaxSteps(it.toInt()) }
            SettingsSwitch("Auto-confirm shell commands", state.autoConfirmShell) { viewModel.updateAutoConfirmShell(it) }
            SettingsSwitch("Auto-confirm file writes", state.autoConfirmWrite) { viewModel.updateAutoConfirmWrite(it) }
            SettingsSwitch("Dry-run mode", state.dryRunMode) { viewModel.updateDryRunMode(it) }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Enabled Tools", color = TextPrimary, fontSize = 14.sp)
            // Stub checklist for enabled tools
            val tools = listOf("read_file", "write_file", "run_shell", "browse_url", "web_search")
            tools.forEach { tool ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = true, onCheckedChange = { /* TODO */ }, colors = CheckboxDefaults.colors(checkedColor = AccentBlue))
                    Text(tool, color = TextSecondary, fontSize = 14.sp)
                }
            }
        }

        // Section: Editor
        SettingsSection(title = "EDITOR") {
            SettingsSlider("Font Size", state.editorFontSize.toFloat(), 8f, 24f) { viewModel.updateEditorFontSize(it.toInt()) }
            SettingsSwitch("Auto-save", state.autoSaveEnabled) { viewModel.updateAutoSaveEnabled(it) }
            SettingsSwitch("AI Inline Suggestions", state.inlineSuggestionsEnabled) { viewModel.updateInlineSuggestionsEnabled(it) }
        }

        // Section: Storage
        SettingsSection(title = "STORAGE") {
            OutlinedTextField(
                value = state.modelsDir,
                onValueChange = { viewModel.updateModelsDir(it) },
                label = { Text("Models Directory", color = TextSecondary) },
                colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.projectsDir,
                onValueChange = { viewModel.updateProjectsDir(it) },
                label = { Text("Projects Directory", color = TextSecondary) },
                colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = { /* TODO */ }, colors = ButtonDefaults.buttonColors(containerColor = DarkSurface), modifier = Modifier.weight(1f)) {
                    Text("Clear Cache", color = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { /* TODO */ }, colors = ButtonDefaults.buttonColors(containerColor = DarkSurface), modifier = Modifier.weight(1f)) {
                    Text("Export Audit Log", color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("── $title", color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun SettingsSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, color = TextPrimary, fontSize = 14.sp)
            Text(if (value % 1 == 0f) value.toInt().toString() else String.format("%.1f", value), color = TextSecondary, fontSize = 14.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            colors = SliderDefaults.colors(
                thumbColor = AccentBlue,
                activeTrackColor = AccentBlue,
                inactiveTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun SettingsSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextPrimary, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue, checkedTrackColor = AccentBlue.copy(alpha = 0.5f))
        )
    }
}
