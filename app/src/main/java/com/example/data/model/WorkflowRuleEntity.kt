package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workflow_rules")
data class WorkflowRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val scheduledTime: String = "08:00", // "HH:mm" (24h format for auto evaluation)
    val recurrence: String = "DAILY", // DAILY, WEEKDAYS, WEEKENDS
    val isEnabled: Boolean = true,
    val category: String = "Routine", // Routine, Work, Health, Focus, Personal
    val priority: String = "HIGH", // HIGH, MEDIUM, LOW
    val taskTemplatesJson: String = "[]", // JSON array of subtask titles to generate
    val lastRunDate: String = "" // YYYY-MM-DD to avoid duplicate spawning on the same day
)
