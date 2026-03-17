package com.mahavtaar.vibecoder.ui.filetree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahavtaar.vibecoder.ui.theme.AccentBlue
import com.mahavtaar.vibecoder.ui.theme.DarkBackground
import com.mahavtaar.vibecoder.ui.theme.TextPrimary
import com.mahavtaar.vibecoder.ui.theme.TextSecondary

@Composable
fun FileTreePanel(
    viewModel: FileTreeViewModel = hiltViewModel(),
    onFileSelected: (path: String) -> Unit
) {
    val rootNode by viewModel.treeRoot.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PROJECT", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { /* TODO: New File Dialog */ }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Add, contentDescription = "New File", tint = Color.Gray)
            }
            IconButton(onClick = { /* TODO: Import SAF */ }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Upload, contentDescription = "Import", tint = Color.Gray)
            }
        }

        // Tree List
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (rootNode?.children?.isEmpty() == true || rootNode == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Empty", tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Open a project folder to start", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    rootNode?.let { root ->
                        item {
                            RenderNode(
                                node = root,
                                depth = 0,
                                onToggle = { viewModel.toggleExpand(it) },
                                onFileClick = { onFileSelected(it.path) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RenderNode(
    node: FileTreeNode,
    depth: Int,
    onToggle: (FileTreeNode.Directory) -> Unit,
    onFileClick: (FileTreeNode.FileNode) -> Unit
) {
    val paddingStart = (depth * 16).dp

    when (node) {
        is FileTreeNode.Directory -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(node) }
                    .padding(start = paddingStart, top = 4.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = "Toggle",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (node.isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = "Folder",
                    tint = AccentBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(node.name, color = TextPrimary, fontSize = 14.sp)
            }

            if (node.isExpanded) {
                Column {
                    node.children.forEach { child ->
                        RenderNode(child, depth + 1, onToggle, onFileClick)
                    }
                }
            }
        }
        is FileTreeNode.FileNode -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFileClick(node) }
                    .padding(start = paddingStart + 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Determine icon based on language (simplified)
                val icon = when (node.language) {
                    "kotlin", "java", "python", "javascript", "typescript" -> Icons.Default.Code
                    else -> Icons.Default.Description
                }

                val iconTint = when (node.language) {
                    "kotlin" -> Color(0xFF7F52FF) // Purple
                    "python" -> Color(0xFFFFD43B) // Yellow
                    "javascript", "typescript" -> Color(0xFFF7DF1E) // Yellow
                    "html", "css" -> Color(0xFFE34F26) // Orange
                    else -> TextSecondary
                }

                Icon(
                    imageVector = icon,
                    contentDescription = "File",
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(node.name, color = TextPrimary, fontSize = 14.sp)
            }
        }
    }
}
