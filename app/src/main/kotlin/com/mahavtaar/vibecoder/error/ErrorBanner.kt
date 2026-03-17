package com.mahavtaar.vibecoder.error

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahavtaar.vibecoder.ui.theme.ErrorRed
import kotlinx.coroutines.delay

@Composable
fun ErrorBanner(error: AppError?, onDismiss: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        if (error != null) {
            isVisible = true
            delay(5000)
            isVisible = false
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ErrorRed)
                .clickable {
                    isVisible = false
                    onDismiss()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Error", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(error?.message ?: "Unknown", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}
