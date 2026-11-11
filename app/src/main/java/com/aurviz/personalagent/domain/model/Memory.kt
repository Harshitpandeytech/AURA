package com.aurviz.personalagent.domain.model

data class Memory(
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
