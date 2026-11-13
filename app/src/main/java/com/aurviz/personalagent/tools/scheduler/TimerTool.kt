package com.aurviz.personalagent.tools.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.aurviz.personalagent.domain.tools.AgentTool
import com.aurviz.personalagent.domain.tools.ToolParameter
import com.aurviz.personalagent.domain.tools.ToolParameterSchema
import com.aurviz.personalagent.domain.tools.ToolResult

class TimerTool(private val context: Context) : AgentTool {
    override val name = "set_timer"
    override val description =
        "Set a physical countdown timer that rings when it finishes. Use durationSeconds for the requested duration."
    override val requiresConfirmation = false
    override val parameterSchema = ToolParameterSchema(
        listOf(
            ToolParameter("durationSeconds", "integer", "Countdown duration in seconds", required = true),
            ToolParameter("label", "string", "Short timer label", required = false)
        )
    )

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val durationSeconds = arguments["durationSeconds"]?.toLongOrNull()
            ?: return ToolResult(false, "", error = "Provide a positive duration in seconds.")
        if (durationSeconds <= 0) {
            return ToolResult(false, "", error = "Timer duration must be positive.")
        }

        val label = arguments["label"].orEmpty().trim().ifBlank { "AURA timer" }
        val triggerMillis = System.currentTimeMillis() + durationSeconds * 1_000
        val requestCode = (triggerMillis xor label.hashCode().toLong()).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java)
                .putExtra(AlarmReceiver.TITLE, "AURA timer finished")
                .putExtra(AlarmReceiver.LABEL, label),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            return ToolResult(false, "", error = "Exact alarm permission is required for physical timers.")
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
        return ToolResult(true, "Timer set for ${formatDuration(durationSeconds)}: $label")
    }

    override fun validateArguments(arguments: Map<String, String>): String? {
        return if (arguments["durationSeconds"].isNullOrBlank()) {
            "Provide durationSeconds."
        } else {
            null
        }
    }

    private fun formatDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (minutes > 0) "${minutes}m ${remainingSeconds}s" else "${seconds}s"
    }
}
