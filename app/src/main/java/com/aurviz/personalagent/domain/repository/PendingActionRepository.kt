package com.aurviz.personalagent.domain.repository

import com.aurviz.personalagent.domain.model.PendingAction
import com.aurviz.personalagent.domain.model.PendingActionStatus
import kotlinx.coroutines.flow.Flow

interface PendingActionRepository {
    fun getPendingActions(): Flow<List<PendingAction>>
    suspend fun getActionById(id: Long): PendingAction?
    suspend fun insertAction(action: PendingAction): Long
    suspend fun updateStatus(id: Long, status: PendingActionStatus)
    suspend fun deleteExpiredActions()
    suspend fun deleteAllActions()
}
