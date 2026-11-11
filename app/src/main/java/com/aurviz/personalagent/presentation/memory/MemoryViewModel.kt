package com.aurviz.personalagent.presentation.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurviz.personalagent.domain.model.Memory
import com.aurviz.personalagent.domain.repository.MemoryRepository
import com.aurviz.personalagent.utils.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MemoryViewModel(
    private val memoryRepository: MemoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<Memory>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Memory>>> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var query = ""
    private var allMemories: List<Memory> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            memoryRepository.getAllMemories()
                .catch { error ->
                    _uiState.value = UiState.Error(error.message ?: "Unable to load memories")
                }
                .collect { memories ->
                    allMemories = memories
                    _uiState.value = UiState.Success(filter(memories))
                }
        }
    }

    fun search(value: String) {
        query = value
        _uiState.value = UiState.Success(filter(allMemories))
    }

    fun save(key: String, value: String, category: String, existing: Memory? = null) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val memory = Memory(
                    id = existing?.id ?: 0,
                    key = key.trim(),
                    value = value.trim(),
                    category = category.trim().ifBlank { "general" },
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
                if (existing == null) memoryRepository.saveMemory(memory) else memoryRepository.updateMemory(memory)
            } catch (error: Exception) {
                _uiState.value = UiState.Error(error.message ?: "Unable to save memory")
            }
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            try {
                memoryRepository.deleteMemory(id)
            } catch (error: Exception) {
                _uiState.value = UiState.Error(error.message ?: "Unable to delete memory")
            }
        }
    }

    private fun filter(memories: List<Memory>): List<Memory> =
        if (query.isBlank()) memories else memories.filter {
            it.key.contains(query, true) || it.value.contains(query, true) || it.category.contains(query, true)
        }
}
