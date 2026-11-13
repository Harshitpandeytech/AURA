package com.aurviz.personalagent.presentation.history

import com.aurviz.personalagent.MainDispatcherRule
import com.aurviz.personalagent.domain.model.ExecutionLog
import com.aurviz.personalagent.domain.model.ExecutionStatus
import com.aurviz.personalagent.domain.repository.ExecutionLogRepository
import com.aurviz.personalagent.utils.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeExecutionLogRepository : ExecutionLogRepository {
        val logsFlow = MutableStateFlow<List<ExecutionLog>>(emptyList())

        override fun getAllLogs(): Flow<List<ExecutionLog>> = logsFlow.asStateFlow()
        override fun getLogsByTaskId(taskId: String): Flow<List<ExecutionLog>> = logsFlow.asStateFlow()
        override suspend fun getLatestLogForTask(taskId: String): ExecutionLog? = logsFlow.value.firstOrNull { it.taskId == taskId }
        override suspend fun insertLog(log: ExecutionLog): Long {
            logsFlow.value = logsFlow.value + log
            return logsFlow.value.size.toLong()
        }
        override suspend fun updateLog(log: ExecutionLog) {
            logsFlow.value = logsFlow.value.map { if (it.id == log.id) log else it }
        }
        override suspend fun deleteLog(id: Long) {
            logsFlow.value = logsFlow.value.filterNot { it.id == id }
        }
        override suspend fun deleteAllLogs() {
            logsFlow.value = emptyList()
        }
        override suspend fun deleteOldLogs(beforeTimestamp: Long) {
            logsFlow.value = logsFlow.value.filter { it.createdAt >= beforeTimestamp }
        }
    }
    @Test
    fun loadsExecutionLogsSuccessfully() {
        val repository = FakeExecutionLogRepository()
        val log = ExecutionLog(id = 1L, taskId = "tool_alarm", command = "setAlarm", status = ExecutionStatus.SUCCESS, result = "Alarm set")
        repository.logsFlow.value = listOf(log)

        val viewModel = HistoryViewModel(repository)
        val state = viewModel.uiState.value

        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(1, data.size)
        assertEquals("setAlarm", data[0].command)
    }

    @Test
    fun clearAll_clearsLogsAndRefreshes() {
        val repository = FakeExecutionLogRepository()
        val log = ExecutionLog(id = 1L, taskId = "tool_email", command = "sendEmail", status = ExecutionStatus.SUCCESS, result = "Sent")
        repository.logsFlow.value = listOf(log)

        val viewModel = HistoryViewModel(repository)
        viewModel.clearAll()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertTrue((state as UiState.Success).data.isEmpty())
    }
}
