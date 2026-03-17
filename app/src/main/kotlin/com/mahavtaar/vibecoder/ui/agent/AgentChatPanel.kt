package com.mahavtaar.vibecoder.ui.agent

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahavtaar.vibecoder.agent.AgentContext
import com.mahavtaar.vibecoder.ui.theme.AccentBlue
import com.mahavtaar.vibecoder.ui.theme.DarkBackground
import com.mahavtaar.vibecoder.ui.theme.DarkSurface
import com.mahavtaar.vibecoder.ui.theme.ErrorRed
import com.mahavtaar.vibecoder.ui.theme.SuccessGreen
import com.mahavtaar.vibecoder.ui.theme.TextPrimary
import com.mahavtaar.vibecoder.ui.theme.ThoughtBubbleBackground
import java.util.UUID

@Composable
fun AgentChatPanel(
    workingDir: String,
    viewModel: AgentViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var taskInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.steps.size) {
        if (state.steps.isNotEmpty()) {
            listState.animateScrollToItem(state.steps.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (state.isRunning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = AccentBlue,
                trackColor = DarkSurface
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
            if (state.steps.isEmpty() && !state.isRunning) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🤖", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Run your first task", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(state.steps) { index, step ->
                        when (step) {
                            is AgentStepUi.ThoughtBubble -> ThoughtBubbleCard(step)
                            is AgentStepUi.ActionCard -> ActionCard(step)
                            is AgentStepUi.ObservationCard -> ObservationCard(step) {
                                viewModel.toggleObservationExpansion(index)
                            }
                            is AgentStepUi.FinalAnswerCard -> FinalAnswerCard(step)
                            is AgentStepUi.ConfirmationCard -> ConfirmationCard(step) { approved ->
                                viewModel.confirmAction(approved)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = taskInput,
                onValueChange = { taskInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("What should the agent do?", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkBackground,
                    unfocusedContainerColor = DarkBackground,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))

            if (state.isRunning) {
                Button(
                    onClick = { viewModel.stopAgent() },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("⏹ Stop")
                }
            } else {
                Button(
                    onClick = {
                        if (taskInput.isNotBlank()) {
                            val context = AgentContext(
                                workingDir = workingDir,
                                sessionId = UUID.randomUUID().toString()
                            )
                            viewModel.startTask(taskInput, context)
                            taskInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("▶ Run")
                }
            }
        }
    }
}

@Composable
fun ThoughtBubbleCard(step: AgentStepUi.ThoughtBubble) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ThoughtBubbleBackground),
        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, topStart = 0.dp, bottomStart = 0.dp),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .border(width = 2.dp, color = AccentBlue, shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp, topStart = 0.dp, bottomStart = 0.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("🤖 THOUGHT", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(step.text, color = TextPrimary, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
fun ActionCard(step: AgentStepUi.ActionCard) {
    var isExpanded by remember { mutableStateOf(false) }

    val borderColor = when (step.status) {
        ActionStatus.RUNNING -> AccentBlue
        ActionStatus.SUCCESS -> SuccessGreen
        ActionStatus.ERROR -> ErrorRed
    }

    val borderWidth = if (step.status == ActionStatus.RUNNING) {
        val infiniteTransition = rememberInfiniteTransition()
        val pulse by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        pulse.dp
    } else 2.dp

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(8.dp))
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ACTION: ", color = Color.Gray, fontSize = 12.sp)
                Text(step.toolName, color = TextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text(step.status.name, color = borderColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))) {
                    Text(step.argsJson.toString(), color = AccentBlue, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ObservationCard(step: AgentStepUi.ObservationCard, onToggleExpand: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color.DarkGray, shape = RoundedCornerShape(8.dp))
            .clickable { onToggleExpand() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("OBSERVATION: ${step.toolName}", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = step.result,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                maxLines = if (step.isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
            if (!step.isExpanded && step.result.lines().size > 3) {
                Text("Tap to expand...", color = AccentBlue, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun FinalAnswerCard(step: AgentStepUi.FinalAnswerCard) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF132A1A)), // Dark green background
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 2.dp, color = SuccessGreen, shape = RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FINAL ANSWER", color = SuccessGreen, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { /* TODO: Copy to clipboard */ }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = SuccessGreen)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(step.text, color = Color.White)
        }
    }
}

@Composable
fun ConfirmationCard(step: AgentStepUi.ConfirmationCard, onRespond: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 2.dp, color = Color(0xFFD29922), shape = RoundedCornerShape(8.dp)) // Warning yellow
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("⚠️ CONFIRMATION REQUIRED", color = Color(0xFFD29922), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(step.message, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tool: ${step.toolName}", color = Color.Gray, fontSize = 12.sp)
            Text("Args: ${step.pendingArgs}", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { onRespond(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground, contentColor = ErrorRed),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("❌ Reject")
                }
                Button(
                    onClick = { onRespond(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White)
                ) {
                    Text("✅ Approve")
                }
            }
        }
    }
}
