package com.aurviz.personalagent.scheduler

import android.content.Context
import com.aurviz.personalagent.domain.scheduler.AgentScheduler
import com.aurviz.personalagent.domain.repository.ScheduledTaskRepository
import com.aurviz.personalagent.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * WorkManager-backed scheduler. WorkManager may defer execution; it is not an exact alarm.
 */
class WorkAgentScheduler(
    private val context: Context,
    private val repository: ScheduledTaskRepository
) : AgentScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun enqueue(taskId: Long) {
        Logger.i("WorkAgentScheduler", "Queueing task $taskId")
        scope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            WorkScheduler(context).enqueue(task.id, ScheduleParser.parse(task.scheduleExpression))
        }
    }

    override fun cancel(taskId: Long) {
        WorkScheduler(context).cancel(taskId)
    }
}
