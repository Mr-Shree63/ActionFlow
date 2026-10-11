package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ActionLogDao
import com.example.data.dao.TaskDao
import com.example.data.dao.WorkflowRuleDao
import com.example.data.model.ActionLogEntity
import com.example.data.model.TaskEntity
import com.example.data.model.WorkflowRuleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [TaskEntity::class, WorkflowRuleEntity::class, ActionLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ActionFlowDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun workflowRuleDao(): WorkflowRuleDao
    abstract fun actionLogDao(): ActionLogDao

    companion object {
        @Volatile
        private var INSTANCE: ActionFlowDatabase? = null

        fun getDatabase(context: Context): ActionFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ActionFlowDatabase::class.java,
                    "actionflow_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialData(database)
                    }
                }
            }

            private suspend fun seedInitialData(database: ActionFlowDatabase) {
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // Pre-seed productivity workflows
                val defaultRules = listOf(
                    WorkflowRuleEntity(
                        title = "Morning Kickoff Routine",
                        description = "Jumpstart your day with mindfulness, water, and top 3 priorities.",
                        scheduledTime = "07:30",
                        recurrence = "DAILY",
                        isEnabled = true,
                        category = "Routine",
                        priority = "HIGH",
                        taskTemplatesJson = "[\"Hydrate: Drink 500ml fresh water\",\"Plan: Identify today's top 3 needle-movers\",\"Review: Check scheduled calendar\"]"
                    ),
                    WorkflowRuleEntity(
                        title = "Deep Work Focus Block",
                        description = "Distraction-free high leverage work session.",
                        scheduledTime = "09:30",
                        recurrence = "WEEKDAYS",
                        isEnabled = true,
                        category = "Focus",
                        priority = "HIGH",
                        taskTemplatesJson = "[\"Deep Work: Focus 90 mins without notifications\",\"Review: Verify core sprint milestone deliverables\"]"
                    ),
                    WorkflowRuleEntity(
                        title = "Midday Productivity Reset",
                        description = "Quick status check, hydration, and afternoon re-alignment.",
                        scheduledTime = "13:30",
                        recurrence = "WEEKDAYS",
                        isEnabled = true,
                        category = "Health",
                        priority = "MEDIUM",
                        taskTemplatesJson = "[\"Health: 15-min posture stretch & movement\",\"Inbox Zero: Process pending messages\"]"
                    ),
                    WorkflowRuleEntity(
                        title = "Evening Wind-Down & Review",
                        description = "Reflect on accomplishments and stage tomorrow for success.",
                        scheduledTime = "20:30",
                        recurrence = "DAILY",
                        isEnabled = true,
                        category = "Personal",
                        priority = "MEDIUM",
                        taskTemplatesJson = "[\"Reflection: Log today's achievements and lessons\",\"Prep: Draft tomorrow's primary agenda\"]"
                    )
                )
                database.workflowRuleDao().insertRules(defaultRules)

                // Pre-seed tasks for today including game and app activities
                val defaultTasks = listOf(
                    TaskEntity(
                        title = "Morning Kickoff: Hydrate & Plan",
                        description = "Drink 500ml water and write down top goals.",
                        category = "Routine",
                        priority = "HIGH",
                        scheduledDate = todayDate,
                        scheduledTime = "07:30 AM",
                        isCompleted = true,
                        source = "AUTO_WORKFLOW"
                    ),
                    TaskEntity(
                        title = "Daily Chess: Solve 3 tactics puzzles",
                        description = "Sharpen strategic thinking with daily chess puzzle activity.",
                        category = "Game",
                        priority = "MEDIUM",
                        scheduledDate = todayDate,
                        scheduledTime = "08:30 AM",
                        isCompleted = false,
                        source = "APP_ACTION",
                        associatedAppName = "Chess",
                        associatedPackageName = "com.chess",
                        associatedActivityName = "Solve 3 Daily Tactics Puzzles",
                        activityDurationMinutes = 15
                    ),
                    TaskEntity(
                        title = "Deep Focus: Listen to ambient soundflow",
                        description = "Launch focus playlist on Spotify.",
                        category = "Focus",
                        priority = "HIGH",
                        scheduledDate = todayDate,
                        scheduledTime = "10:00 AM",
                        isCompleted = false,
                        source = "APP_ACTION",
                        associatedAppName = "Spotify",
                        associatedPackageName = "com.spotify.music",
                        associatedActivityName = "Deep Focus Audio Session",
                        activityDurationMinutes = 45
                    ),
                    TaskEntity(
                        title = "Review product requirements & roadmap",
                        description = "Finalize user story mapping and action items.",
                        category = "Work",
                        priority = "HIGH",
                        scheduledDate = todayDate,
                        scheduledTime = "01:30 PM",
                        isCompleted = false,
                        source = "TEXT_COMMAND"
                    ),
                    TaskEntity(
                        title = "Duolingo: 10-minute language practice",
                        description = "Daily streak language challenge in Duolingo.",
                        category = "Routine",
                        priority = "MEDIUM",
                        scheduledDate = todayDate,
                        scheduledTime = "05:00 PM",
                        isCompleted = false,
                        source = "APP_ACTION",
                        associatedAppName = "Duolingo",
                        associatedPackageName = "com.duolingo",
                        associatedActivityName = "Daily Language Streak Drill",
                        activityDurationMinutes = 10
                    ),
                    TaskEntity(
                        title = "30-minute cardio & stretching",
                        description = "Daily health and physical recovery routine.",
                        category = "Health",
                        priority = "LOW",
                        scheduledDate = todayDate,
                        scheduledTime = "06:30 PM",
                        isCompleted = false,
                        source = "AUTO_WORKFLOW"
                    )
                )
                database.taskDao().insertTasks(defaultTasks)

                // Pre-seed an initial action log
                database.actionLogDao().insertLog(
                    ActionLogEntity(
                        commandText = "System initialized ActionFlow offline engine",
                        actionType = "SYSTEM_INIT",
                        engineUsed = "OFFLINE_ENGINE",
                        resultSummary = "ActionFlow initialized with 4 automated workflows and offline task executor.",
                        isSuccess = true
                    )
                )
            }
        }
    }
}
