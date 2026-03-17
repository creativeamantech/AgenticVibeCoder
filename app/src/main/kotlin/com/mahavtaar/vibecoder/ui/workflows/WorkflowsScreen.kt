package com.mahavtaar.vibecoder.ui.workflows

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahavtaar.vibecoder.agent.WorkflowTemplate
import com.mahavtaar.vibecoder.agent.WorkflowTemplates
import com.mahavtaar.vibecoder.ui.theme.AccentBlue
import com.mahavtaar.vibecoder.ui.theme.DarkBackground
import com.mahavtaar.vibecoder.ui.theme.DarkSurface
import com.mahavtaar.vibecoder.ui.theme.TextPrimary
import com.mahavtaar.vibecoder.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowsScreen(onRunWorkflow: (taskPrompt: String) -> Unit) {
    var showCustomDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PRE-BUILT WORKFLOWS", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(
                onClick = { showCustomDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Custom", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Custom", color = Color.White)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 250.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(WorkflowTemplates.ALL) { template ->
                WorkflowCard(template) {
                    onRunWorkflow(template.taskPrompt)
                }
            }
        }

        if (showCustomDialog) {
            CustomWorkflowDialog(
                onDismiss = { showCustomDialog = false },
                onRun = { prompt ->
                    showCustomDialog = false
                    onRunWorkflow(prompt)
                }
            )
        }
    }
}

@Composable
fun WorkflowCard(template: WorkflowTemplate, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(template.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = template.description,
                color = TextSecondary,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomWorkflowDialog(onDismiss: () -> Unit, onRun: (String) -> Unit) {
    var prompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Custom Workflow", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Task Description", color = TextSecondary) },
                colors = TextFieldDefaults.outlinedTextFieldColors(textColor = TextPrimary),
                modifier = Modifier.fillMaxWidth().height(150.dp),
                maxLines = 5
            )
        },
        confirmButton = {
            Button(
                onClick = { if (prompt.isNotBlank()) onRun(prompt) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Run")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
