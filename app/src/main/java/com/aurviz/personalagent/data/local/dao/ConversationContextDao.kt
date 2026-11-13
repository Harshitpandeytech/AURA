package com.aurviz.personalagent.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aurviz.personalagent.data.local.entity.ConversationContextEntity

@Dao
interface ConversationContextDao {
    @Query("SELECT * FROM conversation_context WHERE id = 1 LIMIT 1")
    suspend fun get(): ConversationContextEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(context: ConversationContextEntity)

    @Query("DELETE FROM conversation_context")
    suspend fun clear()
}
