package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "General", // Work, Routine, Health, Focus, Personal, Game
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val scheduledDate: String, // YYYY-MM-DD
    val scheduledTime: String = "", // e.g. "09:00 AM", "14:30"
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val source: String = "MANUAL", // VOICE_COMMAND, TEXT_COMMAND, AUTO_WORKFLOW, MANUAL, APP_ACTION
    val workflowRuleId: Long? = null,
    val associatedAppName: String? = null, // e.g. "Chess", "Spotify", "Duolingo"
    val associatedPackageName: String? = null, // e.g. "com.chess"
    val associatedActivityName: String? = null, // e.g. "Solve 3 daily chess puzzles"
    val activityDurationMinutes: Int = 0
)
