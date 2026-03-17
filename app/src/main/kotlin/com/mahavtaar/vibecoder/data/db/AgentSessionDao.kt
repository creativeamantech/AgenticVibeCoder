package com.mahavtaar.vibecoder.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AgentSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: AgentSession)

    @Update
    suspend fun update(session: AgentSession)

    @Query("SELECT * FROM agent_session WHERE sessionId = :sessionId LIMIT 1")
    suspend fun get(sessionId: String): AgentSession?

    @Query("SELECT * FROM agent_session ORDER BY startTime DESC")
    suspend fun getAll(): List<AgentSession>
}
