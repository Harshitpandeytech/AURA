package com.aurviz.personalagent.domain.model

data class ScheduledTask(
    val id: Long = 0,
    val instruction: String,
    val scheduleExpression: String,
    val toolConfig: String = "",
    val enabled: Boolean = true,
    val nextRunAt: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastExecutionAt: Long? = null
)
