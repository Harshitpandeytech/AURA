package com.aurviz.personalagent.scheduler

import android.content.Context
import androidx.work.WorkManager
import com.aurviz.personalagent.domain.scheduler.AgentScheduler
import com.aurviz.personalagent.domain.repository.ScheduledTaskRepository
import com.aurviz.personalagent.utils.Logger

/**
 * WorkManager-backed scheduler. WorkManager may defer execution; it is not an exact alarm.
 */
class WorkAgentScheduler(
    private val context: Context,
    private val repository: ScheduledTaskRepository
) : AgentScheduler {
    override suspend fun enqueue(taskId: Long) {
        Logger.i("WorkAgentScheduler", "Queueing task $taskId")
        val task = repository.getTaskById(taskId)
            ?: error("Scheduled task $taskId was not found")
        WorkScheduler(context).enqueue(task.id, ScheduleParser.parse(task.scheduleExpression))
    }

    override fun cancel(taskId: Long) {
        WorkScheduler(context).cancel(taskId)
    }

    override fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWork()
    }
}
