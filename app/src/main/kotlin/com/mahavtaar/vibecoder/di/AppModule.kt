package com.mahavtaar.vibecoder.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context {
        return context
    }

    // TODO: Phase 2 — implement LLM Module stubs here or in a separate LlmModule.kt
    // @Provides
    // @Singleton
    // fun provideLlamaEngine(): LlamaEngine = LlamaCppEngine(...)

    // TODO: Phase 3 — implement Agent Tool dependencies

    // TODO: Phase 4 — implement Browsing Agent dependencies
}
