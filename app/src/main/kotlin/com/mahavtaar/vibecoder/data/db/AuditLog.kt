package com.mahavtaar.vibecoder.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "audit_log")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val stepNumber: Int,
    val eventType: String, // THOUGHT, ACTION_STARTED, ACTION_COMPLETED, FINAL_ANSWER, ERROR
    val toolName: String? = null,
    val inputJson: String? = null,
    val outputText: String? = null
)
