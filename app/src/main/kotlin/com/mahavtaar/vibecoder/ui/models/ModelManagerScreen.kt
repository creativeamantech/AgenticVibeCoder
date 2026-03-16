package com.mahavtaar.vibecoder.ui.models

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahavtaar.vibecoder.ui.theme.DarkBackground

@Composable
fun ModelManagerScreen(viewModel: ModelManagerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Text("Model Manager", color = Color.White)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Engine:", color = Color.White, modifier = Modifier.padding(end = 8.dp))
            RadioButton(
                selected = state.currentEngine == "LlamaCpp",
                onClick = { viewModel.setEngine("LlamaCpp") }
            )
            Text("llama.cpp", color = Color.White)
            RadioButton(
                selected = state.currentEngine == "Ollama",
                onClick = { viewModel.setEngine("Ollama") }
            )
            Text("Ollama", color = Color.White)
        }

        if (state.isDownloading) {
            LinearProgressIndicator(progress = state.downloadProgress, modifier = Modifier.fillMaxWidth())
        }

        state.availableModels.forEach { model ->
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(model, color = Color.White, modifier = Modifier.weight(1f))
                Button(onClick = {
                    viewModel.downloadModel("http://example.com/$model.gguf", "$model.gguf")
                }) {
                    Text("Download")
                }
            }
        }

        Row(modifier = Modifier.padding(top = 16.dp)) {
            Button(onClick = { viewModel.loadModel("path/to/model") }) {
                Text("Load Model")
            }
            Button(onClick = { viewModel.unloadModel() }) {
                Text("Unload Model")
            }
        }
    }
}
