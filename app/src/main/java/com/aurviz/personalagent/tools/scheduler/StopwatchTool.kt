package com.aurviz.personalagent.tools.scheduler

import android.os.SystemClock
import com.aurviz.personalagent.domain.tools.AgentTool
import com.aurviz.personalagent.domain.tools.ToolParameter
import com.aurviz.personalagent.domain.tools.ToolParameterSchema
import com.aurviz.personalagent.domain.tools.ToolResult

class StopwatchTool : AgentTool {
    override val name = "stopwatch"
    override val description =
        "Display-only stopwatch with a live elapsed-time reading. Actions: start, stop, reset, or status."
    override val requiresConfirmation = false
    override val parameterSchema = ToolParameterSchema(
        listOf(
            ToolParameter("action", "string", "start, stop, reset, or status", required = true)
        )
    )

    private var startedAt: Long? = null
    private var elapsedBeforePause = 0L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        return when (arguments["action"].orEmpty().trim().lowercase()) {
            "start" -> {
                if (startedAt == null) startedAt = SystemClock.elapsedRealtime()
                ToolResult(true, "Stopwatch: ${formatElapsed()} (running)")
            }
            "stop" -> {
                elapsedBeforePause = elapsedMillis()
                startedAt = null
                ToolResult(true, "Stopwatch: ${formatElapsed()} (stopped)")
            }
            "reset" -> {
                startedAt = null
                elapsedBeforePause = 0L
                ToolResult(true, "Stopwatch: 00:00.00")
            }
            "status" -> ToolResult(
                true,
                "Stopwatch: ${formatElapsed()} (${if (startedAt == null) "stopped" else "running"})"
            )
            else -> ToolResult(false, "", error = "Action must be start, stop, reset, or status.")
        }
    }

    override fun validateArguments(arguments: Map<String, String>): String? {
        return if (arguments["action"].isNullOrBlank()) "Provide a stopwatch action." else null
    }

    private fun elapsedMillis(): Long {
        return elapsedBeforePause + (startedAt?.let { SystemClock.elapsedRealtime() - it } ?: 0L)
    }

    private fun formatElapsed(): String {
        val milliseconds = elapsedMillis()
        val minutes = milliseconds / 60_000
        val seconds = (milliseconds / 1_000) % 60
        val hundredths = (milliseconds % 1_000) / 10
        return "%02d:%02d.%02d".format(minutes, seconds, hundredths)
    }
}
