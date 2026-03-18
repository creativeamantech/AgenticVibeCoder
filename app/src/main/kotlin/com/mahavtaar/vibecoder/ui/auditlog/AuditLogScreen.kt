package com.mahavtaar.vibecoder.ui.auditlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahavtaar.vibecoder.data.db.AuditLog
import com.mahavtaar.vibecoder.ui.theme.AccentBlue
import com.mahavtaar.vibecoder.ui.theme.DarkBackground
import com.mahavtaar.vibecoder.ui.theme.DarkSurface
import com.mahavtaar.vibecoder.ui.theme.ErrorRed
import com.mahavtaar.vibecoder.ui.theme.SuccessGreen
import com.mahavtaar.vibecoder.ui.theme.TextPrimary
import com.mahavtaar.vibecoder.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun AuditLogScreen(viewModel: AuditLogViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedLog by remember { mutableStateOf<AuditLog?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val pullRefreshState = rememberPullRefreshState(refreshing = state.isRefreshing, onRefresh = { viewModel.refresh() })

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Audit Log", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { showClearDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = TextSecondary)
            }
            IconButton(onClick = {
                val msg = viewModel.exportToJson()
                snackbarMessage = msg
            }) {
                Icon(Icons.Default.Share, contentDescription = "Export", tint = AccentBlue)
            }
        }

        // Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "COMPLETED", "FAILED", "TODAY").forEach { filter ->
                FilterChip(
                    selected = state.filterMode == filter,
                    onClick = { viewModel.setFilter(filter) },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue,
                        selectedLabelColor = Color.White,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        if (state.filteredSessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No logs found.", color = TextSecondary, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.filteredSessions) { sessionWithLogs ->
                    AuditSessionCard(sessionWithLogs, onLogClick = { selectedLog = it })
                }
            }
        }

        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Logs?", color = TextPrimary) },
            text = { Text("This will permanently delete all session and audit logs.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAll()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Clear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = AccentBlue)
                }
            },
            containerColor = DarkSurface
        )
    }

    selectedLog?.let { log ->
        AuditLogDetailDialog(log = log, onDismiss = { selectedLog = null })
    }

    snackbarMessage?.let {
        Snackbar(modifier = Modifier.padding(16.dp)) {
            Text(it, color = Color.White)
        }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(3000)
            snackbarMessage = null
        }
    }
}

@Composable
fun AuditSessionCard(
    sessionWithLogs: AuditSessionWithSteps,
    onLogClick: (AuditLog) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val session = sessionWithLogs.session
    val format = SimpleDateFormat("MMM dd, HH:mm", Locale.US)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = session.taskDescription,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))

                val statusColor = when(session.status) {
                    "COMPLETED" -> SuccessGreen
                    "FAILED" -> ErrorRed
                    else -> AccentBlue
                }
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .border(1.dp, statusColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(session.status, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Start: ${format.format(Date(session.startTime))}", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Steps: ${sessionWithLogs.logs.size}", color = TextSecondary, fontSize = 12.sp)
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))

                    sessionWithLogs.logs.forEach { log ->
                        AuditLogRow(log, onClick = { onLogClick(log) })
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogRow(log: AuditLog, onClick: () -> Unit) {
    val format = SimpleDateFormat("HH:mm:ss", Locale.US)
    val icon = when (log.eventType) {
        "THOUGHT" -> "🧠"
        "ACTION_STARTED", "ACTION_COMPLETED" -> "🔧"
        "FINAL_ANSWER" -> "✅"
        "ERROR" -> "❌"
        else -> "👁"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(format.format(Date(log.timestamp)), color = TextSecondary, fontSize = 10.sp, modifier = Modifier.width(50.dp))
        Text(icon, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Step ${log.stepNumber} • ${log.eventType}", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                if (log.toolName != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.background(Color.DarkGray, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Text(log.toolName, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            val snippet = log.outputText ?: log.inputJson ?: ""
            Text(
                text = snippet,
                color = TextPrimary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AuditLogDetailDialog(log: AuditLog, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Details", color = TextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (log.toolName != null) {
                    Text("Tool: ${log.toolName}", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (log.inputJson != null) {
                    Text("Input JSON:", color = TextSecondary, fontSize = 12.sp)
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(4.dp)).padding(8.dp)) {
                        Text(log.inputJson, color = SuccessGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (log.outputText != null) {
                    Text("Output:", color = TextSecondary, fontSize = 12.sp)
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black, RoundedCornerShape(4.dp)).padding(8.dp)) {
                        Text(log.outputText, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentBlue)
            }
        },
        containerColor = DarkSurface
    )
}
