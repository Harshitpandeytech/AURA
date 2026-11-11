package com.aurviz.personalagent.data.mapper

import com.aurviz.personalagent.data.local.entity.ExecutionLogEntity
import com.aurviz.personalagent.data.local.entity.MemoryEntity
import com.aurviz.personalagent.data.local.entity.PendingActionEntity
import com.aurviz.personalagent.data.local.entity.ScheduledTaskEntity
import com.aurviz.personalagent.domain.model.ExecutionLog
import com.aurviz.personalagent.domain.model.ExecutionStatus
import com.aurviz.personalagent.domain.model.Memory
import com.aurviz.personalagent.domain.model.PendingAction
import com.aurviz.personalagent.domain.model.PendingActionStatus
import com.aurviz.personalagent.domain.model.ScheduledTask

// Memory mappers
fun MemoryEntity.toDomain(): Memory = Memory(
    id = id,
    key = key,
    value = value,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Memory.toEntity(): MemoryEntity = MemoryEntity(
    id = id,
    key = key,
    value = value,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// ScheduledTask mappers
fun ScheduledTaskEntity.toDomain(): ScheduledTask = ScheduledTask(
    id = id,
    instruction = instruction,
    scheduleExpression = scheduleExpression,
    toolConfig = toolConfig,
    enabled = enabled,
    nextRunAt = nextRunAt,
    createdAt = createdAt
)

fun ScheduledTask.toEntity(): ScheduledTaskEntity = ScheduledTaskEntity(
    id = id,
    instruction = instruction,
    scheduleExpression = scheduleExpression,
    toolConfig = toolConfig,
    enabled = enabled,
    nextRunAt = nextRunAt,
    createdAt = createdAt
)

// ExecutionLog mappers
fun ExecutionLogEntity.toDomain(): ExecutionLog = ExecutionLog(
    id = id,
    taskId = taskId,
    command = command,
    status = try { ExecutionStatus.valueOf(status) } catch (_: Exception) { ExecutionStatus.FAILURE },
    result = result,
    durationMs = durationMs,
    createdAt = createdAt
)

fun ExecutionLog.toEntity(): ExecutionLogEntity = ExecutionLogEntity(
    id = id,
    taskId = taskId,
    command = command,
    status = status.name,
    result = result,
    durationMs = durationMs,
    createdAt = createdAt
)

// PendingAction mappers
fun PendingActionEntity.toDomain(): PendingAction = PendingAction(
    id = id,
    toolName = toolName,
    arguments = arguments,
    status = try { PendingActionStatus.valueOf(status) } catch (_: Exception) { PendingActionStatus.EXPIRED },
    expiresAt = expiresAt
)

fun PendingAction.toEntity(): PendingActionEntity = PendingActionEntity(
    id = id,
    toolName = toolName,
    arguments = arguments,
    status = status.name,
    expiresAt = expiresAt
)
