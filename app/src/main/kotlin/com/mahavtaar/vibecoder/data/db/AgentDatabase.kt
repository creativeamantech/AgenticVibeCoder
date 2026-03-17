package com.mahavtaar.vibecoder.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MemoryEntity::class, AgentSession::class, AuditLog::class], version = 1, exportSchema = false)
abstract class AgentDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun agentSessionDao(): AgentSessionDao
    abstract fun auditLogDao(): AuditLogDao
}
