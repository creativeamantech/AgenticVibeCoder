package com.mahavtaar.vibecoder.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_session")
data class AgentSession(
    @PrimaryKey val sessionId: String,
    val taskDescription: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val status: String = "RUNNING", // RUNNING, COMPLETED, FAILED
    val stepsCount: Int = 0
)
