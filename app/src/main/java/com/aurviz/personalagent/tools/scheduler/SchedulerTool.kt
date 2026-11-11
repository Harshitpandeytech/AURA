package com.aurviz.personalagent.tools.scheduler

import com.aurviz.personalagent.domain.model.ScheduledTask
import com.aurviz.personalagent.domain.repository.ScheduledTaskRepository
import com.aurviz.personalagent.domain.scheduler.AgentScheduler
import com.aurviz.personalagent.domain.tools.AgentTool
import com.aurviz.personalagent.domain.tools.ToolParameter
import com.aurviz.personalagent.domain.tools.ToolParameterSchema
import com.aurviz.personalagent.domain.tools.ToolResult
import com.aurviz.personalagent.scheduler.ScheduleParser

class SchedulerTool(
    private val repository: ScheduledTaskRepository,
    private val scheduler: AgentScheduler
) : AgentTool {
    override val name = "schedule_task"
    override val description = "Create a recurring daily or weekly task."
    override val requiresConfirmation = false
    override val parameterSchema = ToolParameterSchema(
        listOf(ToolParameter("instruction", "string", "Full recurring task instruction"))
    )

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val instruction = arguments.getValue("instruction").trim()
        val parsed = ScheduleParser.parse(instruction)
        val id = repository.insertTask(
            ScheduledTask(
                instruction = instruction,
                scheduleExpression = parsed.expression,
                toolConfig = instruction,
                nextRunAt = parsed.nextRunAt
            )
        )
        scheduler.enqueue(id)
        return ToolResult(
            success = true,
            output = "Scheduled task $id for ${parsed.expression}.",
            data = mapOf("taskId" to id, "nextRunAt" to parsed.nextRunAt)
        )
    }

    override fun validateArguments(arguments: Map<String, String>): String? =
        if (arguments["instruction"].isNullOrBlank()) {
            "Missing required arguments: instruction"
        } else {
            null
        }
}
