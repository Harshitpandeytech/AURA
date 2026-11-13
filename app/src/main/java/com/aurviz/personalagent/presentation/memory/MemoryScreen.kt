package com.aurviz.personalagent.presentation.memory

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.aurviz.personalagent.domain.model.Memory
import com.aurviz.personalagent.presentation.components.EmptyState
import com.aurviz.personalagent.presentation.components.ErrorState
import com.aurviz.personalagent.presentation.components.GlassCard
import com.aurviz.personalagent.presentation.components.LoadingState
import com.aurviz.personalagent.presentation.navigation.auraViewModel
import com.aurviz.personalagent.utils.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(viewModel: MemoryViewModel = auraViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Memory?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Personal Memory", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            actions = {
                IconButton(onClick = { editing = null; showEditor = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add memory")
                }
            }
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; viewModel.search(it) },
            label = { Text("Search memories") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true
        )

        when (val state = uiState) {
            UiState.Idle, UiState.Loading -> LoadingState("Loading memories")
            is UiState.Error -> ErrorState(state.message, onRetry = viewModel::refresh)
            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Memory,
                        title = "No memories saved yet",
                        message = "Saved memories will appear here."
                    )
                } else {
                    MemoryList(
                        memories = state.data,
                        onDelete = viewModel::deleteMemory,
                        onEdit = { editing = it; showEditor = true }
                    )
                }
            }
        }
    }
    if (showEditor) {
        MemoryEditor(
            memory = editing,
            onDismiss = { showEditor = false },
            onSave = { key, value, category ->
                viewModel.save(key, value, category, editing)
                showEditor = false
            }
        )
    }
}

@Composable
private fun MemoryList(
    memories: List<Memory>,
    onDelete: (Long) -> Unit,
    onEdit: (Memory) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
    ) {
        items(memories, key = { it.id }) { memory ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                borderColor = Color.White.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = memory.key,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = memory.value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = memory.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onEdit(memory) }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit ${memory.key}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDelete(memory.id) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete ${memory.key}",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryEditor(
    memory: Memory?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var key by remember(memory) { mutableStateOf(memory?.key.orEmpty()) }
    var value by remember(memory) { mutableStateOf(memory?.value.orEmpty()) }
    var category by remember(memory) { mutableStateOf(memory?.category ?: "general") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (memory == null) "Add memory" else "Edit memory") },
        text = {
            Column {
                OutlinedTextField(key, { key = it }, label = { Text("Key") }, singleLine = true)
                OutlinedTextField(value, { value = it }, label = { Text("Value") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                enabled = key.isNotBlank() && value.isNotBlank(),
                onClick = { onSave(key, value, category) }
            ) { Text("Save") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}
