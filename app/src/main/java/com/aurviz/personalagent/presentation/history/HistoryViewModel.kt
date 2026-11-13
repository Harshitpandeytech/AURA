package com.aurviz.personalagent.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurviz.personalagent.domain.model.ExecutionLog
import com.aurviz.personalagent.domain.repository.ExecutionLogRepository
import com.aurviz.personalagent.utils.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val executionLogRepository: ExecutionLogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<ExecutionLog>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ExecutionLog>>> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            executionLogRepository.getAllLogs()
                .catch { error ->
                    _uiState.value = UiState.Error(error.message ?: "Unable to load history")
                }
                .collect { logs ->
                    _uiState.value = UiState.Success(logs)
                }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            try {
                executionLogRepository.deleteAllLogs()
                refresh()
            } catch (error: Exception) {
                _uiState.value = UiState.Error(error.message ?: "Unable to clear history")
            }
        }
    }
}
