package com.mahavtaar.vibecoder.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Background = Color(0xFF0D1117)
private val Surface = Color(0xFF161B22)
private val Primary = Color(0xFF58A6FF)
private val Secondary = Color(0xFF1C2E4A)
private val Error = Color(0xFFF85149)
private val Success = Color(0xFF3FB950)
private val OnBackground = Color(0xFFC9D1D9)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    error = Error,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = OnBackground,
    onSurface = OnBackground,
    onError = Color.White
)

@Composable
fun VibeCodeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
