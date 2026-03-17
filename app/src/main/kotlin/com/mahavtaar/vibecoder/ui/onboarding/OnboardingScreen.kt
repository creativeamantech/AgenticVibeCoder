package com.mahavtaar.vibecoder.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahavtaar.vibecoder.ui.theme.AccentBlue
import com.mahavtaar.vibecoder.ui.theme.DarkBackground
import com.mahavtaar.vibecoder.ui.theme.DarkSurface
import com.mahavtaar.vibecoder.ui.theme.TextPrimary
import com.mahavtaar.vibecoder.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    var selectedEngine by remember { mutableStateOf("ollama") }
    var ollamaHost by remember { mutableStateOf("192.168.1.100") }
    var ollamaPort by remember { mutableStateOf("11434") }
    var workingDir by remember { mutableStateOf("/sdcard/VibeCode/workspace") }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (pagerState.currentPage < 3) {
                TextButton(onClick = onComplete) {
                    Text("Skip", color = TextSecondary)
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp)) // Maintain height
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> PageWelcome()
                1 -> PageEngine(
                    selectedEngine,
                    onEngineSelected = { selectedEngine = it },
                    ollamaHost,
                    onHostChange = { ollamaHost = it },
                    ollamaPort,
                    onPortChange = { ollamaPort = it }
                )
                2 -> PageWorkingDir(workingDir) { workingDir = it }
                3 -> PageReady(onComplete)
            }
        }

        // Bottom Nav + Dots
        Row(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pagerState.pageCount) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (index == pagerState.currentPage) AccentBlue else Color.DarkGray)
                    )
                }
            }

            if (pagerState.currentPage < 3) {
                Button(
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Next →", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun PageWelcome() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Compose-drawn VibeCode logo
        Canvas(modifier = Modifier.size(100.dp)) {
            val w = size.width
            val h = size.height
            val pathV = Path().apply {
                moveTo(w * 0.2f, h * 0.2f)
                lineTo(w * 0.5f, h * 0.8f)
                lineTo(w * 0.8f, h * 0.2f)
            }
            drawPath(pathV, color = AccentBlue, style = Stroke(width = 8.dp.toPx()))

            val pathC = Path().apply {
                moveTo(w * 0.8f, h * 0.8f)
                lineTo(w * 0.5f, h * 0.5f)
                lineTo(w * 0.8f, h * 0.5f)
            }
            drawPath(pathC, color = TextPrimary, style = Stroke(width = 8.dp.toPx()))
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("VibeCode", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your AI Coding Agent,\nOn-Device.", color = TextSecondary, fontSize = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageEngine(
    selectedEngine: String,
    onEngineSelected: (String) -> Unit,
    host: String,
    onHostChange: (String) -> Unit,
    port: String,
    onPortChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Choose LLM Engine", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            EngineCard(
                title = "🌐 Ollama",
                subtitle = "Connect to PC over Wi-Fi",
                isSelected = selectedEngine == "ollama",
                onClick = { onEngineSelected("ollama") },
                modifier = Modifier.weight(1f)
            )
            EngineCard(
                title = "🦙 llama.cpp",
                subtitle = "Run models completely offline",
                isSelected = selectedEngine == "llama.cpp",
                onClick = { onEngineSelected("llama.cpp") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (selectedEngine == "ollama") {
            OutlinedTextField(
                value = host,
                onValueChange = onHostChange,
                label = { Text("Ollama Host IP", color = TextSecondary) },
                colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text("Port", color = TextSecondary) },
                colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Button(onClick = { /* TODO */ }, colors = ButtonDefaults.buttonColors(containerColor = DarkSurface), modifier = Modifier.fillMaxWidth()) {
                Text("Download Models...", color = AccentBlue)
            }
        }
    }
}

@Composable
fun EngineCard(title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (isSelected) AccentBlue.copy(alpha = 0.2f) else DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(120.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, AccentBlue) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, color = TextSecondary, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun PageWorkingDir(workingDir: String, onDirChange: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Workspace Setup", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("VibeCode needs a sandboxed folder to store your projects and isolate agent file modifications.", color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)

        Spacer(modifier = Modifier.height(32.dp))

        Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Selected Path:", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(workingDir, color = AccentBlue, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* TODO: SAF Picker */ }, colors = ButtonDefaults.buttonColors(containerColor = DarkSurface), modifier = Modifier.fillMaxWidth()) {
            Text("Change Directory", color = TextPrimary)
        }
    }
}

@Composable
fun PageReady(onComplete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = "Ready", tint = AccentBlue, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Text("You're all set!", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("The agent is ready to start coding.", color = TextSecondary)

        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onComplete,
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Start Coding →", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
