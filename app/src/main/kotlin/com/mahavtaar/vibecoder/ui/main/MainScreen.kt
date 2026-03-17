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
import androidx.compose.ui.platform.LocalContext
import com.mahavtaar.vibecoder.ui.agent.AgentChatPanel
import com.mahavtaar.vibecoder.ui.agent.BrowserPanel
import com.mahavtaar.vibecoder.ui.editor.EditorScreen
import com.mahavtaar.vibecoder.ui.filetree.FileTreePanel
import com.mahavtaar.vibecoder.ui.theme.DarkBackground
import com.mahavtaar.vibecoder.ui.theme.DarkSurface
import com.mahavtaar.vibecoder.ui.theme.TextPrimary
import java.io.File
import android.webkit.WebView
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahavtaar.vibecoder.editor.EditorViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mahavtaar.vibecoder.ui.terminal.TerminalPanel
import com.mahavtaar.vibecoder.ui.settings.SettingsScreen
import com.mahavtaar.vibecoder.ui.workflows.WorkflowsScreen
import com.mahavtaar.vibecoder.ui.models.ModelManagerScreen
import com.mahavtaar.vibecoder.agent.AgentContext
import java.util.UUID
import com.mahavtaar.vibecoder.ui.agent.AgentViewModel
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    webView: WebView?,
    editorViewModel: EditorViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
    agentViewModel: AgentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val workingDir = File(context.filesDir, "workspace").absolutePath
    val currentDestination by mainViewModel.currentDestination.collectAsState()

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // Toolbar
        TopAppBar(
            title = { Text("VibeCode", color = TextPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkSurface
            ),
            actions = {
                IconButton(onClick = { mainViewModel.navigate(AppDestination.Editor) }) {
                    Icon(Icons.Default.Code, contentDescription = "Editor", tint = if (currentDestination == AppDestination.Editor) AccentBlue else TextPrimary)
                }
                IconButton(onClick = { mainViewModel.navigate(AppDestination.Workflows) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Workflows", tint = if (currentDestination == AppDestination.Workflows) AccentBlue else TextPrimary)
                }
                IconButton(onClick = { mainViewModel.navigate(AppDestination.ModelManager) }) {
                    Icon(Icons.Default.Memory, contentDescription = "Models", tint = if (currentDestination == AppDestination.ModelManager) AccentBlue else TextPrimary)
                }
                IconButton(onClick = { mainViewModel.navigate(AppDestination.Settings) }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (currentDestination == AppDestination.Settings) AccentBlue else TextPrimary)
                }
            }
        )
        Divider(color = Color.DarkGray)

        when (currentDestination) {
            AppDestination.Settings -> SettingsScreen()
            AppDestination.Workflows -> WorkflowsScreen(onRunWorkflow = { prompt ->
                mainViewModel.navigate(AppDestination.Editor) // Switch to main split-pane
                val agentContext = AgentContext(workingDir = workingDir, sessionId = UUID.randomUUID().toString())
                agentViewModel.startTask(prompt, agentContext)
            })
            AppDestination.ModelManager -> ModelManagerScreen()
            else -> {
                if (isTablet) {
                    // Split Pane Area (Editor, Agent, Browser)
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        // Left Pane: File Tree
                        Box(
                            modifier = Modifier
                                .width(200.dp)
                                .fillMaxHeight()
                                .background(DarkBackground)
                        ) {
                            FileTreePanel(
                                onFileSelected = { path -> editorViewModel.openFile(path) }
                            )
                        }

                        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Color.DarkGray)

                        // Center Pane: Code Editor
                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .fillMaxHeight()
                                .background(DarkBackground)
                        ) {
                            EditorScreen(viewModel = editorViewModel)
                        }

                        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = Color.DarkGray)

                        // Right Pane: Agent Chat / Browser
                        Column(
                            modifier = Modifier
                                .weight(1.5f)
                                .fillMaxHeight()
                                .background(DarkSurface)
                        ) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                AgentChatPanel(workingDir = workingDir, viewModel = agentViewModel)
                            }
                            Divider(color = Color.DarkGray)
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                BrowserPanel(webView = webView)
                            }
                        }
                    }
                } else {
                    // Phone Layout: Single active pane + Bottom Nav
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentDestination) {
                            AppDestination.Editor -> {
                                Column {
                                    // Optionally show file tree in a drawer or top row, but for now just Editor
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        EditorScreen(viewModel = editorViewModel)
                                    }
                                }
                            }
                            AppDestination.Agent -> AgentChatPanel(workingDir = workingDir, viewModel = agentViewModel)
                            AppDestination.Browser -> BrowserPanel(webView = webView)
                            AppDestination.Terminal -> TerminalPanel()
                            else -> EditorScreen(viewModel = editorViewModel) // Fallback
                        }
                    }

                    // Phone Bottom Navigation
                    NavigationBar(containerColor = DarkSurface) {
                        NavigationBarItem(
                            selected = currentDestination == AppDestination.Editor,
                            onClick = { mainViewModel.navigate(AppDestination.Editor) },
                            icon = { Icon(Icons.Default.Code, contentDescription = "Editor") },
                            label = { Text("Editor") },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentBlue, unselectedIconColor = TextPrimary, selectedTextColor = AccentBlue, unselectedTextColor = TextPrimary)
                        )
                        NavigationBarItem(
                            selected = currentDestination == AppDestination.Agent,
                            onClick = { mainViewModel.navigate(AppDestination.Agent) },
                            icon = { Icon(Icons.Default.Memory, contentDescription = "Agent") }, // Reuse memory icon for Agent
                            label = { Text("Agent") },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentBlue, unselectedIconColor = TextPrimary, selectedTextColor = AccentBlue, unselectedTextColor = TextPrimary)
                        )
                        NavigationBarItem(
                            selected = currentDestination == AppDestination.Browser,
                            onClick = { mainViewModel.navigate(AppDestination.Browser) },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Browser") }, // Reuse icon
                            label = { Text("Browser") },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentBlue, unselectedIconColor = TextPrimary, selectedTextColor = AccentBlue, unselectedTextColor = TextPrimary)
                        )
                        NavigationBarItem(
                            selected = currentDestination == AppDestination.Terminal,
                            onClick = { mainViewModel.navigate(AppDestination.Terminal) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Terminal") }, // Reuse icon
                            label = { Text("Terminal") },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentBlue, unselectedIconColor = TextPrimary, selectedTextColor = AccentBlue, unselectedTextColor = TextPrimary)
                        )
                    }
                }
            }
        }

        // Bottom Pane: Terminal (Tablet only)
        if (isTablet && (currentDestination == AppDestination.Editor || currentDestination == AppDestination.Agent || currentDestination == AppDestination.Browser || currentDestination == AppDestination.Terminal)) {
            Divider(color = Color.DarkGray)
            TerminalPanel()
        }
    }
}
