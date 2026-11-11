package com.aurviz.personalagent.domain.model

data class ExecutionLog(
    val id: Long = 0,
    val taskId: String,
    val command: String,
    val status: ExecutionStatus,
    val result: String = "",
    val durationMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ExecutionStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILURE,
    CANCELLED,
    TIMEOUT
}
