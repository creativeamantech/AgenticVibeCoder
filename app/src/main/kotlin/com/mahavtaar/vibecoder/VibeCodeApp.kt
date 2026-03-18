package com.mahavtaar.vibecoder

import android.app.Application
import com.mahavtaar.vibecoder.error.CrashReporter
import com.mahavtaar.vibecoder.notifications.NotificationManager
import com.mahavtaar.vibecoder.util.WakeLockManager
import com.mahavtaar.vibecoder.data.db.AuditLogDao
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VibeCodeApp : Application() {

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var auditLogDao: AuditLogDao

    // We would normally inject WakeLockManager here to initialize or hold a reference if needed early

    override fun onCreate() {
        super.onCreate()

        notificationManager.createChannels()

        val crashReporter = CrashReporter(this, auditLogDao, notificationManager)
        Thread.setDefaultUncaughtExceptionHandler(crashReporter)
    }
}

// PHASE 9 COMPLETE
