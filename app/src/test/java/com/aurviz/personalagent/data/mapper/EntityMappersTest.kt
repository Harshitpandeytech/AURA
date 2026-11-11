package com.aurviz.personalagent.data.mapper

import com.aurviz.personalagent.data.local.entity.ExecutionLogEntity
import com.aurviz.personalagent.domain.model.ExecutionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappersTest {
    @Test
    fun executionLog_unknownStatus_mapsToFailure() {
        val domain = ExecutionLogEntity(
            id = 4,
            taskId = "task-1",
            command = "summarize",
            status = "NOT_A_STATUS",
            result = "",
            durationMs = 12,
            createdAt = 1L
        ).toDomain()

        assertEquals(ExecutionStatus.FAILURE, domain.status)
        assertEquals("summarize", domain.toEntity().command)
    }
}
