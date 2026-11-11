package com.aurviz.personalagent.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aurviz.personalagent.AURAApplication
import com.aurviz.personalagent.domain.model.ExecutionLog
import com.aurviz.personalagent.domain.model.ExecutionStatus

class AgentWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(TASK_ID, -1L)
        if (taskId <= 0) return Result.failure()
        val container = (applicationContext as AURAApplication).appContainer
        val task = container.scheduledTaskRepository.getTaskById(taskId)
            ?: return Result.failure()
        if (!task.enabled) return Result.success()
        val started = System.currentTimeMillis()
        val workflow = task.toolConfig.ifBlank { task.instruction }
        return try {
            val response = container.agentOrchestrator.processCommand(workflow)
            container.executionLogRepository.insertLog(
                ExecutionLog(
                    taskId = task.id.toString(),
                    command = workflow,
                    status = ExecutionStatus.SUCCESS,
                    result = response.content,
                    durationMs = System.currentTimeMillis() - started
                )
            )
            updateNextRun(container, task)
            Result.success()
        } catch (error: Exception) {
            container.executionLogRepository.insertLog(
                ExecutionLog(
                    taskId = task.id.toString(),
                    command = workflow,
                    status = ExecutionStatus.FAILURE,
                    result = error.message ?: "Scheduled task failed",
                    durationMs = System.currentTimeMillis() - started
                )
            )
            Result.retry()
        }
    }

    private suspend fun updateNextRun(
        container: com.aurviz.personalagent.di.AppContainer,
        task: com.aurviz.personalagent.domain.model.ScheduledTask
    ) {
        val parsed = ScheduleParser.parse(task.scheduleExpression)
        container.scheduledTaskRepository.updateTask(
            task.copy(nextRunAt = parsed.nextRunAt)
        )
    }

    companion object {
        const val TASK_ID = "task_id"
    }
}
