package com.mahavtaar.vibecoder.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mahavtaar.vibecoder.ui.theme.DarkBackground
import com.mahavtaar.vibecoder.ui.theme.DarkSurface
import com.mahavtaar.vibecoder.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // Toolbar
        TopAppBar(
            title = { Text("VibeCode", color = TextPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkSurface
            ),
            actions = {
                // TODO: Add toolbar actions [☰ Project] [▶ Run] [🤖 Agent] [⚙ Settings]
            }
        )
        Divider(color = Color.DarkGray)

        // Split Pane Area
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Left Pane: File Tree
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .background(DarkBackground)
                    .padding(8.dp),
            ) {
                Text("📁 File Tree Stub", color = TextPrimary)
                // TODO: Phase 6 - Implement File Explorer
            }

            Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Color.DarkGray)

            // Center Pane: Code Editor
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .background(DarkBackground)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("💻 Code Editor Stub (MonacoWebView)", color = TextPrimary)
                // TODO: Phase 6 - Implement MonacoWebView wrapper and tabs
            }

            Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Color.DarkGray)

            // Right Pane: Agent Chat / Browser
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight()
                    .background(DarkSurface)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖 Agent / 🌐 Browser Stub", color = TextPrimary)
                // TODO: Phase 4 & 5 - Implement Agent Chat Panel and BrowsingAgent UI
            }
        }

        Divider(color = Color.DarkGray)

        // Bottom Pane: Terminal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color.Black)
                .padding(8.dp)
        ) {
            Text("$ Terminal Stub (xterm.js)", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
            // TODO: Phase 7 - Implement XtermWebView and TerminalService
        }
    }
}
