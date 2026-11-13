package com.aurviz.personalagent.domain.scheduler

interface AgentScheduler {
    suspend fun enqueue(taskId: Long)
    fun cancel(taskId: Long)
    fun cancelAll() {}
}
