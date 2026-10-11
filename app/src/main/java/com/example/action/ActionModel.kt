package com.example.action

enum class ActionType {
    CREATE_TASK,
    COMPLETE_TASK,
    DELETE_TASK,
    CLEAR_COMPLETED,
    TRIGGER_WORKFLOW,
    RUN_AUTOMATIONS,
    QUERY_TASKS,
    FOCUS_SESSION,
    OPEN_APP,
    GAME_ACTIVITY,
    UNKNOWN
}

data class ParsedTaskItem(
    val title: String,
    val category: String = "General",
    val priority: String = "MEDIUM",
    val scheduledDate: String, // YYYY-MM-DD
    val scheduledTime: String = "",
    val associatedAppName: String? = null,
    val associatedPackageName: String? = null,
    val activityGoal: String? = null,
    val durationMinutes: Int = 0
)

data class ParsedAction(
    val type: ActionType,
    val tasksToCreate: List<ParsedTaskItem> = emptyList(),
    val targetQuery: String = "",
    val workflowName: String = "",
    val focusMinutes: Int = 25,
    val targetAppName: String = "",
    val targetPackageName: String = "",
    val activityGoal: String = "",
    val durationMinutes: Int = 0,
    val rawInput: String = "",
    val speechReply: String = "",
    val engineUsed: String = "OFFLINE_ENGINE"
)

data class ActionResult(
    val success: Boolean,
    val summary: String,
    val speechReply: String,
    val actionType: ActionType,
    val createdTaskCount: Int = 0,
    val modifiedTaskCount: Int = 0,
    val engineUsed: String = "OFFLINE_ENGINE",
    val undoneTaskIds: List<Long> = emptyList(),
    val launchedAppName: String? = null,
    val launchedPackageName: String? = null
)
