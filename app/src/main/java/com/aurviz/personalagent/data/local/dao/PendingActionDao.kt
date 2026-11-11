package com.aurviz.personalagent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aurviz.personalagent.data.local.entity.PendingActionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingActionDao {
    @Query("SELECT * FROM pending_actions WHERE status = 'AWAITING_APPROVAL' ORDER BY expiresAt ASC")
    fun getPendingActions(): Flow<List<PendingActionEntity>>

    @Query("SELECT * FROM pending_actions WHERE id = :id")
    suspend fun getActionById(id: Long): PendingActionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: PendingActionEntity): Long

    @Query("UPDATE pending_actions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM pending_actions WHERE expiresAt < :now AND status = 'AWAITING_APPROVAL'")
    suspend fun deleteExpiredActions(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM pending_actions")
    suspend fun deleteAllActions()
}
