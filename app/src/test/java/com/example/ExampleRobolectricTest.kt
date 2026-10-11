package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.action.ActionType
import com.example.action.OfflineActionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ActionFlow", appName)
  }

  @Test
  fun `offline action engine parses task creation command`() {
    val engine = OfflineActionEngine()
    val result = engine.parseCommand("Add task: Buy groceries at 5 PM with high priority")
    assertEquals(ActionType.CREATE_TASK, result.type)
    assertEquals("OFFLINE_ENGINE", result.engineUsed)
    assertTrue(result.tasksToCreate.isNotEmpty())
    val task = result.tasksToCreate.first()
    assertEquals("HIGH", task.priority)
    assertTrue(task.title.contains("Buy groceries", ignoreCase = true))
  }

  @Test
  fun `offline action engine parses workflow trigger`() {
    val engine = OfflineActionEngine()
    val result = engine.parseCommand("Run morning routine")
    assertEquals(ActionType.TRIGGER_WORKFLOW, result.type)
    assertEquals("Morning Kickoff Routine", result.workflowName)
  }

  @Test
  fun `offline action engine parses open app command`() {
    val engine = OfflineActionEngine()
    val result = engine.parseCommand("Open Spotify")
    assertEquals(ActionType.OPEN_APP, result.type)
    assertEquals("Spotify", result.targetAppName)
  }

  @Test
  fun `offline action engine parses in-game activity command`() {
    val engine = OfflineActionEngine()
    val result = engine.parseCommand("Do daily puzzle in Chess")
    assertEquals(ActionType.GAME_ACTIVITY, result.type)
    assertEquals("Chess", result.targetAppName)
    assertEquals("daily puzzle", result.activityGoal)
  }

  @Test
  fun `offline action engine parses play game command`() {
    val engine = OfflineActionEngine()
    val result = engine.parseCommand("Play Chess for 20 minutes")
    assertEquals(ActionType.GAME_ACTIVITY, result.type)
    assertEquals("Chess", result.targetAppName)
    assertEquals(20, result.durationMinutes)
  }
}
