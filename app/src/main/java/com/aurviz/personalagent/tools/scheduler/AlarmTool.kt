package com.aurviz.personalagent.tools.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.aurviz.personalagent.domain.tools.AgentTool
import com.aurviz.personalagent.domain.tools.ToolParameter
import com.aurviz.personalagent.domain.tools.ToolParameterSchema
import com.aurviz.personalagent.domain.tools.ToolResult
import java.time.Instant

class AlarmTool(private val context: Context) : AgentTool {
    override val name = "set_alarm"
    override val description =
        "Set a one-time device alarm. Convert the requested local time to an ISO-8601 timestamp with timezone."
    override val requiresConfirmation = false
    override val parameterSchema = ToolParameterSchema(
        listOf(
            ToolParameter("triggerAt", "string", "ISO-8601 timestamp including timezone", required = false),
            ToolParameter("delayMinutes", "integer", "Minutes from now for a relative alarm", required = false),
            ToolParameter("label", "string", "Short alarm label", required = false)
        )
    )

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val triggerMillis = arguments["delayMinutes"]?.toLongOrNull()?.let { minutes ->
            if (minutes <= 0) return ToolResult(false, "", error = "Alarm delay must be positive.")
            System.currentTimeMillis() + minutes * 60_000
        } ?: try {
            Instant.parse(arguments["triggerAt"].orEmpty()).toEpochMilli()
        } catch (_: Exception) {
            return ToolResult(
                false,
                "",
                error = "Provide either a future ISO-8601 time or a positive delay in minutes."
            )
        }
        if (triggerMillis <= System.currentTimeMillis()) {
            return ToolResult(false, "", error = "Alarm time must be in the future.")
        }

        val label = arguments["label"].orEmpty().trim().ifBlank { "AURA alarm" }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val requestCode = (triggerMillis xor label.hashCode().toLong()).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, AlarmReceiver::class.java).putExtra(AlarmReceiver.LABEL, label),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val exact = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        if (!exact && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        if (exact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
        return ToolResult(
            true,
            if (exact) "Alarm set for ${java.time.Instant.ofEpochMilli(triggerMillis)}: $label"
            else "Alarm scheduled approximately for ${java.time.Instant.ofEpochMilli(triggerMillis)} because exact-alarm permission is disabled: $label"
        )
    }

    override fun validateArguments(arguments: Map<String, String>): String? {
        if (arguments["triggerAt"].isNullOrBlank() && arguments["delayMinutes"].isNullOrBlank()) {
            return "Provide either triggerAt or delayMinutes."
        }
        return null
    }
}
