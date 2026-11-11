package com.aurviz.personalagent.ai

import com.aurviz.personalagent.domain.model.Memory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {
    @Test
    fun build_includesUserMessageAndMemories() {
        val prompt = PromptBuilder().build(
            userMessage = "What city do I live in?",
            memories = listOf(Memory(key = "city", value = "Pune", category = "fact"))
        )
        assertTrue(prompt.contains("What city do I live in?"))
        assertTrue(prompt.contains("city: Pune"))
    }
}

class ToolCallParserTest {
    private val parser = ToolCallParser()

    @Test
    fun parse_readsToolNameAndArguments() {
        val calls = parser.parse(
            """[{"name":"save_memory","arguments":{"key":"city","value":"Pune"}}]"""
        )
        assertEquals(1, calls.size)
        assertEquals("save_memory", calls[0].toolName)
        assertEquals("Pune", calls[0].arguments["value"])
    }

    @Test
    fun parse_blankOrInvalid_returnsEmpty() {
        assertTrue(parser.parse("").isEmpty())
        assertTrue(parser.parse("not-json").isEmpty())
    }
}

class GeminiAgentTest {
    @Test
    fun complete_isDisabled() {
        val agent = GeminiAgent()
        assertTrue(!agent.isEnabled)
        try {
            kotlinx.coroutines.runBlocking { agent.complete("hello") }
            throw AssertionError("AI completion should stay disabled")
        } catch (error: IllegalStateException) {
            assertEquals("AI integration is not enabled", error.message)
        }
    }
}
