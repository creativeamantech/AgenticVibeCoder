package com.mahavtaar.vibecoder.ui.models

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahavtaar.vibecoder.ui.theme.DarkBackground

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun ModelManagerScreen(viewModel: ModelManagerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullRefreshState(refreshing = state.isRefreshing, onRefresh = { viewModel.refresh() })
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).pullRefresh(pullRefreshState)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
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
                Button(
                    onClick = {
                        val modelsDir = File(context.getExternalFilesDir(null), "models")
                        viewModel.loadModel(File(modelsDir, "Qwen2.5-Coder-7B.gguf").absolutePath)
                    },
                    enabled = !state.isLoadingModel
                ) {
                    Text("Load Qwen2.5")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.unloadModel() }) {
                    Text("Unload Model")
                }
            }

            if (state.isLoadingModel) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White)
            } else if (state.isModelLoaded) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("✅ Model Loaded: ${state.currentModelName}", color = androidx.compose.ui.graphics.Color.Green)
            }

            }
            PullRefreshIndicator(
                refreshing = state.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
