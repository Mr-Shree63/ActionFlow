package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.WorkflowRuleEntity
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import org.json.JSONArray

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkflowEditDialog(
    workflow: WorkflowRuleEntity?,
    onDismiss: () -> Unit,
    onSave: (WorkflowRuleEntity) -> Unit
) {
    var title by remember { mutableStateOf(workflow?.title ?: "") }
    var description by remember { mutableStateOf(workflow?.description ?: "") }
    var scheduledTime by remember { mutableStateOf(workflow?.scheduledTime ?: "08:30") }
    var recurrence by remember { mutableStateOf(workflow?.recurrence ?: "DAILY") }
    var category by remember { mutableStateOf(workflow?.category ?: "Routine") }
    var priority by remember { mutableStateOf(workflow?.priority ?: "HIGH") }

    // Convert JSON templates to lines for editing
    var templatesText by remember {
        mutableStateOf(
            if (workflow != null) {
                try {
                    val arr = JSONArray(workflow.taskTemplatesJson)
                    val list = mutableListOf<String>()
                    for (i in 0 until arr.length()) list.add(arr.getString(i))
                    list.joinToString("\n")
                } catch (e: Exception) {
                    workflow.taskTemplatesJson
                }
            } else {
                "Drink 500ml water\nReview top 3 daily objectives\n10-minute stretch"
            }
        )
    }

    val recurrences = listOf("DAILY", "WEEKDAYS", "WEEKENDS")
    val categories = listOf("Routine", "Work", "Health", "Focus", "Personal")
    val priorities = listOf("HIGH", "MEDIUM", "LOW")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (workflow == null) "Create Scheduled Workflow" else "Edit Workflow Automation",
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
                    label = { Text("Workflow Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workflow_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Workflow Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = scheduledTime,
                    onValueChange = { scheduledTime = it },
                    label = { Text("Trigger Time (HH:mm 24h, e.g. 07:30)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Recurrence
                Text("Recurrence Schedule", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recurrences.forEach { rec ->
                        FilterChip(
                            selected = recurrence == rec,
                            onClick = { recurrence = rec },
                            label = { Text(rec) }
                        )
                    }
                }

                // Category
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

                // Priority
                Text("Task Priority", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                // Task templates
                OutlinedTextField(
                    value = templatesText,
                    onValueChange = { templatesText = it },
                    label = { Text("Auto-Generated Tasks (1 per line)") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val lines = templatesText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                        val jsonArr = JSONArray()
                        lines.forEach { jsonArr.put(it) }

                        val toSave = workflow?.copy(
                            title = title.trim(),
                            description = description.trim(),
                            scheduledTime = scheduledTime.trim(),
                            recurrence = recurrence,
                            category = category,
                            priority = priority,
                            taskTemplatesJson = jsonArr.toString()
                        ) ?: WorkflowRuleEntity(
                            title = title.trim(),
                            description = description.trim(),
                            scheduledTime = scheduledTime.trim(),
                            recurrence = recurrence,
                            category = category,
                            priority = priority,
                            taskTemplatesJson = jsonArr.toString()
                        )
                        onSave(toSave)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_workflow_button")
            ) {
                Text("Save Workflow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
