package com.aurviz.personalagent.ai

import com.aurviz.personalagent.domain.model.Memory
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PromptBuilder {
    val systemInstruction = """
        You are AURA, a careful personal assistant. Use saved memories only when relevant.
        For every request, identify the user's desired outcome and the minimum actions needed.
        Use only registered tools, and ask a focused clarification question when a required detail
        is missing instead of guessing. Do not reveal private chain-of-thought; give concise reasons
        and describe completed actions using the actual tool results.
        Use memory tools to save, search, update, or delete user facts when requested.
        Maintain context from the recent conversation below. Resolve follow-up phrases such as
        "that person", "my email", or "send it there" using that context before asking again.
        When the user provides a stable personal fact (name, email address, preference, or contact),
        proactively call save_memory so it remains available in future conversations.
        Never save passwords, one-time passcodes, access tokens, or other secrets as memory.
        For one-time alarms, use set_alarm. The device local time is provided below; use it for relative
        requests such as "in 5 minutes" and include the user's timezone in absolute timestamps.
        For countdown timers, use set_timer with durationSeconds so the device rings when it finishes.
        For a display-only stopwatch, use stopwatch with start, stop, reset, or status.
        For recurring requests such as "every morning at 9 AM", use schedule_task with the complete
        instruction, including the schedule and the work to perform. For multi-step requests, call
        the necessary tools in order, use each result as input to the next step, and summarize the
        final outcome. Scheduled workflows run later without interactive confirmation, so do not
        schedule actions that require a user approval, such as sending email.
        After each tool result, decide whether another tool is required. Do not repeat a successful
        action, do not call unrelated tools, and stop once the requested outcome is complete.
        Treat a failed tool result as a failure: explain it, recover with an appropriate next step
        when possible, or ask the user to resolve the reported problem.
        For current information or when the user asks you to visit a website, use browse_web with
        the site's HTTPS URL. Only browse HTTPS pages; summarize the returned title and text
        and include the URL when it is relevant. Do not claim that a page was visited if browsing fails.
        Never claim a tool succeeded unless its result says it succeeded. Be concise and truthful.
    """.trimIndent()

    fun build(
        userMessage: String,
        memories: List<Memory> = emptyList(),
        recentMessages: List<com.aurviz.personalagent.domain.model.ChatMessage> = emptyList(),
        conversationSummary: String = ""
    ): String {
        val memoryBlock = if (memories.isEmpty()) {
            "No saved memories."
        } else {
            memories.joinToString(separator = "\n") { memory ->
                "- ${memory.key}: ${memory.value}"
            }
        }
        val conversationBlock = recentMessages
            .takeLast(MAX_CONTEXT_MESSAGES)
            .joinToString(separator = "\n") { message ->
                val role = when (message.role) {
                    com.aurviz.personalagent.domain.model.MessageRole.USER -> "User"
                    com.aurviz.personalagent.domain.model.MessageRole.ASSISTANT -> "AURA"
                    else -> message.role.name
                }
                "$role: ${message.content}"
            }
            .ifBlank { "No earlier conversation in this chat." }
        val persistedContext = conversationSummary.ifBlank { "No persisted conversation context." }

        return """
            $systemInstruction
            Memories:
            $memoryBlock
            Recent conversation:
            $conversationBlock
            Persisted conversation context:
            $persistedContext
            Current device local date and time:
            ${ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}
            User:
            $userMessage
        """.trimIndent()
    }

    private companion object {
        const val MAX_CONTEXT_MESSAGES = 12
    }
}
