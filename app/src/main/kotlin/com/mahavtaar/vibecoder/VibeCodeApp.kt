package com.mahavtaar.vibecoder

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VibeCodeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // TODO: Phase 2 - Initialize LLM core
        // TODO: Phase 3 - Initialize storage/tools
    }
}
