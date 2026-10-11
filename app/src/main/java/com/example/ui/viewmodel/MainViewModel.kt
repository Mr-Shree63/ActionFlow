package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.action.ActionDispatcher
import com.example.action.ActionResult
import com.example.action.ActionType
import com.example.action.AppInfo
import com.example.action.WorkflowAutomationService
import com.example.audio.SpeechManager
import com.example.data.db.ActionFlowDatabase
import com.example.data.model.ActionLogEntity
import com.example.data.model.TaskEntity
import com.example.data.model.WorkflowRuleEntity
import com.example.data.repository.ActionLogRepository
import com.example.data.repository.TaskRepository
import com.example.data.repository.WorkflowRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab(val title: String) {
    ACTION_HUB("Action Hub"),
    DAILY_TASKS("Daily Tasks"),
    WORKFLOWS("Workflows"),
    AUDIT_LOGS("History & Logs")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ActionFlowDatabase.getDatabase(application)
    private val taskRepository = TaskRepository(database.taskDao())
    private val workflowRepository = WorkflowRepository(database.workflowRuleDao())
    private val actionLogRepository = ActionLogRepository(database.actionLogDao())

    val automationService = WorkflowAutomationService(taskRepository, workflowRepository, actionLogRepository)
    private val actionDispatcher = ActionDispatcher(application, taskRepository, workflowRepository, actionLogRepository, automationService)
    val speechManager = SpeechManager(application)
    val appLauncherManager = actionDispatcher.appLauncherManager

    // Navigation
    private val _currentTab = MutableStateFlow(AppTab.ACTION_HUB)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Command console
    private val _commandInput = MutableStateFlow("")
    val commandInput: StateFlow<String> = _commandInput.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _lastActionResult = MutableStateFlow<ActionResult?>(null)
    val lastActionResult: StateFlow<ActionResult?> = _lastActionResult.asStateFlow()

    // Force Offline Mode toggle
    private val _forceOfflineMode = MutableStateFlow(false)
    val forceOfflineMode: StateFlow<Boolean> = _forceOfflineMode.asStateFlow()

    // Focus Session timer
    private val _isFocusActive = MutableStateFlow(false)
    val isFocusActive: StateFlow<Boolean> = _isFocusActive.asStateFlow()

    private val _focusRemainingSeconds = MutableStateFlow(25 * 60)
    val focusRemainingSeconds: StateFlow<Int> = _focusRemainingSeconds.asStateFlow()

    private var focusJob: Job? = null

    // Available Apps & Games Directory
    private val _availableApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val availableApps: StateFlow<List<AppInfo>> = _availableApps.asStateFlow()

    // Daily Tasks filtering
    private val _taskFilterCategory = MutableStateFlow("All")
    val taskFilterCategory: StateFlow<String> = _taskFilterCategory.asStateFlow()

    private val _taskSearchQuery = MutableStateFlow("")
    val taskSearchQuery: StateFlow<String> = _taskSearchQuery.asStateFlow()

    // Dialog states
    private val _editingTask = MutableStateFlow<TaskEntity?>(null)
    val editingTask: StateFlow<TaskEntity?> = _editingTask.asStateFlow()

    private val _showTaskDialog = MutableStateFlow(false)
    val showTaskDialog: StateFlow<Boolean> = _showTaskDialog.asStateFlow()

    private val _editingWorkflow = MutableStateFlow<WorkflowRuleEntity?>(null)
    val editingWorkflow: StateFlow<WorkflowRuleEntity?> = _editingWorkflow.asStateFlow()

    private val _showWorkflowDialog = MutableStateFlow(false)
    val showWorkflowDialog: StateFlow<Boolean> = _showWorkflowDialog.asStateFlow()

    // Live Data Streams
    val allTasks: StateFlow<List<TaskEntity>> = taskRepository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWorkflows: StateFlow<List<WorkflowRuleEntity>> = workflowRepository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<ActionLogEntity>> = actionLogRepository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered tasks for UI
    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        _taskFilterCategory,
        _taskSearchQuery
    ) { tasks, category, query ->
        tasks.filter { task ->
            val matchesCategory = category == "All" || task.category.equals(category, ignoreCase = true)
            val matchesQuery = query.isBlank() || task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Voice callback handler
        speechManager.onResultListener = { spokenText ->
            _commandInput.value = spokenText
            executeCommand(spokenText, isVoice = true)
        }

        // On launch: load installed and popular apps & evaluate today's scheduled workflows
        viewModelScope.launch {
            _availableApps.value = appLauncherManager.getInstalledOrAvailableApps()
            delay(400)
            automationService.evaluateAllScheduledAutomations()
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun updateCommandInput(text: String) {
        _commandInput.value = text
    }

    fun toggleForceOffline() {
        _forceOfflineMode.value = !_forceOfflineMode.value
    }

    fun toggleTts() {
        speechManager.toggleTts()
    }

    fun startVoiceInput() {
        speechManager.startListening()
    }

    fun stopVoiceInput() {
        speechManager.stopListening()
    }

    fun executeCommand(command: String = _commandInput.value, isVoice: Boolean = false) {
        val textToRun = command.trim()
        if (textToRun.isEmpty()) return

        viewModelScope.launch {
            _isExecuting.value = true
            val result = actionDispatcher.executeCommand(textToRun, isVoice = isVoice)
            _lastActionResult.value = result
            _isExecuting.value = false
            _commandInput.value = ""

            // Voice confirmation playback
            if (result.speechReply.isNotBlank()) {
                speechManager.speak(result.speechReply)
            }

            // Start focus session if command was a focus session
            if (result.actionType == ActionType.FOCUS_SESSION) {
                startFocusSession(25)
            }
        }
    }

    fun launchApp(app: AppInfo) {
        viewModelScope.launch {
            val launched = appLauncherManager.launchAppByPackage(app.packageName)
            val summary = if (launched) "Launched ${app.name}." else "Could not open ${app.name} on this device."
            val speech = "Opening ${app.name}."
            _lastActionResult.value = ActionResult(
                success = launched,
                summary = summary,
                speechReply = speech,
                actionType = ActionType.OPEN_APP,
                engineUsed = "OFFLINE_ENGINE",
                launchedAppName = app.name,
                launchedPackageName = app.packageName
            )
            speechManager.speak(speech)
        }
    }

    fun launchTaskApp(task: TaskEntity) {
        viewModelScope.launch {
            val appName = task.associatedAppName ?: return@launch
            val (launched, resolvedName) = appLauncherManager.launchAppByName(appName)
            val speech = if (launched) "Opening $resolvedName for your task." else "Could not open $resolvedName."
            speechManager.speak(speech)
        }
    }

    fun launchGameActivity(app: AppInfo, activityGoal: String, durationMinutes: Int = 15) {
        viewModelScope.launch {
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val timeText = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            val taskId = taskRepository.insertTask(
                TaskEntity(
                    title = "${app.name}: $activityGoal",
                    description = "In-app activity tracking session ($durationMinutes mins)",
                    category = if (app.isGame) "Game" else "App",
                    priority = "MEDIUM",
                    scheduledDate = todayDate,
                    scheduledTime = timeText,
                    source = "APP_ACTION",
                    associatedAppName = app.name,
                    associatedPackageName = app.packageName,
                    associatedActivityName = activityGoal,
                    activityDurationMinutes = durationMinutes
                )
            )

            appLauncherManager.launchAppByPackage(app.packageName)

            val summary = "Created activity task and launched ${app.name} for '$activityGoal'."
            val speech = "Opening ${app.name}. Enjoy your activity!"
            _lastActionResult.value = ActionResult(
                success = true,
                summary = summary,
                speechReply = speech,
                actionType = ActionType.GAME_ACTIVITY,
                createdTaskCount = 1,
                engineUsed = "OFFLINE_ENGINE",
                launchedAppName = app.name,
                undoneTaskIds = listOf(taskId)
            )
            speechManager.speak(speech)
        }
    }

    fun undoLastAction() {
        val lastResult = _lastActionResult.value ?: return
        viewModelScope.launch {
            if (lastResult.undoneTaskIds.isNotEmpty()) {
                for (id in lastResult.undoneTaskIds) {
                    taskRepository.deleteTaskById(id)
                }
                _lastActionResult.value = null
                speechManager.speak("Undid task creation.")
            }
        }
    }

    fun dismissActionResult() {
        _lastActionResult.value = null
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = !task.isCompleted,
                completedAt = if (!task.isCompleted) System.currentTimeMillis() else null
            )
            taskRepository.updateTask(updated)
            if (updated.isCompleted) {
                speechManager.speak("Done: ${task.title}")
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    fun runAutomationsNow() {
        viewModelScope.launch {
            _isExecuting.value = true
            val count = automationService.evaluateAllScheduledAutomations()
            val summary = if (count > 0) "Generated $count scheduled workflow tasks for today."
            else "All scheduled daily workflows are already up-to-date."
            _lastActionResult.value = ActionResult(
                success = true,
                summary = summary,
                speechReply = summary,
                actionType = ActionType.RUN_AUTOMATIONS,
                createdTaskCount = count,
                engineUsed = "OFFLINE_ENGINE"
            )
            speechManager.speak(summary)
            _isExecuting.value = false
        }
    }

    fun toggleWorkflowEnabled(rule: WorkflowRuleEntity) {
        viewModelScope.launch {
            workflowRepository.updateRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun triggerWorkflowNow(rule: WorkflowRuleEntity) {
        viewModelScope.launch {
            val count = automationService.triggerWorkflowRuleManually(rule)
            val summary = "Executed '${rule.title}', generated $count tasks."
            _lastActionResult.value = ActionResult(
                success = true,
                summary = summary,
                speechReply = summary,
                actionType = ActionType.TRIGGER_WORKFLOW,
                createdTaskCount = count,
                engineUsed = "OFFLINE_ENGINE"
            )
            speechManager.speak(summary)
        }
    }

    fun setTaskFilterCategory(category: String) {
        _taskFilterCategory.value = category
    }

    fun setTaskSearchQuery(query: String) {
        _taskSearchQuery.value = query
    }

    fun openAddTaskDialog(task: TaskEntity? = null) {
        _editingTask.value = task
        _showTaskDialog.value = true
    }

    fun closeTaskDialog() {
        _showTaskDialog.value = false
        _editingTask.value = null
    }

    fun saveTask(task: TaskEntity) {
        viewModelScope.launch {
            if (task.id == 0L) {
                taskRepository.insertTask(task)
            } else {
                taskRepository.updateTask(task)
            }
            closeTaskDialog()
        }
    }

    fun openAddWorkflowDialog(workflow: WorkflowRuleEntity? = null) {
        _editingWorkflow.value = workflow
        _showWorkflowDialog.value = true
    }

    fun closeWorkflowDialog() {
        _showWorkflowDialog.value = false
        _editingWorkflow.value = null
    }

    fun saveWorkflow(rule: WorkflowRuleEntity) {
        viewModelScope.launch {
            if (rule.id == 0L) {
                workflowRepository.insertRule(rule)
            } else {
                workflowRepository.updateRule(rule)
            }
            closeWorkflowDialog()
        }
    }

    fun deleteWorkflow(rule: WorkflowRuleEntity) {
        viewModelScope.launch {
            workflowRepository.deleteRule(rule)
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            actionLogRepository.clearLogs()
        }
    }

    // Focus Timer
    fun startFocusSession(minutes: Int = 25) {
        focusJob?.cancel()
        _isFocusActive.value = true
        _focusRemainingSeconds.value = minutes * 60
        focusJob = viewModelScope.launch {
            while (_focusRemainingSeconds.value > 0 && _isFocusActive.value) {
                delay(1000)
                _focusRemainingSeconds.value -= 1
            }
            if (_isFocusActive.value) {
                _isFocusActive.value = false
                speechManager.speak("Focus session complete. Well done!")
            }
        }
    }

    fun stopFocusSession() {
        focusJob?.cancel()
        _isFocusActive.value = false
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        focusJob?.cancel()
    }
}
