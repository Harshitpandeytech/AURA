package com.aurviz.personalagent.presentation.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurviz.personalagent.domain.model.ScheduledTask
import com.aurviz.personalagent.presentation.components.EmptyState
import com.aurviz.personalagent.presentation.components.ErrorState
import com.aurviz.personalagent.presentation.components.LoadingState
import com.aurviz.personalagent.presentation.navigation.auraViewModel
import com.aurviz.personalagent.utils.UiState
import com.aurviz.personalagent.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: TasksViewModel = auraViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Scheduled Tasks", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            actions = {
                androidx.compose.material3.IconButton(onClick = { showAdd = true }) {
                    androidx.compose.material3.Icon(Icons.Default.Add, contentDescription = "Add task")
                }
            }
        )

        when (val state = uiState) {
            UiState.Idle, UiState.Loading -> LoadingState("Loading tasks")
            is UiState.Error -> ErrorState(state.message, onRetry = viewModel::refresh)
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Schedule,
                        title = "No scheduled tasks",
                        message = "Scheduled tasks will appear here."
                    )
                } else {
                    TaskList(
                        tasks = state.data,
                        onEnabledChange = viewModel::setEnabled,
                        onDelete = viewModel::delete
                    )
                }
            }
        }
    }
    if (showAdd) {
        ScheduleDialog(
            onDismiss = { showAdd = false },
            onSchedule = {
                viewModel.schedule(it)
                showAdd = false
            }
        )
    }
}

@Composable
private fun TaskList(
    tasks: List<ScheduledTask>,
    onEnabledChange: (ScheduledTask, Boolean) -> Unit,
    onDelete: (ScheduledTask) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.instruction, fontWeight = FontWeight.SemiBold)
                        Text(
                            task.scheduleExpression,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Next: ${DateTimeUtils.formatDisplay(task.nextRunAt)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "Last: ${task.lastExecutionAt?.let(DateTimeUtils::formatDisplay) ?: "Never"}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Switch(
                        checked = task.enabled,
                        onCheckedChange = { enabled -> onEnabledChange(task, enabled) }
                    )
                    androidx.compose.material3.IconButton(onClick = { onDelete(task) }) {
                        androidx.compose.material3.Icon(Icons.Default.Delete, contentDescription = "Delete task")
                    }
                }

            }
        }
    }
}

@Composable
private fun ScheduleDialog(
    onDismiss: () -> Unit,
    onSchedule: (String) -> Unit
) {
    var instruction by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule a task") },
        text = {
            OutlinedTextField(
                value = instruction,
                onValueChange = { instruction = it },
                label = { Text("Example: Every day at 9 AM, check important emails") },
                minLines = 3
            )
        },
        confirmButton = {
            Button(enabled = instruction.isNotBlank(), onClick = { onSchedule(instruction) }) {
                Text("Schedule")
            }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}
