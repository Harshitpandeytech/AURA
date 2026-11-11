package com.aurviz.personalagent.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurviz.personalagent.domain.repository.ExecutionLogRepository
import com.aurviz.personalagent.domain.repository.MemoryRepository
import com.aurviz.personalagent.domain.repository.PendingActionRepository
import com.aurviz.personalagent.domain.repository.ScheduledTaskRepository
import com.aurviz.personalagent.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.net.Uri
import com.aurviz.personalagent.data.remote.GmailApiService
import com.aurviz.personalagent.data.remote.GmailOAuthManager
import java.util.UUID

data class SettingsUiModel(
    val appVersion: String,
    val apiKeyConfigured: Boolean,
    val gmailConnected: Boolean = false,
    val statusMessage: String? = null
)

class SettingsViewModel(
    private val appVersion: String,
    apiKeyConfigured: Boolean,
    private val memoryRepository: MemoryRepository,
    private val scheduledTaskRepository: ScheduledTaskRepository,
    private val executionLogRepository: ExecutionLogRepository,
    private val pendingActionRepository: PendingActionRepository,
    private val gmailApiService: GmailApiService? = null,
    private val gmailOAuthManager: GmailOAuthManager? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<SettingsUiModel>>(UiState.Loading)
    val uiState: StateFlow<UiState<SettingsUiModel>> = _uiState.asStateFlow()

    private var latest = SettingsUiModel(
        appVersion = appVersion,
        apiKeyConfigured = apiKeyConfigured,
        gmailConnected = gmailApiService?.isConnected() == true
    )

    init {
        viewModelScope.launch {
            publish(UiState.Success(latest))
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                memoryRepository.deleteAllMemories()
                scheduledTaskRepository.deleteAllTasks()
                executionLogRepository.deleteAllLogs()
                pendingActionRepository.deleteAllActions()
                publish(UiState.Success(latest.copy(statusMessage = "All local data cleared")))
            } catch (error: Exception) {
                _uiState.value = UiState.Error(error.message ?: "Unable to clear data")
            }
        }
    }

    fun dismissError() {
        _uiState.value = UiState.Success(latest)
    }

    fun gmailAuthorizationUri(): Uri? =
        gmailOAuthManager?.authorizationUri(UUID.randomUUID().toString())

    private fun publish(state: UiState<SettingsUiModel>) {
        if (state is UiState.Success) {
            latest = state.data
        }
        _uiState.value = state
    }
}
