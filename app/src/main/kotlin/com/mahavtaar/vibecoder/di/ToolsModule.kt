package com.mahavtaar.vibecoder.di

import android.content.Context
import androidx.room.Room
import com.mahavtaar.vibecoder.agent.memory.AgentMemory
import com.mahavtaar.vibecoder.agent.tools.*
import com.mahavtaar.vibecoder.browser.BrowsingAgent
import com.mahavtaar.vibecoder.browser.BrowsingRateLimiter
import com.mahavtaar.vibecoder.data.db.AgentDatabase
import com.mahavtaar.vibecoder.data.db.MemoryDao
import com.mahavtaar.vibecoder.llm.LlamaEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ToolsModule {

    @Provides
    @Singleton
    fun provideBrowsingRateLimiter(): BrowsingRateLimiter {
        return BrowsingRateLimiter()
    }

    @Provides
    @Singleton
    fun provideAgentDatabase(@ApplicationContext context: Context): AgentDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AgentDatabase::class.java,
            "agent_memory.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideMemoryDao(database: AgentDatabase): MemoryDao {
        return database.memoryDao()
    }

    @Provides
    @Singleton
    fun provideAgentSessionDao(database: AgentDatabase): com.mahavtaar.vibecoder.data.db.AgentSessionDao {
        return database.agentSessionDao()
    }

    @Provides
    @Singleton
    fun provideAuditLogDao(database: AgentDatabase): com.mahavtaar.vibecoder.data.db.AuditLogDao {
        return database.auditLogDao()
    }

    @Provides
    @Singleton
    fun provideAgentMemory(memoryDao: MemoryDao): AgentMemory {
        return AgentMemory(memoryDao)
    }

    @Provides
    @Singleton
    fun provideInMemoryHashMap(): InMemoryHashMap {
        return InMemoryHashMap()
    }

    @Provides
    @Singleton
    fun provideWorkingDir(@ApplicationContext context: Context): File {
        // Use an app-private sandbox directory to prevent accidental system destruction
        val dir = File(context.filesDir, "workspace")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    @Provides
    @Singleton
    @ElementsIntoSet
    fun provideAgentTools(
        workingDir: File,
        inMemoryHashMap: InMemoryHashMap,
        llamaEngine: LlamaEngine,
        browsingAgent: BrowsingAgent,
        browsingRateLimiter: BrowsingRateLimiter
    ): Set<AgentTool> {
        return setOf(
            // File Tools
            ReadFileTool(workingDir),
            WriteFileTool(workingDir),
            AppendFileTool(workingDir),
            ListDirTool(workingDir),
            CreateDirTool(workingDir),
            DeleteFileTool(workingDir),
            MoveFileTool(workingDir),
            SearchInFilesTool(workingDir),
            PatchFileTool(workingDir),

            // Shell Tools
            RunShellTool(workingDir),
            RunGradleTool(workingDir),
            RunPythonTool(workingDir),
            GetEnvTool(workingDir),

            // Memory Tools
            RememberTool(inMemoryHashMap),
            RecallTool(inMemoryHashMap),
            ListMemoryTool(inMemoryHashMap),
            ForgetTool(inMemoryHashMap),
            AddToScratchpadTool(inMemoryHashMap),
            ClearScratchpadTool(inMemoryHashMap),

            // Code Tools
            AnalyzeCodeTool(workingDir),
            FindErrorsTool(workingDir),
            GenerateCodeTool(workingDir, llamaEngine),
            ExplainCodeTool(workingDir, llamaEngine),

            // Project Tools
            ScaffoldProjectTool(workingDir),
            GitCommandTool(workingDir),
            ReadGradleTool(workingDir),
            AddDependencyTool(workingDir),

            // Browsing Tools
            BrowseUrlTool(browsingAgent, browsingRateLimiter),
            ClickElementTool(browsingAgent, browsingRateLimiter),
            FillInputTool(browsingAgent, browsingRateLimiter),
            ExtractTextTool(browsingAgent, browsingRateLimiter),
            TakeScreenshotTool(browsingAgent, browsingRateLimiter),
            ScrollPageTool(browsingAgent, browsingRateLimiter),
            WaitForElementTool(browsingAgent, browsingRateLimiter),
            GetPageLinksTool(browsingAgent, browsingRateLimiter),
            WebSearchTool(browsingAgent, browsingRateLimiter),
            DownloadFileTool(browsingAgent, browsingRateLimiter, workingDir)
        )
    }

    @Provides
    @Singleton
    fun provideToolRegistry(tools: Set<@JvmSuppressWildcards AgentTool>): ToolRegistry {
        return ToolRegistry(tools)
    }
}
