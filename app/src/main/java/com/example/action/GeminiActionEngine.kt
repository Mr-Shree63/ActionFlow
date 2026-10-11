package com.example.action

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiActionEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun parseWithGemini(input: String): ParsedAction? = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val systemPrompt = """
            You are an AI Action Engine for a personal productivity workflow app.
            Today's date is: $todayDate.
            User command: "$input".
            Convert this command into an actionable workflow action. Return ONLY raw JSON without markdown code fences.
            JSON Schema:
            {
              "actionType": "CREATE_TASK" | "COMPLETE_TASK" | "DELETE_TASK" | "CLEAR_COMPLETED" | "TRIGGER_WORKFLOW" | "RUN_AUTOMATIONS" | "FOCUS_SESSION" | "QUERY_TASKS" | "OPEN_APP" | "GAME_ACTIVITY",
              "speechReply": "Short friendly voice confirmation under 15 words",
              "workflowName": "",
              "targetQuery": "",
              "targetAppName": "",
              "activityGoal": "",
              "durationMinutes": 15,
              "focusMinutes": 25,
              "tasks": [
                {
                  "title": "Task title",
                  "category": "Work" | "Routine" | "Health" | "Focus" | "Personal" | "Game",
                  "priority": "HIGH" | "MEDIUM" | "LOW",
                  "scheduledDate": "YYYY-MM-DD",
                  "scheduledTime": "HH:MM AM/PM",
                  "associatedAppName": "",
                  "activityGoal": ""
                }
              ]
            }
        """.trimIndent()

        try {
            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", systemPrompt))
                        })
                    })
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext null
            }

            val responseBody = response.body?.string() ?: return@withContext null
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates") ?: return@withContext null
            if (candidates.length() == 0) return@withContext null

            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return@withContext null
            val parts = content.optJSONArray("parts") ?: return@withContext null
            if (parts.length() == 0) return@withContext null

            val text = parts.getJSONObject(0).optString("text")
            val cleanJson = text.replace("```json", "").replace("```", "").trim()
            val parsedObj = JSONObject(cleanJson)

            val actionTypeStr = parsedObj.optString("actionType", "CREATE_TASK")
            val actionType = try {
                ActionType.valueOf(actionTypeStr)
            } catch (e: Exception) {
                ActionType.CREATE_TASK
            }

            val speechReply = parsedObj.optString("speechReply", "Action processed.")
            val workflowName = parsedObj.optString("workflowName", "")
            val targetQuery = parsedObj.optString("targetQuery", "")
            val targetAppName = parsedObj.optString("targetAppName", "")
            val activityGoal = parsedObj.optString("activityGoal", "")
            val durationMinutes = parsedObj.optInt("durationMinutes", 15)
            val focusMinutes = parsedObj.optInt("focusMinutes", 25)

            val taskList = mutableListOf<ParsedTaskItem>()
            val tasksArray = parsedObj.optJSONArray("tasks")
            if (tasksArray != null) {
                for (i in 0 until tasksArray.length()) {
                    val t = tasksArray.getJSONObject(i)
                    taskList.add(
                        ParsedTaskItem(
                            title = t.optString("title", "Productivity Task"),
                            category = t.optString("category", "General"),
                            priority = t.optString("priority", "MEDIUM"),
                            scheduledDate = t.optString("scheduledDate", todayDate),
                            scheduledTime = t.optString("scheduledTime", ""),
                            associatedAppName = t.optString("associatedAppName", "").ifBlank { null },
                            activityGoal = t.optString("activityGoal", "").ifBlank { null }
                        )
                    )
                }
            }

            ParsedAction(
                type = actionType,
                tasksToCreate = taskList,
                targetQuery = targetQuery,
                workflowName = workflowName,
                targetAppName = targetAppName,
                activityGoal = activityGoal,
                durationMinutes = durationMinutes,
                focusMinutes = focusMinutes,
                rawInput = input,
                speechReply = speechReply,
                engineUsed = "GEMINI_AI"
            )
        } catch (e: Throwable) {
            null
        }
    }
}
