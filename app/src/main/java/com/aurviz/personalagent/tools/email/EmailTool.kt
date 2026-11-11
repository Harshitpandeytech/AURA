package com.aurviz.personalagent.tools.email

import com.aurviz.personalagent.data.remote.GmailApiService
import com.aurviz.personalagent.data.remote.GmailDraft
import com.aurviz.personalagent.domain.model.PendingAction
import com.aurviz.personalagent.domain.model.PendingActionStatus
import com.aurviz.personalagent.domain.repository.PendingActionRepository
import com.aurviz.personalagent.domain.tools.AgentTool
import com.aurviz.personalagent.domain.tools.ConfirmableTool
import com.aurviz.personalagent.domain.tools.ToolParameter
import com.aurviz.personalagent.domain.tools.ToolParameterSchema
import com.aurviz.personalagent.domain.tools.ToolResult
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class EmailTool(
    private val gmail: GmailApiService,
    private val pendingActions: PendingActionRepository
) : AgentTool, ConfirmableTool {
    override val name = "send_email"
    override val description = "Prepare an email draft. Sending always requires explicit user confirmation."
    override val requiresConfirmation = true
    override val parameterSchema = ToolParameterSchema(
        listOf(
            ToolParameter("to", "string", "Recipient email address"),
            ToolParameter("subject", "string", "Email subject"),
            ToolParameter("body", "string", "Email body")
        )
    )

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        if (!gmail.isConnected()) return ToolResult(false, "", error = "Connect Gmail before sending email.")
        val draft = GmailDraft(
            to = arguments.getValue("to"),
            subject = arguments.getValue("subject"),
            body = arguments.getValue("body")
        )
        val argumentsJson = buildJsonObject {
            put("to", draft.to)
            put("subject", draft.subject)
            put("body", draft.body)
        }.toString()
        pendingActions.insertAction(PendingAction(toolName = name, arguments = argumentsJson))
        return ToolResult(
            true,
            "Email draft prepared for ${draft.to}. Awaiting explicit confirmation before sending.",
            mapOf("to" to draft.to, "subject" to draft.subject, "body" to draft.body)
        )
    }

    override suspend fun confirm(): ToolResult {
        pendingActions.deleteExpiredActions()
        val pending = pendingActions.getPendingActions().first().firstOrNull { it.toolName == name }
            ?: return ToolResult(false, "", error = "No email draft is awaiting confirmation.")
        val args = Json.parseToJsonElement(pending.arguments).jsonObject
        val draft = GmailDraft(
            to = args["to"]?.jsonPrimitive?.content ?: error("Pending email has no recipient"),
            subject = args["subject"]?.jsonPrimitive?.content.orEmpty(),
            body = args["body"]?.jsonPrimitive?.content.orEmpty()
        )
        gmail.send(draft)
        pendingActions.updateStatus(pending.id, PendingActionStatus.EXECUTED)
        return ToolResult(true, "Email sent to ${draft.to}.")
    }

    override fun validateArguments(arguments: Map<String, String>): String? {
        val missing = parameterSchema.parameters.filter { arguments[it.name].isNullOrBlank() }
        if (missing.isNotEmpty()) return "Missing required arguments: ${missing.joinToString { it.name }}"
        if (!arguments.getValue("to").contains("@")) return "Recipient must be a valid email address"
        return null
    }
}

class ImportantEmailTool(private val gmail: GmailApiService) : AgentTool {
    override val name = "get_important_emails"
    override val description = "Retrieve the user's important Gmail messages."
    override val requiresConfirmation = false
    override val parameterSchema = ToolParameterSchema(emptyList())

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val messages = gmail.importantMessages()
        return ToolResult(
            true,
            messages.joinToString("\n") { "${it.id}: ${it.subject} - ${it.snippet}" }
                .ifBlank { "No important emails found." }
        )
    }

    override fun validateArguments(arguments: Map<String, String>): String? = null
}

class ReadEmailTool(private val gmail: GmailApiService) : AgentTool {
    override val name = "read_email"
    override val description = "Read the content of a Gmail message by ID."
    override val requiresConfirmation = false
    override val parameterSchema = ToolParameterSchema(
        listOf(ToolParameter("id", "string", "Gmail message ID"))
    )

    override suspend fun execute(arguments: Map<String, String>): ToolResult =
        ToolResult(true, gmail.readMessage(arguments.getValue("id")))

    override fun validateArguments(arguments: Map<String, String>): String? =
        if (arguments["id"].isNullOrBlank()) "Missing required arguments: id" else null
}
