package com.mahavtaar.vibecoder.browser

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.webkit.WebView
import androidx.core.app.NotificationCompat
import com.mahavtaar.vibecoder.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BrowsingService : Service() {

    private val binder = LocalBinder()
    lateinit var webView: WebView

    @Inject
    lateinit var webViewProvider: WebViewProvider

    inner class LocalBinder : Binder() {
        fun getService(): BrowsingService = this@BrowsingService
    }

    override fun onCreate() {
        super.onCreate()

        // Ensure WebView is created on Main thread (Android requires this)
        webView = WebView(this)
        webViewProvider.setWebView(webView)

        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        webViewProvider.setWebView(null)
        webView.destroy()
    }

    private fun startForegroundService() {
        val channelId = "vibecode_browser_channel"
        val channelName = "VibeCode Browser Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("VibeCode Agent Browsing")
            .setContentText("The agent is running background web tasks.")
            .setSmallIcon(R.mipmap.ic_launcher) // Fallback icon
            .build()

        startForeground(1, notification)
    }
}
