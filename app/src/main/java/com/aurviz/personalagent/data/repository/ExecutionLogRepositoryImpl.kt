package com.aurviz.personalagent.data.repository

import com.aurviz.personalagent.data.local.dao.ExecutionLogDao
import com.aurviz.personalagent.data.mapper.toDomain
import com.aurviz.personalagent.data.mapper.toEntity
import com.aurviz.personalagent.domain.model.ExecutionLog
import com.aurviz.personalagent.domain.repository.ExecutionLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExecutionLogRepositoryImpl(
    private val executionLogDao: ExecutionLogDao
) : ExecutionLogRepository {

    override fun getAllLogs(): Flow<List<ExecutionLog>> {
        return executionLogDao.getAllLogs().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getLogsByTaskId(taskId: String): Flow<List<ExecutionLog>> {
        return executionLogDao.getLogsByTaskId(taskId).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getLatestLogForTask(taskId: String): ExecutionLog? =
        executionLogDao.getLatestLogForTask(taskId)?.toDomain()

    override suspend fun insertLog(log: ExecutionLog): Long {
        return executionLogDao.insertLog(log.toEntity())
    }

    override suspend fun updateLog(log: ExecutionLog) {
        executionLogDao.updateLog(log.toEntity())
    }

    override suspend fun deleteLog(id: Long) {
        executionLogDao.deleteLog(id)
    }

    override suspend fun deleteAllLogs() {
        executionLogDao.deleteAllLogs()
    }

    override suspend fun deleteOldLogs(beforeTimestamp: Long) {
        executionLogDao.deleteOldLogs(beforeTimestamp)
    }
}
