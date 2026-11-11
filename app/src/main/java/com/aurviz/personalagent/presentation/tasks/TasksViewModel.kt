package com.aurviz.personalagent.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurviz.personalagent.domain.model.ScheduledTask
import com.aurviz.personalagent.domain.repository.ExecutionLogRepository
import com.aurviz.personalagent.domain.repository.ScheduledTaskRepository
import com.aurviz.personalagent.domain.scheduler.AgentScheduler
import com.aurviz.personalagent.scheduler.ScheduleParser
import com.aurviz.personalagent.utils.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TasksViewModel(
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val agentScheduler: AgentScheduler? = null,
    private val executionLogRepository: ExecutionLogRepository? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<ScheduledTask>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ScheduledTask>>> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            scheduledTaskRepository.getAllTasks()
                .catch { error ->
                    _uiState.value = UiState.Error(error.message ?: "Unable to load tasks")
                }
                .collect { tasks ->
                    val enriched = tasks.map { task ->
                        val last = executionLogRepository?.getLatestLogForTask(task.id.toString())
                        task.copy(lastExecutionAt = last?.createdAt)
                    }
                    _uiState.value = UiState.Success(enriched)
                }
        }
    }

    fun schedule(instruction: String) {
        viewModelScope.launch {
            try {
                val parsed = ScheduleParser.parse(instruction)
                val id = scheduledTaskRepository.insertTask(
                    ScheduledTask(
                        instruction = instruction.trim(),
                        scheduleExpression = parsed.expression,
                        nextRunAt = parsed.nextRunAt
                    )
                )
                agentScheduler?.enqueue(id)
            } catch (error: Exception) {
                _uiState.value = UiState.Error(error.message ?: "Unable to schedule task")
            }
        }
    }

    fun setEnabled(task: ScheduledTask, enabled: Boolean) {
        viewModelScope.launch {
            try {
                scheduledTaskRepository.setTaskEnabled(task.id, enabled)
                if (enabled) agentScheduler?.enqueue(task.id) else agentScheduler?.cancel(task.id)
            } catch (error: Exception) {
                _uiState.value = UiState.Error(error.message ?: "Unable to update task")
            }
        }
    }

    fun delete(task: ScheduledTask) {
        viewModelScope.launch {
            try {
                agentScheduler?.cancel(task.id)
                scheduledTaskRepository.deleteTask(task.id)
            } catch (error: Exception) {
                _uiState.value = UiState.Error(error.message ?: "Unable to delete task")
            }
        }
    }
}
