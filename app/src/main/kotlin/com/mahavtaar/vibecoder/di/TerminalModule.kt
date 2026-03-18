package com.mahavtaar.vibecoder.di

import com.mahavtaar.vibecoder.terminal.TerminalSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TerminalModule {

    @Provides
    @Singleton
    fun provideTerminalSessionManager(): TerminalSessionManager {
        return TerminalSessionManager()
    }
}
