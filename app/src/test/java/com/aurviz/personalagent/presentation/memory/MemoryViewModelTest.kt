package com.aurviz.personalagent.presentation.memory

import com.aurviz.personalagent.MainDispatcherRule
import com.aurviz.personalagent.domain.model.Memory
import com.aurviz.personalagent.domain.repository.MemoryRepository
import com.aurviz.personalagent.utils.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MemoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadsMemoriesAndDeletesOne() {
        val repository = FakeMemoryRepository(
            listOf(
                Memory(id = 1, key = "city", value = "Pune", category = "fact"),
                Memory(id = 2, key = "name", value = "AURA", category = "fact")
            )
        )
        val viewModel = MemoryViewModel(repository)
        val loaded = viewModel.uiState.value as UiState.Success
        assertEquals(2, loaded.data.size)

        viewModel.deleteMemory(1)

        val remaining = viewModel.uiState.value as UiState.Success
        assertEquals(listOf("name"), remaining.data.map { it.key })
    }

    @Test
    fun repositoryFailure_surfacesError() {
        val viewModel = MemoryViewModel(FailingMemoryRepository())
        assertTrue(viewModel.uiState.value is UiState.Error)
    }
}

private class FakeMemoryRepository(
    initial: List<Memory>
) : MemoryRepository {
    private val memories = MutableStateFlow(initial)

    override fun getAllMemories(): Flow<List<Memory>> = memories

    override fun getMemoriesByCategory(category: String): Flow<List<Memory>> {
        return memories.map { items -> items.filter { it.category == category } }
    }

    override suspend fun getMemoryByKey(key: String): Memory? {
        return memories.value.firstOrNull { it.key == key }
    }

    override suspend fun searchMemories(query: String): List<Memory> {
        return memories.value.filter { it.key.contains(query) || it.value.contains(query) }
    }

    override suspend fun saveMemory(memory: Memory): Long {
        memories.value = memories.value + memory
        return memory.id
    }

    override suspend fun updateMemory(memory: Memory) {
        memories.value = memories.value.map { if (it.id == memory.id) memory else it }
    }

    override suspend fun deleteMemory(id: Long) {
        memories.value = memories.value.filterNot { it.id == id }
    }

    override suspend fun deleteAllMemories() {
        memories.value = emptyList()
    }
}

private class FailingMemoryRepository : MemoryRepository {
    override fun getAllMemories(): Flow<List<Memory>> = kotlinx.coroutines.flow.flow {
        throw IllegalStateException("database unavailable")
    }

    override fun getMemoriesByCategory(category: String): Flow<List<Memory>> = getAllMemories()
    override suspend fun getMemoryByKey(key: String): Memory? = null
    override suspend fun searchMemories(query: String): List<Memory> = emptyList()
    override suspend fun saveMemory(memory: Memory): Long = 0
    override suspend fun updateMemory(memory: Memory) = Unit
    override suspend fun deleteMemory(id: Long) = Unit
    override suspend fun deleteAllMemories() = Unit
}
