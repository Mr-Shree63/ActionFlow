package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TaskEntity
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditDialog(
    task: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (TaskEntity) -> Unit
) {
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var category by remember { mutableStateOf(task?.category ?: "Work") }
    var priority by remember { mutableStateOf(task?.priority ?: "MEDIUM") }
    var scheduledDate by remember { mutableStateOf(task?.scheduledDate ?: todayDate) }
    var scheduledTime by remember { mutableStateOf(task?.scheduledTime ?: "09:00 AM") }
    var associatedAppName by remember { mutableStateOf(task?.associatedAppName ?: "") }
    var associatedActivityName by remember { mutableStateOf(task?.associatedActivityName ?: "") }

    val categories = listOf("Work", "Game", "Routine", "Health", "Focus", "Personal")
    val priorities = listOf("HIGH", "MEDIUM", "LOW")
    val popularAppSuggestions = listOf("None", "Chess", "Spotify", "Duolingo", "YouTube", "Subway Surfers", "Calculator", "Chrome")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (task == null) "Schedule New Task / App Activity" else "Edit Task / Activity",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task / Activity Title *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                Text("Category", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                // App / Game Activity Link
                Text("Link App or Game (Optional Activity Action)", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    popularAppSuggestions.forEach { appSug ->
                        val isSelected = (appSug == "None" && associatedAppName.isBlank()) ||
                                (appSug != "None" && associatedAppName.equals(appSug, ignoreCase = true))
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (appSug == "None") {
                                    associatedAppName = ""
                                } else {
                                    associatedAppName = appSug
                                    if (appSug == "Chess" || appSug == "Subway Surfers") category = "Game"
                                    if (title.isBlank()) title = "Play $appSug session"
                                }
                            },
                            label = { Text(appSug) }
                        )
                    }
                }

                if (associatedAppName.isNotBlank()) {
                    OutlinedTextField(
                        value = associatedActivityName,
                        onValueChange = { associatedActivityName = it },
                        label = { Text("Activity Goal (e.g. Solve daily puzzle, 15m run)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Priority selector
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    priorities.forEach { prio ->
                        val pColor = when (prio) {
                            "HIGH" -> PriorityHigh
                            "MEDIUM" -> PriorityMedium
                            else -> PriorityLow
                        }
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = { Text(prio) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = pColor.copy(alpha = 0.2f),
                                selectedLabelColor = pColor
                            )
                        )
                    }
                }

                // Date & Time row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = scheduledDate,
                        onValueChange = { scheduledDate = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = scheduledTime,
                        onValueChange = { scheduledTime = it },
                        label = { Text("Time (e.g. 10:00 AM)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val app = associatedAppName.trim().ifBlank { null }
                        val act = associatedActivityName.trim().ifBlank { null }
                        val source = if (app != null) "APP_ACTION" else (task?.source ?: "MANUAL")

                        val toSave = task?.copy(
                            title = title.trim(),
                            description = description.trim(),
                            category = category,
                            priority = priority,
                            scheduledDate = scheduledDate.trim(),
                            scheduledTime = scheduledTime.trim(),
                            associatedAppName = app,
                            associatedActivityName = act,
                            source = source
                        ) ?: TaskEntity(
                            title = title.trim(),
                            description = description.trim(),
                            category = category,
                            priority = priority,
                            scheduledDate = scheduledDate.trim(),
                            scheduledTime = scheduledTime.trim(),
                            associatedAppName = app,
                            associatedActivityName = act,
                            source = source
                        )
                        onSave(toSave)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_task_button")
            ) {
                Text("Save Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
