package com.aurviz.personalagent

import androidx.test.core.app.ApplicationProvider
import com.aurviz.personalagent.presentation.chat.ChatViewModel
import com.aurviz.personalagent.presentation.history.HistoryViewModel
import com.aurviz.personalagent.presentation.memory.MemoryViewModel
import com.aurviz.personalagent.presentation.settings.SettingsViewModel
import com.aurviz.personalagent.presentation.tasks.TasksViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = AURAApplication::class, sdk = [34])
class ApplicationInitializationTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun application_initializesSharedDependenciesAndViewModels() {
        val app = ApplicationProvider.getApplicationContext<AURAApplication>()
        val container = app.appContainer

        assertTrue(container.database.openHelper.writableDatabase.isOpen)
        assertNotNull(container.memoryRepository)
        assertNotNull(container.scheduledTaskRepository)
        assertNotNull(container.executionLogRepository)
        assertNotNull(container.pendingActionRepository)
        assertNotNull(container.networkClient.okHttpClient)
        assertNotNull(container.secureStorage)
        assertNotNull(container.agentScheduler)
        assertNotNull(container.voiceInput)
        assertEquals(container.appConfig.isGeminiKeyConfigured(), container.geminiAgent.isEnabled)
        assertTrue(container.toolRegistry.getToolNames().containsAll(
            listOf("save_memory", "search_memory", "update_memory", "delete_memory")
        ))

        val factory = container.viewModelFactory
        assertNotNull(factory.create(ChatViewModel::class.java))
        assertNotNull(factory.create(MemoryViewModel::class.java))
        assertNotNull(factory.create(TasksViewModel::class.java))
        assertNotNull(factory.create(HistoryViewModel::class.java))
        assertNotNull(factory.create(SettingsViewModel::class.java))
    }
}
