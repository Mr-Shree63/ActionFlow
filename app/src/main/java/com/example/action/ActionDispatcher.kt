package com.example.action

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.model.ActionLogEntity
import com.example.data.model.TaskEntity
import com.example.data.repository.ActionLogRepository
import com.example.data.repository.TaskRepository
import com.example.data.repository.WorkflowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActionDispatcher(
    private val context: Context,
    private val taskRepository: TaskRepository,
    private val workflowRepository: WorkflowRepository,
    private val actionLogRepository: ActionLogRepository,
    private val automationService: WorkflowAutomationService,
    val appLauncherManager: AppLauncherManager = AppLauncherManager(context)
) {
    private val offlineEngine = OfflineActionEngine()
    private val geminiEngine = GeminiActionEngine()

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun executeCommand(input: String, isVoice: Boolean = false): ActionResult = withContext(Dispatchers.IO) {
        val cleanInput = input.trim()
        if (cleanInput.isEmpty()) {
            return@withContext ActionResult(
                success = false,
                summary = "Command was empty.",
                speechReply = "I didn't catch that. Please state a task or command.",
                actionType = ActionType.UNKNOWN
            )
        }

        // Dual Engine evaluation:
        // Try Gemini if online, otherwise use 100% Offline Engine
        var parsedAction: ParsedAction? = null
        var usedEngine = "OFFLINE_ENGINE"

        if (isNetworkAvailable()) {
            try {
                parsedAction = geminiEngine.parseWithGemini(cleanInput)
                if (parsedAction != null) {
                    usedEngine = "GEMINI_AI"
                }
            } catch (e: Exception) {
                parsedAction = null
            }
        }

        if (parsedAction == null) {
            parsedAction = offlineEngine.parseCommand(cleanInput)
            usedEngine = "OFFLINE_ENGINE"
        }

        // Execute action against database and device apps
        val result = when (parsedAction.type) {
            ActionType.OPEN_APP -> {
                val appToOpen = parsedAction.targetAppName.ifBlank { "App" }
                val (launched, resolvedName) = appLauncherManager.launchAppByName(appToOpen)
                val summary = if (launched) "Successfully launched '$resolvedName'."
                else "Attempted to open '$appToOpen'."
                val speech = if (launched) "Opening $resolvedName."
                else "Could not open $appToOpen on this device."

                ActionResult(
                    success = launched,
                    summary = summary,
                    speechReply = speech,
                    actionType = ActionType.OPEN_APP,
                    engineUsed = usedEngine,
                    launchedAppName = resolvedName
                )
            }

            ActionType.GAME_ACTIVITY -> {
                val appToOpen = parsedAction.targetAppName.ifBlank { "Game" }
                val (launched, resolvedName) = appLauncherManager.launchAppByName(appToOpen)
                val activityDesc = parsedAction.activityGoal.ifBlank { "Activity session in $resolvedName" }
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // Auto-create scheduled task in the daily productivity plan
                val taskId = taskRepository.insertTask(
                    TaskEntity(
                        title = "$resolvedName: $activityDesc",
                        description = "Automated in-app activity tracking session (${parsedAction.durationMinutes} mins)",
                        category = "Game",
                        priority = "MEDIUM",
                        scheduledDate = todayDate,
                        scheduledTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
                        source = "APP_ACTION",
                        associatedAppName = resolvedName,
                        associatedActivityName = activityDesc,
                        activityDurationMinutes = parsedAction.durationMinutes
                    )
                )

                val summary = "Created activity task & launched '$resolvedName' for '$activityDesc' (${parsedAction.durationMinutes}m)."
                val speech = "Opening $resolvedName for $activityDesc."

                ActionResult(
                    success = true,
                    summary = summary,
                    speechReply = speech,
                    actionType = ActionType.GAME_ACTIVITY,
                    createdTaskCount = 1,
                    engineUsed = usedEngine,
                    launchedAppName = resolvedName,
                    undoneTaskIds = listOf(taskId)
                )
            }

            ActionType.CREATE_TASK -> {
                val sourceTag = if (isVoice) "VOICE_COMMAND" else "TEXT_COMMAND"
                val taskIds = mutableListOf<Long>()
                for (item in parsedAction.tasksToCreate) {
                    val id = taskRepository.insertTask(
                        TaskEntity(
                            title = item.title,
                            category = item.category,
                            priority = item.priority,
                            scheduledDate = item.scheduledDate,
                            scheduledTime = item.scheduledTime,
                            source = sourceTag,
                            associatedAppName = item.associatedAppName,
                            associatedPackageName = item.associatedPackageName,
                            associatedActivityName = item.activityGoal,
                            activityDurationMinutes = item.durationMinutes
                        )
                    )
                    taskIds.add(id)
                }

                val count = parsedAction.tasksToCreate.size
                val summary = if (count == 1) {
                    val item = parsedAction.tasksToCreate.first()
                    "Created task '${item.title}' (${item.priority} Priority, ${item.category})" +
                            if (item.scheduledTime.isNotEmpty()) " at ${item.scheduledTime}" else ""
                } else {
                    "Created $count tasks: " + parsedAction.tasksToCreate.joinToString(", ") { it.title }
                }

                val speech = parsedAction.speechReply.ifBlank {
                    if (count == 1) "Created task: ${parsedAction.tasksToCreate.first().title}"
                    else "Added $count tasks to your workflow."
                }

                ActionResult(
                    success = true,
                    summary = summary,
                    speechReply = speech,
                    actionType = ActionType.CREATE_TASK,
                    createdTaskCount = count,
                    engineUsed = usedEngine,
                    undoneTaskIds = taskIds
                )
            }

            ActionType.COMPLETE_TASK -> {
                val allTasks = taskRepository.allTasks.first()
                val target = parsedAction.targetQuery.lowercase(Locale.getDefault())

                val match = allTasks.firstOrNull { !it.isCompleted && it.title.lowercase(Locale.getDefault()).contains(target) }
                    ?: allTasks.firstOrNull { it.title.lowercase(Locale.getDefault()).contains(target) }

                if (match != null) {
                    taskRepository.updateTask(
                        match.copy(
                            isCompleted = true,
                            completedAt = System.currentTimeMillis()
                        )
                    )
                    ActionResult(
                        success = true,
                        summary = "Completed task: '${match.title}'.",
                        speechReply = "Marked '${match.title}' as completed.",
                        actionType = ActionType.COMPLETE_TASK,
                        modifiedTaskCount = 1,
                        engineUsed = usedEngine
                    )
                } else {
                    ActionResult(
                        success = false,
                        summary = "Could not find a task matching '$target'.",
                        speechReply = "I couldn't find a task matching $target.",
                        actionType = ActionType.COMPLETE_TASK,
                        engineUsed = usedEngine
                    )
                }
            }

            ActionType.DELETE_TASK -> {
                val allTasks = taskRepository.allTasks.first()
                val target = parsedAction.targetQuery.lowercase(Locale.getDefault())
                val match = allTasks.firstOrNull { it.title.lowercase(Locale.getDefault()).contains(target) }

                if (match != null) {
                    taskRepository.deleteTask(match)
                    ActionResult(
                        success = true,
                        summary = "Deleted task: '${match.title}'.",
                        speechReply = "Deleted task '${match.title}'.",
                        actionType = ActionType.DELETE_TASK,
                        modifiedTaskCount = 1,
                        engineUsed = usedEngine
                    )
                } else {
                    ActionResult(
                        success = false,
                        summary = "No task found matching '$target' to delete.",
                        speechReply = "No matching task found to delete.",
                        actionType = ActionType.DELETE_TASK,
                        engineUsed = usedEngine
                    )
                }
            }

            ActionType.CLEAR_COMPLETED -> {
                val deletedCount = taskRepository.deleteCompletedTasks()
                ActionResult(
                    success = true,
                    summary = "Cleared $deletedCount completed tasks.",
                    speechReply = "Cleared $deletedCount completed tasks.",
                    actionType = ActionType.CLEAR_COMPLETED,
                    modifiedTaskCount = deletedCount,
                    engineUsed = usedEngine
                )
            }

            ActionType.TRIGGER_WORKFLOW -> {
                val rules = workflowRepository.getEnabledRules()
                val targetName = parsedAction.workflowName.lowercase(Locale.getDefault())
                val rule = rules.firstOrNull { it.title.lowercase(Locale.getDefault()).contains(targetName) }

                if (rule != null) {
                    val spawned = automationService.triggerWorkflowRuleManually(rule)
                    ActionResult(
                        success = true,
                        summary = "Workflow '${rule.title}' executed ($spawned tasks created).",
                        speechReply = "Workflow '${rule.title}' executed. $spawned tasks created.",
                        actionType = ActionType.TRIGGER_WORKFLOW,
                        createdTaskCount = spawned,
                        engineUsed = usedEngine
                    )
                } else {
                    ActionResult(
                        success = false,
                        summary = "No workflow found matching '${parsedAction.workflowName}'.",
                        speechReply = "Workflow '${parsedAction.workflowName}' not found.",
                        actionType = ActionType.TRIGGER_WORKFLOW,
                        engineUsed = usedEngine
                    )
                }
            }

            ActionType.RUN_AUTOMATIONS -> {
                val spawned = automationService.evaluateAllScheduledAutomations()
                val summary = if (spawned > 0) "Automations evaluated: $spawned new tasks scheduled for today."
                else "All scheduled automations are up-to-date for today."
                val speech = if (spawned > 0) "Generated $spawned scheduled tasks for today's workflow."
                else "Your scheduled daily workflow is already up to date."

                ActionResult(
                    success = true,
                    summary = summary,
                    speechReply = speech,
                    actionType = ActionType.RUN_AUTOMATIONS,
                    createdTaskCount = spawned,
                    engineUsed = usedEngine
                )
            }

            ActionType.FOCUS_SESSION -> {
                ActionResult(
                    success = true,
                    summary = "Focus timer ready: ${parsedAction.focusMinutes} minutes.",
                    speechReply = "Focus session for ${parsedAction.focusMinutes} minutes is active.",
                    actionType = ActionType.FOCUS_SESSION,
                    engineUsed = usedEngine
                )
            }

            ActionType.QUERY_TASKS -> {
                val pending = taskRepository.pendingTasks.first()
                val summary = if (pending.isEmpty()) "You have 0 pending tasks! You are all caught up."
                else "You have ${pending.size} pending tasks. Next up: '${pending.first().title}'."
                val speech = if (pending.isEmpty()) "You have no pending tasks. Great job!"
                else "You have ${pending.size} pending tasks. Top priority is ${pending.first().title}."

                ActionResult(
                    success = true,
                    summary = summary,
                    speechReply = speech,
                    actionType = ActionType.QUERY_TASKS,
                    engineUsed = usedEngine
                )
            }

            ActionType.UNKNOWN -> {
                ActionResult(
                    success = false,
                    summary = "Could not parse command: '$cleanInput'.",
                    speechReply = "I could not understand that command.",
                    actionType = ActionType.UNKNOWN,
                    engineUsed = usedEngine
                )
            }
        }

        // Record in ActionLog table
        actionLogRepository.insertLog(
            ActionLogEntity(
                commandText = cleanInput,
                actionType = result.actionType.name,
                engineUsed = result.engineUsed,
                resultSummary = result.summary,
                isSuccess = result.success
            )
        )

        result
    }
}
