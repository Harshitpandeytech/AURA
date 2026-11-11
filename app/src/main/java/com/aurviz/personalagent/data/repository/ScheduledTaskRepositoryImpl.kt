package com.aurviz.personalagent.data.repository

import com.aurviz.personalagent.data.local.dao.ScheduledTaskDao
import com.aurviz.personalagent.data.mapper.toDomain
import com.aurviz.personalagent.data.mapper.toEntity
import com.aurviz.personalagent.domain.model.ScheduledTask
import com.aurviz.personalagent.domain.repository.ScheduledTaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScheduledTaskRepositoryImpl(
    private val scheduledTaskDao: ScheduledTaskDao
) : ScheduledTaskRepository {

    override fun getAllTasks(): Flow<List<ScheduledTask>> {
        return scheduledTaskDao.getAllTasks().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getEnabledTasks(): Flow<List<ScheduledTask>> {
        return scheduledTaskDao.getEnabledTasks().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getTaskById(id: Long): ScheduledTask? {
        return scheduledTaskDao.getTaskById(id)?.toDomain()
    }

    override suspend fun insertTask(task: ScheduledTask): Long {
        return scheduledTaskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: ScheduledTask) {
        scheduledTaskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(id: Long) {
        scheduledTaskDao.deleteTask(id)
    }

    override suspend fun deleteAllTasks() {
        scheduledTaskDao.deleteAllTasks()
    }

    override suspend fun setTaskEnabled(id: Long, enabled: Boolean) {
        scheduledTaskDao.setTaskEnabled(id, enabled)
    }
}
