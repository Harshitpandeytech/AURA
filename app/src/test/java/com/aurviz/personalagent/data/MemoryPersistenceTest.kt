package com.aurviz.personalagent.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aurviz.personalagent.data.local.AuraDatabase
import com.aurviz.personalagent.data.repository.MemoryRepositoryImpl
import com.aurviz.personalagent.domain.model.Memory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MemoryPersistenceTest {
    private val database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AuraDatabase::class.java
    ).allowMainThreadQueries().build()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun memoryCrudAndKeywordSearch_arePersistedThroughRepository() = runBlocking {
        val repository = MemoryRepositoryImpl(database.memoryDao())
        val id = repository.saveMemory(
            Memory(key = "city", value = "Pune", category = "fact")
        )

        assertEquals("Pune", repository.getMemoryByKey("city")?.value)
        assertEquals(1, repository.searchMemories("pune").size)

        repository.updateMemory(
            repository.getMemoryByKey("city")!!.copy(
                id = id,
                value = "Mumbai",
                updatedAt = System.currentTimeMillis()
            )
        )
        assertEquals("Mumbai", repository.getMemoryByKey("city")?.value)

        assertEquals(1, repository.getAllMemories().first().size)
        repository.deleteMemory(id)
        assertEquals(0, repository.getAllMemories().first().size)
    }
}
