package com.mahavtaar.vibecoder.ui.terminal

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahavtaar.vibecoder.ui.theme.AccentBlue
import com.mahavtaar.vibecoder.ui.theme.DarkBackground
import com.mahavtaar.vibecoder.ui.theme.DarkSurface
import com.mahavtaar.vibecoder.ui.theme.ErrorRed
import com.mahavtaar.vibecoder.ui.theme.TextPrimary
import com.mahavtaar.vibecoder.ui.theme.TextSecondary

@Composable
fun TerminalPanel(
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var isExpanded by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val expandedHeight = screenHeight * 0.4f
    val collapsedHeight = 48.dp

    val currentHeight by animateDpAsState(targetValue = if (isExpanded) expandedHeight else collapsedHeight)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(currentHeight)
            .background(DarkBackground)
    ) {
        // Top Toolbar / Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(DarkSurface)
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TERMINAL", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(end = 16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.sessions) { sessionInfo ->
                    TerminalTabItem(
                        info = sessionInfo,
                        isActive = sessionInfo.id == state.activeSessionId,
                        onClick = {
                            viewModel.switchSession(sessionInfo.id)
                            isExpanded = true
                        },
                        onClose = { viewModel.closeSession(sessionInfo.id) }
                    )
                }
            }

            IconButton(onClick = {
                viewModel.createNewSession()
                isExpanded = true
            }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Add, contentDescription = "New Terminal", tint = Color.LightGray)
            }

            IconButton(onClick = { isExpanded = false }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Collapse", tint = Color.LightGray)
            }
        }

        Divider(color = Color.DarkGray)

        if (isExpanded) {
            // Main Xterm Area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val activeSession = viewModel.getActiveSession()
                XtermWebView(
                    bridge = viewModel,
                    activeSession = activeSession,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(4.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickCommandChip("./gradlew assembleDebug") { viewModel.executeCommand("./gradlew assembleDebug") }
                QuickCommandChip("git status") { viewModel.executeCommand("git status") }
                QuickCommandChip("ls -la") { viewModel.executeCommand("ls -la") }
                QuickCommandChip("pwd") { viewModel.executeCommand("pwd") }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { viewModel.clearTerminal() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = TextSecondary)
                }
                IconButton(onClick = { /* TODO: Copy Output */ }, modifier = Modifier.size(24.dp).padding(start=4.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Output", tint = TextSecondary)
                }
                IconButton(onClick = {
                    viewModel.getActiveSession()?.kill()
                }, modifier = Modifier.size(24.dp).padding(start=4.dp)) {
                    Icon(Icons.Default.Stop, contentDescription = "Kill Process", tint = ErrorRed)
                }
            }
        }
    }
}

@Composable
fun TerminalTabItem(
    info: TerminalSessionInfo,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val bgColor = if (isActive) DarkBackground else DarkSurface
    val textColor = if (isActive) TextPrimary else TextSecondary

    Row(
        modifier = Modifier
            .height(32.dp)
            .background(bgColor, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(info.title, color = textColor, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onClose, modifier = Modifier.size(16.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun QuickCommandChip(command: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .background(Color.DarkGray, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(command, color = Color.White, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
    }
}
