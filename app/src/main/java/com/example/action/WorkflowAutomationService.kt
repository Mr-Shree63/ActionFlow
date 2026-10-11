package com.example.action

import com.example.data.model.ActionLogEntity
import com.example.data.model.TaskEntity
import com.example.data.model.WorkflowRuleEntity
import com.example.data.repository.ActionLogRepository
import com.example.data.repository.TaskRepository
import com.example.data.repository.WorkflowRepository
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WorkflowAutomationService(
    private val taskRepository: TaskRepository,
    private val workflowRepository: WorkflowRepository,
    private val actionLogRepository: ActionLogRepository
) {

    suspend fun evaluateAllScheduledAutomations(): Int {
        val enabledRules = workflowRepository.getEnabledRules()
        val todayStr = getTodayDate()
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY

        var totalTasksCreated = 0

        for (rule in enabledRules) {
            // Check recurrence
            val shouldRunToday = when (rule.recurrence.uppercase(Locale.getDefault())) {
                "DAILY" -> true
                "WEEKDAYS" -> !isWeekend
                "WEEKENDS" -> isWeekend
                else -> true
            }

            if (!shouldRunToday) continue

            // If already spawned today, skip to prevent duplicates
            if (rule.lastRunDate == todayStr) {
                // Already ran today
                continue
            }

            // Check if current time has passed or matches scheduled time
            // For a smooth experience, on launch or evaluation, any rule scheduled for today that hasn't run gets executed!
            val tasksCreated = spawnTasksForRule(rule, todayStr)
            if (tasksCreated > 0) {
                totalTasksCreated += tasksCreated
                workflowRepository.updateRule(rule.copy(lastRunDate = todayStr))
                actionLogRepository.insertLog(
                    ActionLogEntity(
                        commandText = "Scheduled Workflow Auto-Trigger: ${rule.title}",
                        actionType = "AUTO_WORKFLOW",
                        engineUsed = "OFFLINE_ENGINE",
                        resultSummary = "Automatically generated $tasksCreated productivity tasks for ${rule.title} at ${rule.scheduledTime}.",
                        isSuccess = true
                    )
                )
            }
        }

        return totalTasksCreated
    }

    suspend fun triggerWorkflowRuleManually(rule: WorkflowRuleEntity): Int {
        val todayStr = getTodayDate()
        val tasksCreated = spawnTasksForRule(rule, todayStr)
        workflowRepository.updateRule(rule.copy(lastRunDate = todayStr))

        actionLogRepository.insertLog(
            ActionLogEntity(
                commandText = "Manual Trigger: ${rule.title}",
                actionType = "AUTO_WORKFLOW",
                engineUsed = "OFFLINE_ENGINE",
                resultSummary = "Spawned $tasksCreated tasks for workflow '${rule.title}'.",
                isSuccess = true
            )
        )
        return tasksCreated
    }

    private suspend fun spawnTasksForRule(rule: WorkflowRuleEntity, todayStr: String): Int {
        val taskTemplates = parseTemplates(rule.taskTemplatesJson)
        if (taskTemplates.isEmpty()) return 0

        val entitiesToCreate = taskTemplates.map { templateTitle ->
            TaskEntity(
                title = templateTitle,
                description = "Automated step from [${rule.title}]",
                category = rule.category,
                priority = rule.priority,
                scheduledDate = todayStr,
                scheduledTime = formatTimeText(rule.scheduledTime),
                isCompleted = false,
                source = "AUTO_WORKFLOW",
                workflowRuleId = rule.id
            )
        }

        taskRepository.insertTasks(entitiesToCreate)
        return entitiesToCreate.size
    }

    private fun parseTemplates(jsonStr: String): List<String> {
        val results = mutableListOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val item = array.optString(i)
                if (item.isNotBlank()) results.add(item)
            }
        } catch (e: Exception) {
            // Fallback for comma separated text
            if (jsonStr.isNotBlank()) {
                results.addAll(jsonStr.split(",", "\n").map { it.trim() }.filter { it.isNotEmpty() })
            }
        }
        return results
    }

    private fun formatTimeText(hhmm: String): String {
        return try {
            val parts = hhmm.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val meridian = if (h >= 12) "PM" else "AM"
            val displayHour = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, m, meridian)
        } catch (e: Exception) {
            hhmm
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
