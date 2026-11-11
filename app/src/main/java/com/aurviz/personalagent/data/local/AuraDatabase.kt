package com.aurviz.personalagent.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aurviz.personalagent.data.local.dao.ExecutionLogDao
import com.aurviz.personalagent.data.local.dao.MemoryDao
import com.aurviz.personalagent.data.local.dao.PendingActionDao
import com.aurviz.personalagent.data.local.dao.ScheduledTaskDao
import com.aurviz.personalagent.data.local.entity.ExecutionLogEntity
import com.aurviz.personalagent.data.local.entity.MemoryEntity
import com.aurviz.personalagent.data.local.entity.PendingActionEntity
import com.aurviz.personalagent.data.local.entity.ScheduledTaskEntity

@Database(
    entities = [
        MemoryEntity::class,
        ScheduledTaskEntity::class,
        ExecutionLogEntity::class,
        PendingActionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun scheduledTaskDao(): ScheduledTaskDao
    abstract fun executionLogDao(): ExecutionLogDao
    abstract fun pendingActionDao(): PendingActionDao

    companion object {
        private const val DATABASE_NAME = "aura_database"

        @Volatile
        private var INSTANCE: AuraDatabase? = null

        fun getInstance(context: Context): AuraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuraDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
