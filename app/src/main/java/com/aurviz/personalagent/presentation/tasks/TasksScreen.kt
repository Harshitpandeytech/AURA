package com.aurviz.personalagent.presentation.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurviz.personalagent.domain.model.ScheduledTask
import com.aurviz.personalagent.presentation.components.EmptyState
import com.aurviz.personalagent.presentation.components.ErrorState
import com.aurviz.personalagent.presentation.components.GlassCard
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
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
    ) {
        items(tasks, key = { it.id }) { task ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                borderColor = Color.White.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.instruction,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.scheduleExpression,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Next: ${DateTimeUtils.formatDisplay(task.nextRunAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Last: ${task.lastExecutionAt?.let(DateTimeUtils::formatDisplay) ?: "Never"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = task.enabled,
                        onCheckedChange = { enabled -> onEnabledChange(task, enabled) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = { onDelete(task) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete task",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
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
