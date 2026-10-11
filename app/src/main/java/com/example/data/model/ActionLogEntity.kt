package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_logs")
data class ActionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val commandText: String,
    val actionType: String, // CREATE_TASK, COMPLETE_TASK, DELETE_TASK, AUTO_WORKFLOW, SUMMARY, FOCUS
    val engineUsed: String, // OFFLINE_ENGINE, GEMINI_AI
    val resultSummary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
)
