package com.mahavtaar.vibecoder.notifications

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mahavtaar.vibecoder.MainActivity
import com.mahavtaar.vibecoder.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val AGENT_CHANNEL = "agent_channel"
        const val TERMINAL_CHANNEL = "terminal_channel"
        const val DOWNLOAD_CHANNEL = "download_channel"

        const val ACTION_AGENT_STOP = "com.mahavtaar.vibecoder.AGENT_STOP"
        const val ACTION_AGENT_APPROVE = "com.mahavtaar.vibecoder.AGENT_APPROVE"
        const val ACTION_AGENT_REJECT = "com.mahavtaar.vibecoder.AGENT_REJECT"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager

    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val agentChannel = NotificationChannel(AGENT_CHANNEL, "VibeCode Agent", AndroidNotificationManager.IMPORTANCE_HIGH)
            val terminalChannel = NotificationChannel(TERMINAL_CHANNEL, "VibeCode Terminal", AndroidNotificationManager.IMPORTANCE_LOW)
            val downloadChannel = NotificationChannel(DOWNLOAD_CHANNEL, "Model Downloads", AndroidNotificationManager.IMPORTANCE_DEFAULT)

            notificationManager.createNotificationChannels(listOf(agentChannel, terminalChannel, downloadChannel))
        }
    }

    fun showAgentRunning(taskDescription: String) {
        val stopIntent = Intent(context, AgentNotificationReceiver::class.java).apply { action = ACTION_AGENT_STOP }
        val stopPendingIntent = PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, AGENT_CHANNEL)
            .setContentTitle("Agent Running")
            .setContentText("Agent is working on: $taskDescription")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .addAction(0, "Stop", stopPendingIntent)
            .build()

        notificationManager.notify(1001, notification)
    }

    fun showAgentCompleted(taskDescription: String, stepCount: Int) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, AGENT_CHANNEL)
            .setContentTitle("Agent Completed")
            .setContentText("Finished in $stepCount steps: $taskDescription")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1002, notification)
    }

    fun showAgentFailed(error: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, AGENT_CHANNEL)
            .setContentTitle("Agent Failed")
            .setContentText(error)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1003, notification)
    }

    fun showAgentConfirmationRequired(toolName: String, args: String) {
        val approveIntent = Intent(context, AgentNotificationReceiver::class.java).apply { action = ACTION_AGENT_APPROVE }
        val approvePendingIntent = PendingIntent.getBroadcast(context, 1, approveIntent, PendingIntent.FLAG_IMMUTABLE)

        val rejectIntent = Intent(context, AgentNotificationReceiver::class.java).apply { action = ACTION_AGENT_REJECT }
        val rejectPendingIntent = PendingIntent.getBroadcast(context, 2, rejectIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, AGENT_CHANNEL)
            .setContentTitle("Confirmation Required")
            .setContentText("Tool $toolName requested: $args")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .addAction(0, "✅ Approve", approvePendingIntent)
            .addAction(0, "❌ Reject", rejectPendingIntent)
            .build()

        notificationManager.notify(1004, notification)
    }

    fun showDownloadProgress(modelName: String, percent: Int) {
        val notification = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL)
            .setContentTitle("Downloading $modelName")
            .setContentText("$percent%")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setProgress(100, percent, false)
            .setOngoing(true)
            .build()

        notificationManager.notify(1005, notification)
    }

    fun showDownloadComplete(modelName: String) {
        val notification = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL)
            .setContentTitle("Download Complete")
            .setContentText("$modelName is ready to use.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1005, notification)
    }

    fun showCrashNotification(logPath: String) {
        val notification = NotificationCompat.Builder(context, AGENT_CHANNEL)
            .setContentTitle("VibeCode Crashed")
            .setContentText("Tap to share crash log from $logPath")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1006, notification)
    }

    fun cancelNotification(id: Int) {
        notificationManager.cancel(id)
    }
}
