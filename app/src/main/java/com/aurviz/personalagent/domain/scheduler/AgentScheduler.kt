package com.aurviz.personalagent.domain.scheduler

interface AgentScheduler {
    fun enqueue(taskId: Long)
    fun cancel(taskId: Long)
    fun cancelAll() {}
}
