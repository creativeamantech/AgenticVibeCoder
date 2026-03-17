package com.mahavtaar.vibecoder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mahavtaar.vibecoder.ui.main.MainScreen
import com.mahavtaar.vibecoder.ui.theme.VibeCodeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.mahavtaar.vibecoder.browser.WebViewProvider

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var webViewProvider: WebViewProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start foreground service
        val serviceIntent = Intent(this, com.mahavtaar.vibecoder.browser.BrowsingService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            val webView by webViewProvider.webViewFlow.collectAsState()

            VibeCodeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(webView = webView)
                }
            }
        }
    }
}
