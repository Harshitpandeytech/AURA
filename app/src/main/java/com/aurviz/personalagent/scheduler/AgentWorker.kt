package com.aurviz.personalagent.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
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
            showNotification(
                title = "AURA Scheduled Task",
                message = response.content
            )
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

    private fun showNotification(title: String, message: String) {
        runCatching {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "AURA Scheduled Tasks",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }.onFailure { error ->
            com.aurviz.personalagent.utils.Logger.e(
                "AgentWorker",
                "Unable to show scheduled-task notification",
                error
            )
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
        const val CHANNEL_ID = "aura_scheduled_tasks"
    }
}
