package com.aurviz.personalagent.domain.repository

import com.aurviz.personalagent.domain.model.Memory
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun getAllMemories(): Flow<List<Memory>>
    fun getMemoriesByCategory(category: String): Flow<List<Memory>>
    suspend fun getMemoryByKey(key: String): Memory?
    suspend fun searchMemories(query: String): List<Memory>
    suspend fun saveMemory(memory: Memory): Long
    suspend fun updateMemory(memory: Memory)
    suspend fun deleteMemory(id: Long)
    suspend fun deleteAllMemories()
}
