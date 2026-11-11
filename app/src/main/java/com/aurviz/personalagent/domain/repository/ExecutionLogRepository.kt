package com.aurviz.personalagent.domain.repository

import com.aurviz.personalagent.domain.model.ExecutionLog
import kotlinx.coroutines.flow.Flow

interface ExecutionLogRepository {
    fun getAllLogs(): Flow<List<ExecutionLog>>
    fun getLogsByTaskId(taskId: String): Flow<List<ExecutionLog>>
    suspend fun getLatestLogForTask(taskId: String): ExecutionLog?
    suspend fun insertLog(log: ExecutionLog): Long
    suspend fun updateLog(log: ExecutionLog)
    suspend fun deleteLog(id: Long)
    suspend fun deleteAllLogs()
    suspend fun deleteOldLogs(beforeTimestamp: Long)
}
