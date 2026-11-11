package com.aurviz.personalagent.scheduler

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class WorkScheduler(private val context: Context) {
    fun enqueue(taskId: Long, schedule: ParsedSchedule) {
        val interval = if (schedule.frequency == Frequency.DAILY) 1L else 7L
        val delay = (schedule.nextRunAt - System.currentTimeMillis()).coerceAtLeast(0)
        val request = PeriodicWorkRequestBuilder<AgentWorker>(interval, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(workDataOf(AgentWorker.TASK_ID to taskId))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName(taskId),
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(taskId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(taskId))
    }

    private fun workName(taskId: Long) = "aura-task-$taskId"
}
