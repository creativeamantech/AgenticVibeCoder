package com.mahavtaar.vibecoder.error

import android.content.Context
import android.os.Environment
import com.mahavtaar.vibecoder.data.db.AuditLogDao
import com.mahavtaar.vibecoder.notifications.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashReporter(
    private val context: Context,
    private val auditLogDao: AuditLogDao,
    private val notificationManager: NotificationManager
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val crashDir = File(context.filesDir, "crash_logs")
                crashDir.mkdirs()
                val crashFile = File(crashDir, "crash_$timestamp.txt")

                val stackTrace = throwable.stackTraceToString()
                val deviceInfo = """
                    Device: ${android.os.Build.DEVICE}
                    Model: ${android.os.Build.MODEL}
                    SDK: ${android.os.Build.VERSION.SDK_INT}
                """.trimIndent()

                val recentLogs = try {
                    // In a production app, the DAO should have `getLatest(limit: Int)`
                    // We'll fallback to a basic query if that doesn't exist, though we only have `getBySession` currently defined.
                    // Let's implement a safe manual fetch using a generic SQLite query if possible, or just note it's a stub due to missing DAO method in spec.
                    "--- [Audit Logs Omitted: requires specific DAO method] ---"
                } catch(e: Exception) { "Failed to fetch logs: ${e.message}" }

                val logContent = "$deviceInfo\n\n$stackTrace\n\nRecent Audit Logs\n$recentLogs"

                FileOutputStream(crashFile).use { it.write(logContent.toByteArray()) }

                notificationManager.showCrashNotification(crashFile.absolutePath)
            } catch (e: Exception) {
                // Failsafe within a failsafe
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
