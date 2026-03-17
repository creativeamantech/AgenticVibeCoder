package com.mahavtaar.vibecoder.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AuditLogDao {
    @Insert
    suspend fun insert(log: AuditLog)

    @Query("SELECT * FROM audit_log WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getBySession(sessionId: String): List<AuditLog>
}
