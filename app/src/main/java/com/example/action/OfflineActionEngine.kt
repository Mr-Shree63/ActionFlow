package com.example.action

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class OfflineActionEngine {

    fun parseCommand(input: String): ParsedAction {
        val cleanInput = input.trim()
        val lower = cleanInput.lowercase(Locale.getDefault())

        val todayDate = getFormattedDate(0)
        val tomorrowDate = getFormattedDate(1)

        // 1. Clear completed tasks
        if (lower.contains("clear completed") || lower.contains("delete completed") || lower.contains("clean up completed")) {
            return ParsedAction(
                type = ActionType.CLEAR_COMPLETED,
                rawInput = cleanInput,
                speechReply = "Clearing all completed tasks from your workspace.",
                engineUsed = "OFFLINE_ENGINE"
            )
        }

        // 2. Trigger automated workflows or all automations
        if (lower.contains("run automation") || lower.contains("trigger automation") || lower.contains("check automation")) {
            return ParsedAction(
                type = ActionType.RUN_AUTOMATIONS,
                rawInput = cleanInput,
                speechReply = "Evaluating and executing all scheduled daily workflow automations.",
                engineUsed = "OFFLINE_ENGINE"
            )
        }

        if (lower.contains("morning routine") || lower.contains("morning kickoff")) {
            return ParsedAction(
                type = ActionType.TRIGGER_WORKFLOW,
                workflowName = "Morning Kickoff Routine",
                rawInput = cleanInput,
                speechReply = "Triggering Morning Kickoff Routine workflow.",
                engineUsed = "OFFLINE_ENGINE"
            )
        }

        if (lower.contains("deep work") || lower.contains("focus block")) {
            return ParsedAction(
                type = ActionType.TRIGGER_WORKFLOW,
                workflowName = "Deep Work Focus Block",
                rawInput = cleanInput,
                speechReply = "Triggering Deep Work Focus Block workflow.",
                engineUsed = "OFFLINE_ENGINE"
            )
        }

        if (lower.contains("evening routine") || lower.contains("evening wind-down") || lower.contains("evening review")) {
            return ParsedAction(
                type = ActionType.TRIGGER_WORKFLOW,
                workflowName = "Evening Wind-Down & Review",
                rawInput = cleanInput,
                speechReply = "Triggering Evening Wind-Down & Review workflow.",
                engineUsed = "OFFLINE_ENGINE"
            )
        }

        if (lower.startsWith("run workflow ") || lower.startsWith("trigger workflow ")) {
            val name = cleanInput.substringAfter("workflow").trim()
            return ParsedAction(
                type = ActionType.TRIGGER_WORKFLOW,
                workflowName = name,
                rawInput = cleanInput,
                speechReply = "Triggering workflow: $name.",
                engineUsed = "OFFLINE_ENGINE"
            )
        }

        // 2b. In-App Game Activity Macro: "do [activity] in [game/app]" (e.g. "do daily puzzle in chess", "do daily quest in subway surfers")
        if (lower.startsWith("do ") && lower.contains(" in ")) {
            val activityPart = cleanInput.substring(3).substringBeforeLast(" in ").trim()
            val appPart = cleanInput.substringAfterLast(" in ").trim()
            if (activityPart.isNotEmpty() && appPart.isNotEmpty()) {
                val minMatcher = Pattern.compile("(\\d+)\\s*(?:min|minute)").matcher(lower)
                val duration = if (minMatcher.find()) minMatcher.group(1)?.toIntOrNull() ?: 15 else 15
                return ParsedAction(
                    type = ActionType.GAME_ACTIVITY,
                    targetAppName = appPart,
                    activityGoal = activityPart,
                    durationMinutes = duration,
                    rawInput = cleanInput,
                    speechReply = "Opening $appPart for activity: $activityPart.",
                    engineUsed = "OFFLINE_ENGINE"
                )
            }
        }

        // 2c. Play Game Command: "play [game] (for [X] minutes)"
        if (lower.startsWith("play ")) {
            var gamePart = cleanInput.substring(5).trim()
            var duration = 15
            val minMatcher = Pattern.compile("(\\d+)\\s*(?:min|minute)").matcher(gamePart.lowercase(Locale.getDefault()))
            if (minMatcher.find()) {
                duration = minMatcher.group(1)?.toIntOrNull() ?: 15
                gamePart = gamePart.replace(Regex("(?i)\\s*for\\s+\\d+.*$"), "").trim()
            }
            if (gamePart.isNotEmpty()) {
                return ParsedAction(
                    type = ActionType.GAME_ACTIVITY,
                    targetAppName = gamePart,
                    activityGoal = "Play $gamePart session",
                    durationMinutes = duration,
                    rawInput = cleanInput,
                    speechReply = "Opening $gamePart for a $duration minute gaming session.",
                    engineUsed = "OFFLINE_ENGINE"
                )
            }
        }

        // 2d. Open App Command: "open [app]" or "launch [app]"
        val openPrefixes = listOf("open app ", "open ", "launch app ", "launch ", "start app ")
        for (prefix in openPrefixes) {
            if (lower.startsWith(prefix) && !lower.startsWith("start focus")) {
                val appName = cleanInput.substring(prefix.length).trim()
                if (appName.isNotEmpty()) {
                    return ParsedAction(
                        type = ActionType.OPEN_APP,
                        targetAppName = appName,
                        rawInput = cleanInput,
                        speechReply = "Opening $appName.",
                        engineUsed = "OFFLINE_ENGINE"
                    )
                }
            }
        }

        // 3. Focus session / timer command
        if (lower.contains("focus") && (lower.contains("min") || lower.contains("minute") || lower.contains("pomodoro"))) {
            val minutesMatcher = Pattern.compile("(\\d+)\\s*(?:min|minute)").matcher(lower)
            val minutes = if (minutesMatcher.find()) {
                minutesMatcher.group(1)?.toIntOrNull() ?: 25
            } else 25
            return ParsedAction(
                type = ActionType.FOCUS_SESSION,
                focusMinutes = minutes,
                rawInput = cleanInput,
                speechReply = "Starting $minutes minute deep focus session.",
                engineUsed = "OFFLINE_ENGINE"
            )
        }

        // 4. Complete / Done task
        val completePrefixes = listOf(
            "complete task ", "complete ", "finish task ", "finish ",
            "mark done ", "mark as done ", "check off ", "done "
        )
        for (prefix in completePrefixes) {
            if (lower.startsWith(prefix)) {
                val target = cleanInput.substring(prefix.length).trim()
                if (target.isNotEmpty()) {
                    return ParsedAction(
                        type = ActionType.COMPLETE_TASK,
                        targetQuery = target,
                        rawInput = cleanInput,
                        speechReply = "Marking task '$target' as completed.",
                        engineUsed = "OFFLINE_ENGINE"
                    )
                }
            }
        }

        // 5. Delete task
        val deletePrefixes = listOf("delete task ", "delete ", "remove task ", "remove ")
        for (prefix in deletePrefixes) {
            if (lower.startsWith(prefix)) {
                val target = cleanInput.substring(prefix.length).trim()
                if (target.isNotEmpty()) {
                    return ParsedAction(
                        type = ActionType.DELETE_TASK,
                        targetQuery = target,
                        rawInput = cleanInput,
                        speechReply = "Deleting task '$target'.",
                        engineUsed = "OFFLINE_ENGINE"
                    )
                }
            }
        }

        // 6. Query / List tasks
        if (lower.contains("what are my tasks") || lower.contains("show tasks") ||
            lower.contains("list tasks") || lower.contains("show pending") || lower == "tasks"
        ) {
            return ParsedAction(
                type = ActionType.QUERY_TASKS,
                rawInput = cleanInput,
                speechReply = "Checking your scheduled tasks for today.",
                engineUsed = "OFFLINE_ENGINE"
            )
        }

        // 7. Multi-task creation parsing ("create tasks: task1, task2, task3" or "add tasks: ...")
        if (lower.contains("create tasks:") || lower.contains("add tasks:") || lower.contains("tasks:")) {
            val itemsStr = cleanInput.substringAfter(":").trim()
            val items = itemsStr.split(",", ";", " and ").map { it.trim() }.filter { it.isNotEmpty() }
            if (items.isNotEmpty()) {
                val parsedItems = items.map { itemText ->
                    parseSingleTask(itemText, todayDate, tomorrowDate)
                }
                return ParsedAction(
                    type = ActionType.CREATE_TASK,
                    tasksToCreate = parsedItems,
                    rawInput = cleanInput,
                    speechReply = "Created ${parsedItems.size} tasks for your workflow.",
                    engineUsed = "OFFLINE_ENGINE"
                )
            }
        }

        // 8. Single task creation
        var taskTitle = cleanInput
        val createPrefixes = listOf(
            "create a task to ", "create task to ", "create task ", "create a task ",
            "add a task to ", "add task to ", "add task ", "add a task ",
            "schedule task ", "schedule a task ", "schedule ",
            "remind me to ", "remind me ", "new task ", "todo ", "add "
        )

        for (prefix in createPrefixes) {
            if (lower.startsWith(prefix)) {
                taskTitle = cleanInput.substring(prefix.length).trim()
                break
            }
        }

        val singleParsed = parseSingleTask(taskTitle, todayDate, tomorrowDate)
        return ParsedAction(
            type = ActionType.CREATE_TASK,
            tasksToCreate = listOf(singleParsed),
            rawInput = cleanInput,
            speechReply = "Added task: ${singleParsed.title}" +
                    if (singleParsed.scheduledTime.isNotEmpty()) " at ${singleParsed.scheduledTime}" else "",
            engineUsed = "OFFLINE_ENGINE"
        )
    }

    private fun parseSingleTask(rawText: String, todayDate: String, tomorrowDate: String): ParsedTaskItem {
        var text = rawText.trim()

        // 1. Extract Priority
        var priority = "MEDIUM"
        val lower = text.lowercase(Locale.getDefault())
        when {
            lower.contains("urgent") || lower.contains("high priority") || lower.contains("priority high") || lower.contains("important") -> {
                priority = "HIGH"
                text = text.replace(Regex("(?i)\\b(urgent|high priority|priority high|important)\\b"), "").trim()
            }
            lower.contains("low priority") || lower.contains("priority low") -> {
                priority = "LOW"
                text = text.replace(Regex("(?i)\\b(low priority|priority low)\\b"), "").trim()
            }
            lower.contains("medium priority") || lower.contains("priority medium") -> {
                priority = "MEDIUM"
                text = text.replace(Regex("(?i)\\b(medium priority|priority medium)\\b"), "").trim()
            }
        }

        // 2. Extract Category & App link detection
        var category = "General"
        var appName: String? = null
        var activityGoal: String? = null

        when {
            lower.contains("chess") -> {
                category = "Game"
                appName = "Chess"
                activityGoal = "Solve daily chess puzzle & play match"
            }
            lower.contains("subway surfer") -> {
                category = "Game"
                appName = "Subway Surfers"
                activityGoal = "Complete daily high-score run"
            }
            lower.contains("minecraft") -> {
                category = "Game"
                appName = "Minecraft"
                activityGoal = "Build and adventure"
            }
            lower.contains("roblox") -> {
                category = "Game"
                appName = "Roblox"
                activityGoal = "Play community mini-game"
            }
            lower.contains("duolingo") -> {
                category = "Routine"
                appName = "Duolingo"
                activityGoal = "Complete daily language streak"
            }
            lower.contains("spotify") -> {
                category = "Focus"
                appName = "Spotify"
                activityGoal = "Play deep focus music"
            }
            lower.contains("youtube") -> {
                category = "Media"
                appName = "YouTube"
                activityGoal = "Watch learning/fitness video"
            }
            lower.contains("game") -> category = "Game"
            lower.contains("work") || lower.contains("client") || lower.contains("meeting") || lower.contains("project") || lower.contains("email") -> category = "Work"
            lower.contains("health") || lower.contains("workout") || lower.contains("exercise") || lower.contains("gym") || lower.contains("water") || lower.contains("walk") -> category = "Health"
            lower.contains("routine") || lower.contains("morning") || lower.contains("evening") || lower.contains("habit") -> category = "Routine"
            lower.contains("focus") || lower.contains("deep work") || lower.contains("study") || lower.contains("read") || lower.contains("code") -> category = "Focus"
            lower.contains("personal") || lower.contains("groceries") || lower.contains("buy") || lower.contains("call") || lower.contains("home") -> category = "Personal"
        }

        // 3. Extract Date
        var scheduledDate = todayDate
        if (text.contains("tomorrow", ignoreCase = true)) {
            scheduledDate = tomorrowDate
            text = text.replace(Regex("(?i)\\btomorrow\\b"), "").trim()
        } else if (text.contains("today", ignoreCase = true)) {
            scheduledDate = todayDate
            text = text.replace(Regex("(?i)\\btoday\\b"), "").trim()
        } else if (text.contains("tonight", ignoreCase = true)) {
            scheduledDate = todayDate
            text = text.replace(Regex("(?i)\\btonight\\b"), "").trim()
        }

        // 4. Extract Time (e.g., "at 5pm", "at 5:30 pm", "at 14:00", "5pm", "9:00 am")
        var scheduledTime = ""
        val timePattern = Pattern.compile("(?i)(?:at\\s+)?(\\b\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)\\b|\\b\\d{1,2}:\\d{2}\\b)")
        val timeMatcher = timePattern.matcher(text)
        if (timeMatcher.find()) {
            scheduledTime = timeMatcher.group(1)?.uppercase(Locale.getDefault()) ?: ""
            if (!scheduledTime.contains(":") && (scheduledTime.endsWith("AM") || scheduledTime.endsWith("PM"))) {
                val num = scheduledTime.substring(0, scheduledTime.length - 2).trim()
                val meridian = scheduledTime.takeLast(2)
                scheduledTime = String.format(Locale.getDefault(), "%02d:00 %s", num.toIntOrNull() ?: 12, meridian)
            }
            text = text.replace(timeMatcher.group(0) ?: "", "").trim()
        }

        // Clean up common remaining connectors like "for", "at", extra commas
        text = text.replace(Regex("^(?:for|at|to)\\s+"), "")
            .replace(Regex("\\s+(?:for|at)\\s*$"), "")
            .trim()

        if (text.isEmpty()) {
            text = "Productivity Task"
        }

        val capitalizedTitle = text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        return ParsedTaskItem(
            title = capitalizedTitle,
            category = category,
            priority = priority,
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime,
            associatedAppName = appName,
            activityGoal = activityGoal
        )
    }

    private fun getFormattedDate(daysOffset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, daysOffset)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
}
