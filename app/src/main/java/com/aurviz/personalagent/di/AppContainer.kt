package com.aurviz.personalagent.di

import android.content.Context
import com.aurviz.personalagent.BuildConfig
import com.aurviz.personalagent.ai.GeminiAgent
import com.aurviz.personalagent.ai.GroqAgent
import com.aurviz.personalagent.ai.PromptBuilder
import com.aurviz.personalagent.ai.ToolCallParser
import com.aurviz.personalagent.data.local.AuraDatabase
import com.aurviz.personalagent.data.remote.NetworkClient
import com.aurviz.personalagent.data.repository.ExecutionLogRepositoryImpl
import com.aurviz.personalagent.data.repository.ConversationContextRepositoryImpl
import com.aurviz.personalagent.data.repository.MemoryRepositoryImpl
import com.aurviz.personalagent.data.repository.PendingActionRepositoryImpl
import com.aurviz.personalagent.data.repository.ScheduledTaskRepositoryImpl
import com.aurviz.personalagent.domain.repository.ExecutionLogRepository
import com.aurviz.personalagent.domain.repository.ConversationContextRepository
import com.aurviz.personalagent.domain.repository.MemoryRepository
import com.aurviz.personalagent.domain.repository.PendingActionRepository
import com.aurviz.personalagent.domain.repository.ScheduledTaskRepository
import com.aurviz.personalagent.domain.scheduler.AgentScheduler
import com.aurviz.personalagent.domain.tools.ToolRegistry
import com.aurviz.personalagent.scheduler.WorkAgentScheduler
import com.aurviz.personalagent.security.SecureStorage
import com.aurviz.personalagent.tools.memory.MemoryTool
import com.aurviz.personalagent.tools.memory.SearchMemoryTool
import com.aurviz.personalagent.tools.memory.UpdateMemoryTool
import com.aurviz.personalagent.tools.memory.DeleteMemoryTool
import com.aurviz.personalagent.tools.email.EmailTool
import com.aurviz.personalagent.tools.email.ImportantEmailTool
import com.aurviz.personalagent.tools.email.ReadEmailTool
import com.aurviz.personalagent.tools.browser.BrowserTool
import com.aurviz.personalagent.tools.scheduler.SchedulerTool
import com.aurviz.personalagent.tools.scheduler.AlarmTool
import com.aurviz.personalagent.tools.scheduler.StopwatchTool
import com.aurviz.personalagent.tools.scheduler.TimerTool
import com.aurviz.personalagent.data.remote.GmailApiService
import com.aurviz.personalagent.utils.Logger
import com.aurviz.personalagent.voice.VoiceInput

/**
 * Manual dependency injection container.
 * Provides application-wide singletons. Created once from [com.aurviz.personalagent.AURAApplication].
 */
class AppContainer(private val applicationContext: Context) {

    val database: AuraDatabase by lazy {
        Logger.i("AppContainer", "Initializing database")
        AuraDatabase.getInstance(applicationContext)
    }

    val memoryRepository: MemoryRepository by lazy {
        MemoryRepositoryImpl(database.memoryDao())
    }

    val scheduledTaskRepository: ScheduledTaskRepository by lazy {
        ScheduledTaskRepositoryImpl(database.scheduledTaskDao())
    }

    val executionLogRepository: ExecutionLogRepository by lazy {
        ExecutionLogRepositoryImpl(database.executionLogDao())
    }

    val pendingActionRepository: PendingActionRepository by lazy {
        PendingActionRepositoryImpl(database.pendingActionDao())
    }

    val conversationContextRepository: ConversationContextRepository by lazy {
        ConversationContextRepositoryImpl(database.conversationContextDao())
    }

    val toolRegistry: ToolRegistry by lazy {
        Logger.i("AppContainer", "Initializing tool registry")
        ToolRegistry().apply {
            register(MemoryTool(memoryRepository))
            register(SearchMemoryTool(memoryRepository))
            register(UpdateMemoryTool(memoryRepository))
            register(DeleteMemoryTool(memoryRepository))
            register(EmailTool(gmailApiService, pendingActionRepository))
            register(ImportantEmailTool(gmailApiService))
            register(ReadEmailTool(gmailApiService))
            register(BrowserTool(applicationContext))
            register(SchedulerTool(scheduledTaskRepository, agentScheduler))
            register(AlarmTool(applicationContext))
            register(TimerTool(applicationContext))
            register(StopwatchTool())
        }
    }

    val networkClient: NetworkClient by lazy { NetworkClient() }

    val secureStorage: SecureStorage by lazy { SecureStorage(applicationContext) }

    val gmailApiService: GmailApiService by lazy {
        GmailApiService(networkClient.okHttpClient, secureStorage)
    }

    val gmailOAuthManager by lazy {
        com.aurviz.personalagent.data.remote.GmailOAuthManager(
            networkClient.okHttpClient,
            secureStorage,
            appConfig.gmailClientId
        )
    }

    val agentScheduler: AgentScheduler by lazy {
        WorkAgentScheduler(applicationContext, scheduledTaskRepository)
    }

    val voiceInput: VoiceInput by lazy { VoiceInput() }

    val groqAgent: GroqAgent by lazy { GroqAgent(networkClient.okHttpClient, appConfig) }

    val geminiAgent: GeminiAgent by lazy {
        GeminiAgent(networkClient.okHttpClient, appConfig, fallback = groqAgent)
    }

    val promptBuilder: PromptBuilder by lazy { PromptBuilder() }

    val toolCallParser: ToolCallParser by lazy { ToolCallParser() }

    val agentOrchestrator by lazy {
        com.aurviz.personalagent.domain.agent.DefaultAgentOrchestrator(
            memoryRepository = memoryRepository,
            geminiAgent = geminiAgent,
            promptBuilder = promptBuilder,
            toolRegistry = toolRegistry,
            executionLogRepository = executionLogRepository,
            conversationContextRepository = conversationContextRepository
        )
    }

    val appConfig: AppConfig by lazy {
        AppConfig(
            apiKey = BuildConfig.GEMINI_API_KEY,
            gmailClientId = BuildConfig.GMAIL_CLIENT_ID,
            geminiModel = BuildConfig.GEMINI_MODEL,
            groqApiKey = BuildConfig.GROQ_API_KEY,
            groqModel = BuildConfig.GROQ_MODEL
        )
    }

    val viewModelFactory: AuraViewModelFactory by lazy { AuraViewModelFactory(this) }

    init {
        Logger.i("AppContainer", "AppContainer initialized")
    }
}
