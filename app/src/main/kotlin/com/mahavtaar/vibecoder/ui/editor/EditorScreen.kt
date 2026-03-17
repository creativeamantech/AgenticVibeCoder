package com.mahavtaar.vibecoder.ui.editor

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahavtaar.vibecoder.editor.EditorViewModel
import com.mahavtaar.vibecoder.editor.FileTab
import com.mahavtaar.vibecoder.editor.MonacoWebView
import com.mahavtaar.vibecoder.ui.theme.AccentBlue
import com.mahavtaar.vibecoder.ui.theme.DarkBackground
import com.mahavtaar.vibecoder.ui.theme.DarkSurface
import com.mahavtaar.vibecoder.ui.theme.SuccessGreen
import com.mahavtaar.vibecoder.ui.theme.TextPrimary
import com.mahavtaar.vibecoder.ui.theme.TextSecondary

@Composable
fun EditorScreen(viewModel: EditorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // Tab Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.tabs) { tab ->
                FileTabItem(
                    tab = tab,
                    isActive = tab.id == state.activeTab?.id,
                    onClick = { viewModel.switchTab(tab.id) },
                    onClose = { viewModel.closeTab(tab.id) }
                )
            }
        }
        Divider(color = Color.DarkGray)

        // Main Editor Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.tabs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No file open. Select a file from the tree.", color = TextSecondary)
                }
            } else {
                MonacoWebView(
                    bridge = viewModel,
                    modifier = Modifier.fillMaxSize()
                ) { wrapper ->
                    viewModel.setWebViewWrapper(wrapper)
                }

                // Diff View Overlay Buttons
                if (state.isDiffMode) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(DarkSurface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.exitAgentDiff(accept = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Text("Accept Changes", color = Color.White)
                        }
                        Button(
                            onClick = { viewModel.exitAgentDiff(accept = false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Reject", color = Color.White)
                        }
                    }
                }

                // Ghost text helper button
                if (state.isGhostTextVisible) {
                    Button(
                        onClick = { viewModel.acceptGhostText() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                    ) {
                        Text("Accept AI Suggestion [Tab]", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AccentBlue)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val language = state.activeTab?.language?.capitalize() ?: "Plain Text"
            Text(language, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isSaving) {
                    Text("Saving...", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(end = 16.dp))
                }
                Text("Ln ${state.cursorLine}, Col ${state.cursorCol}", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.width(16.dp))
                Text("UTF-8", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun FileTabItem(
    tab: FileTab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val bgColor = if (isActive) DarkBackground else DarkSurface
    val textColor = if (isActive) TextPrimary else TextSecondary

    Row(
        modifier = Modifier
            .height(36.dp)
            .background(bgColor, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tab.isDirty) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentBlue))
            Spacer(modifier = Modifier.width(6.dp))
        }

        Text(tab.fileName, color = textColor, fontSize = 14.sp)

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = onClose, modifier = Modifier.size(16.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor, modifier = Modifier.size(14.dp))
        }
    }
}
