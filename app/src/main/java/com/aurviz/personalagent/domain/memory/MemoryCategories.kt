package com.aurviz.personalagent.domain.memory

object MemoryCategories {
    const val GENERAL = "general"
    const val PREFERENCE = "preference"
    const val FACT = "fact"

    val defaults: List<String> = listOf(GENERAL, PREFERENCE, FACT)
}
