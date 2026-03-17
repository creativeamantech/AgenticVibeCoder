package com.mahavtaar.vibecoder.terminal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mahavtaar.vibecoder.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TerminalService : Service() {

    private val binder = LocalBinder()

    @Inject
    lateinit var sessionManager: TerminalSessionManager

    inner class LocalBinder : Binder() {
        fun getService(): TerminalService = this@TerminalService
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateNotification()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        sessionManager.destroyAll()
    }

    private fun startForegroundService() {
        val channelId = "vibecode_terminal_channel"
        val channelName = "VibeCode Terminal Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        updateNotification()
    }

    fun updateNotification() {
        val activeCount = sessionManager.sessions.value.size
        val notification = NotificationCompat.Builder(this, "vibecode_terminal_channel")
            .setContentTitle("VibeCode Terminal")
            .setContentText("$activeCount session(s) active")
            .setSmallIcon(R.mipmap.ic_launcher) // Fallback icon
            .build()

        startForeground(2, notification)
    }
}
