package com.aurviz.personalagent.ai

import com.aurviz.personalagent.domain.tools.AgentTool
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class GeminiToolDeclaration(val declarations: JsonArray)

internal fun toolDeclarations(tools: List<AgentTool>): JsonArray =
    JsonArray(
        tools.map { tool ->
            buildJsonObject {
                put(
                    "functionDeclarations",
                    JsonArray(
                        listOf(
                            buildJsonObject {
                                put("name", tool.name)
                                put("description", tool.description)
                                put(
                                    "parameters",
                                    buildJsonObject {
                                        put("type", "OBJECT")
                                        put(
                                            "properties",
                                            buildJsonObject {
                                                tool.parameterSchema.parameters.forEach { parameter ->
                                                    put(
                                                        parameter.name,
                                                        buildJsonObject {
                                                            put("type", parameter.type.uppercase())
                                                            put("description", parameter.description)
                                                        }
                                                    )
                                                }
                                            }
                                        )
                                        put(
                                            "required",
                                            JsonArray(
                                                tool.parameterSchema.parameters
                                                    .filter { it.required }
                                                    .map { parameter -> JsonPrimitive(parameter.name) }
                                            )
                                        )
                                    }
                                )
                            }
                        )
                    )
                )
            }
        }
    )
