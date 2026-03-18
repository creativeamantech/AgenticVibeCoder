package com.mahavtaar.vibecoder.ui.agent

import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahavtaar.vibecoder.ui.theme.AccentBlue
import com.mahavtaar.vibecoder.ui.theme.DarkBackground
import com.mahavtaar.vibecoder.ui.theme.DarkSurface
import com.mahavtaar.vibecoder.ui.theme.TextPrimary

@Composable
fun BrowserPanel(
    webView: WebView?,
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val currentPage by viewModel.currentPage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var urlInput by remember { mutableStateOf("") }
    var isWebViewMode by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter URL or search", color = Color.Gray) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkBackground,
                    unfocusedContainerColor = DarkBackground,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Button(
                onClick = { viewModel.navigate(urlInput) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Go")
            }

            Button(
                onClick = { isWebViewMode = !isWebViewMode },
                colors = ButtonDefaults.buttonColors(containerColor = if (isWebViewMode) AccentBlue else Color.DarkGray),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(if (isWebViewMode) "WV" else "HTTP")
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = AccentBlue)
        }

        // Web Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isWebViewMode && webView != null) {
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Http Mode placeholder UI
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(currentPage?.title ?: "No Page Loaded", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(currentPage?.visibleText ?: "Content goes here", color = Color.LightGray)
                }
            }

            // Screenshot Thumbnail
            currentPage?.screenshot?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Screenshot Thumbnail",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(100.dp)
                        .border(2.dp, AccentBlue)
                )
            }
        }

        // Bottom Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentPage?.title ?: "No Title",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = currentPage?.url ?: "about:blank",
                color = Color.Gray,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
        }
    }
}
