package com.aurviz.personalagent.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aurviz.personalagent.AURAApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as AURAApplication).appContainer
                val scheduler = WorkScheduler(context.applicationContext)
                container.scheduledTaskRepository.getEnabledTasks().first().forEach { task ->
                    scheduler.enqueue(task.id, ScheduleParser.parse(task.scheduleExpression))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
