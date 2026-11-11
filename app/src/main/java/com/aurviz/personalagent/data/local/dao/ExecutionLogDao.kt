package com.aurviz.personalagent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aurviz.personalagent.data.local.entity.ExecutionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionLogDao {
    @Query("SELECT * FROM execution_logs ORDER BY createdAt DESC")
    fun getAllLogs(): Flow<List<ExecutionLogEntity>>

    @Query("SELECT * FROM execution_logs WHERE taskId = :taskId ORDER BY createdAt DESC")
    fun getLogsByTaskId(taskId: String): Flow<List<ExecutionLogEntity>>

    @Query("SELECT * FROM execution_logs WHERE taskId = :taskId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestLogForTask(taskId: String): ExecutionLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ExecutionLogEntity): Long

    @Update
    suspend fun updateLog(log: ExecutionLogEntity)

    @Query("DELETE FROM execution_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("DELETE FROM execution_logs")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM execution_logs WHERE createdAt < :beforeTimestamp")
    suspend fun deleteOldLogs(beforeTimestamp: Long)
}
