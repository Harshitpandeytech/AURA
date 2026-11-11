package com.aurviz.personalagent.data.repository

import com.aurviz.personalagent.data.local.dao.MemoryDao
import com.aurviz.personalagent.data.mapper.toDomain
import com.aurviz.personalagent.data.mapper.toEntity
import com.aurviz.personalagent.domain.model.Memory
import com.aurviz.personalagent.domain.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemoryRepositoryImpl(
    private val memoryDao: MemoryDao
) : MemoryRepository {

    override fun getAllMemories(): Flow<List<Memory>> {
        return memoryDao.getAllMemories().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getMemoriesByCategory(category: String): Flow<List<Memory>> {
        return memoryDao.getMemoriesByCategory(category).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getMemoryByKey(key: String): Memory? {
        return memoryDao.getMemoryByKey(key)?.toDomain()
    }

    override suspend fun searchMemories(query: String): List<Memory> {
        return memoryDao.searchMemories(query).map { it.toDomain() }
    }

    override suspend fun saveMemory(memory: Memory): Long {
        return memoryDao.insertMemory(memory.toEntity())
    }

    override suspend fun updateMemory(memory: Memory) {
        memoryDao.updateMemory(memory.toEntity())
    }

    override suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemory(id)
    }

    override suspend fun deleteAllMemories() {
        memoryDao.deleteAllMemories()
    }
}
