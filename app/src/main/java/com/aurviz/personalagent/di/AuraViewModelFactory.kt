package com.aurviz.personalagent.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aurviz.personalagent.AURAApplication
import com.aurviz.personalagent.presentation.chat.ChatViewModel
import com.aurviz.personalagent.presentation.history.HistoryViewModel
import com.aurviz.personalagent.presentation.memory.MemoryViewModel
import com.aurviz.personalagent.presentation.settings.SettingsViewModel
import com.aurviz.personalagent.presentation.tasks.TasksViewModel

class AuraViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel: ViewModel = when {
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> ChatViewModel(container.agentOrchestrator)
            modelClass.isAssignableFrom(MemoryViewModel::class.java) ->
                MemoryViewModel(container.memoryRepository)
            modelClass.isAssignableFrom(TasksViewModel::class.java) ->
                TasksViewModel(
                    container.scheduledTaskRepository,
                    container.agentScheduler,
                    container.executionLogRepository
                )
            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(container.executionLogRepository)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(
                    appVersion = AURAApplication.VERSION,
                    apiKeyConfigured = container.appConfig.isGeminiKeyConfigured(),
                    memoryRepository = container.memoryRepository,
                    scheduledTaskRepository = container.scheduledTaskRepository,
                    executionLogRepository = container.executionLogRepository,
                    pendingActionRepository = container.pendingActionRepository,
                    conversationContextRepository = container.conversationContextRepository,
                    agentScheduler = container.agentScheduler,
                    gmailApiService = container.gmailApiService,
                    gmailOAuthManager = container.gmailOAuthManager,
                    backendClient = container.backendClient
                )
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return viewModel as T
    }
}
