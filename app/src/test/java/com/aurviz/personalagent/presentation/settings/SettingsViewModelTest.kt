package com.aurviz.personalagent.presentation.settings

import com.aurviz.personalagent.MainDispatcherRule
import com.aurviz.personalagent.domain.model.ExecutionLog
import com.aurviz.personalagent.domain.model.Memory
import com.aurviz.personalagent.domain.model.PendingAction
import com.aurviz.personalagent.domain.model.PendingActionStatus
import com.aurviz.personalagent.domain.model.ScheduledTask
import com.aurviz.personalagent.domain.repository.ExecutionLogRepository
import com.aurviz.personalagent.domain.repository.MemoryRepository
import com.aurviz.personalagent.domain.repository.PendingActionRepository
import com.aurviz.personalagent.domain.repository.ScheduledTaskRepository
import com.aurviz.personalagent.utils.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun clearAllData_reportsSuccess() {
        val memory = RecordingMemoryRepository()
        val tasks = RecordingTaskRepository()
        val logs = RecordingLogRepository()
        val actions = RecordingActionRepository()
        val viewModel = SettingsViewModel(
            appVersion = "1.0.0",
            apiKeyConfigured = false,
            memoryRepository = memory,
            scheduledTaskRepository = tasks,
            executionLogRepository = logs,
            pendingActionRepository = actions
        )

        viewModel.clearAllData()

        val state = viewModel.uiState.value as UiState.Success
        assertEquals("1.0.0", state.data.appVersion)
        assertEquals("All local data cleared", state.data.statusMessage)
        assertTrue(memory.cleared && tasks.cleared && logs.cleared && actions.cleared)
    }
}

private class RecordingMemoryRepository : MemoryRepository {
    var cleared = false
    override fun getAllMemories(): Flow<List<Memory>> = flowOf(emptyList())
    override fun getMemoriesByCategory(category: String): Flow<List<Memory>> = flowOf(emptyList())
    override suspend fun getMemoryByKey(key: String): Memory? = null
    override suspend fun searchMemories(query: String): List<Memory> = emptyList()
    override suspend fun saveMemory(memory: Memory): Long = 0
    override suspend fun updateMemory(memory: Memory) = Unit
    override suspend fun deleteMemory(id: Long) = Unit
    override suspend fun deleteAllMemories() { cleared = true }
}

private class RecordingTaskRepository : ScheduledTaskRepository {
    var cleared = false
    override fun getAllTasks(): Flow<List<ScheduledTask>> = flowOf(emptyList())
    override fun getEnabledTasks(): Flow<List<ScheduledTask>> = flowOf(emptyList())
    override suspend fun getTaskById(id: Long): ScheduledTask? = null
    override suspend fun insertTask(task: ScheduledTask): Long = 0
    override suspend fun updateTask(task: ScheduledTask) = Unit
    override suspend fun deleteTask(id: Long) = Unit
    override suspend fun deleteAllTasks() { cleared = true }
    override suspend fun setTaskEnabled(id: Long, enabled: Boolean) = Unit
}

private class RecordingLogRepository : ExecutionLogRepository {
    var cleared = false
    override fun getAllLogs(): Flow<List<ExecutionLog>> = flowOf(emptyList())
    override fun getLogsByTaskId(taskId: String): Flow<List<ExecutionLog>> = flowOf(emptyList())
    override suspend fun getLatestLogForTask(taskId: String): ExecutionLog? = null
    override suspend fun insertLog(log: ExecutionLog): Long = 0
    override suspend fun updateLog(log: ExecutionLog) = Unit
    override suspend fun deleteLog(id: Long) = Unit
    override suspend fun deleteAllLogs() { cleared = true }
    override suspend fun deleteOldLogs(beforeTimestamp: Long) = Unit
}

private class RecordingActionRepository : PendingActionRepository {
    var cleared = false
    override fun getPendingActions(): Flow<List<PendingAction>> = flowOf(emptyList())
    override suspend fun getActionById(id: Long): PendingAction? = null
    override suspend fun insertAction(action: PendingAction): Long = 0
    override suspend fun updateStatus(id: Long, status: PendingActionStatus) = Unit
    override suspend fun deleteExpiredActions() = Unit
    override suspend fun deleteAllActions() { cleared = true }
}
