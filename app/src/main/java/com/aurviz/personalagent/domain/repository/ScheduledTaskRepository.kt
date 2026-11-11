package com.aurviz.personalagent.domain.repository

import com.aurviz.personalagent.domain.model.ScheduledTask
import kotlinx.coroutines.flow.Flow

interface ScheduledTaskRepository {
    fun getAllTasks(): Flow<List<ScheduledTask>>
    fun getEnabledTasks(): Flow<List<ScheduledTask>>
    suspend fun getTaskById(id: Long): ScheduledTask?
    suspend fun insertTask(task: ScheduledTask): Long
    suspend fun updateTask(task: ScheduledTask)
    suspend fun deleteTask(id: Long)
    suspend fun deleteAllTasks()
    suspend fun setTaskEnabled(id: Long, enabled: Boolean)
}
