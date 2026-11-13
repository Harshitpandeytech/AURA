package com.aurviz.personalagent.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurviz.personalagent.domain.repository.ExecutionLogRepository
import com.aurviz.personalagent.domain.repository.ConversationContextRepository
import com.aurviz.personalagent.domain.repository.MemoryRepository
import com.aurviz.personalagent.domain.repository.PendingActionRepository
import com.aurviz.personalagent.domain.repository.ScheduledTaskRepository
import com.aurviz.personalagent.domain.scheduler.AgentScheduler
import com.aurviz.personalagent.utils.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.net.Uri
import com.aurviz.personalagent.data.remote.GmailApiService
import com.aurviz.personalagent.data.remote.GmailOAuthManager
import com.aurviz.personalagent.data.remote.BackendClient
import java.util.UUID

data class SettingsUiModel(
    val appVersion: String,
    val apiKeyConfigured: Boolean,
    val backendConfigured: Boolean = false,
    val backendAuthenticated: Boolean = false,
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
    private val conversationContextRepository: ConversationContextRepository? = null,
    private val agentScheduler: AgentScheduler? = null,
    private val gmailApiService: GmailApiService? = null,
    private val gmailOAuthManager: GmailOAuthManager? = null,
    private val backendClient: BackendClient? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<SettingsUiModel>>(UiState.Loading)
    val uiState: StateFlow<UiState<SettingsUiModel>> = _uiState.asStateFlow()

    private var latest = SettingsUiModel(
        appVersion = appVersion,
        apiKeyConfigured = apiKeyConfigured,
        backendConfigured = backendClient?.isConfigured == true,
        backendAuthenticated = backendClient?.isAuthenticated == true,
        gmailConnected = gmailApiService?.isConnected() == true
    )

    init {
        viewModelScope.launch {
            publish(UiState.Success(latest))
        }
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                agentScheduler?.cancelAll()
                memoryRepository.deleteAllMemories()
                scheduledTaskRepository.deleteAllTasks()
                executionLogRepository.deleteAllLogs()
                pendingActionRepository.deleteAllActions()
                conversationContextRepository?.clear()
                publish(UiState.Success(latest.copy(statusMessage = "All local data cleared")))
                onComplete()
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

    fun authenticateBackend(email: String, password: String, register: Boolean) {
        viewModelScope.launch {
            try {
                if (register) {
                    backendClient?.register(email, password)
                } else {
                    backendClient?.login(email, password)
                }
                publish(
                    UiState.Success(
                        latest.copy(
                            backendAuthenticated = backendClient?.isAuthenticated == true,
                            statusMessage = if (register) "Backend account created" else "Signed in to backend"
                        )
                    )
                )
            } catch (error: Exception) {
                _uiState.value = UiState.Error(error.message ?: "Backend authentication failed")
            }
        }
    }

    fun signOutBackend() {
        backendClient?.clearSession()
        publish(UiState.Success(latest.copy(backendAuthenticated = false, statusMessage = "Signed out")))
    }

    private fun publish(state: UiState<SettingsUiModel>) {
        if (state is UiState.Success) {
            latest = state.data
        }
        _uiState.value = state
    }
}
