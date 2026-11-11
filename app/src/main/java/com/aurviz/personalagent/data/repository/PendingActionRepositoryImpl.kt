package com.aurviz.personalagent.data.repository

import com.aurviz.personalagent.data.local.dao.PendingActionDao
import com.aurviz.personalagent.data.mapper.toDomain
import com.aurviz.personalagent.data.mapper.toEntity
import com.aurviz.personalagent.domain.model.PendingAction
import com.aurviz.personalagent.domain.model.PendingActionStatus
import com.aurviz.personalagent.domain.repository.PendingActionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PendingActionRepositoryImpl(
    private val pendingActionDao: PendingActionDao
) : PendingActionRepository {

    override fun getPendingActions(): Flow<List<PendingAction>> {
        return pendingActionDao.getPendingActions().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getActionById(id: Long): PendingAction? {
        return pendingActionDao.getActionById(id)?.toDomain()
    }

    override suspend fun insertAction(action: PendingAction): Long {
        return pendingActionDao.insertAction(action.toEntity())
    }

    override suspend fun updateStatus(id: Long, status: PendingActionStatus) {
        pendingActionDao.updateStatus(id, status.name)
    }

    override suspend fun deleteExpiredActions() {
        pendingActionDao.deleteExpiredActions()
    }

    override suspend fun deleteAllActions() {
        pendingActionDao.deleteAllActions()
    }
}
