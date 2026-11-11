package com.aurviz.personalagent.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeUtils {
    private val displayFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    fun nowMillis(): Long = System.currentTimeMillis()

    fun formatDisplay(epochMillis: Long): String {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(epochMillis),
            ZoneId.systemDefault()
        )
        return dateTime.format(displayFormatter)
    }

    fun formatTime(epochMillis: Long): String {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(epochMillis),
            ZoneId.systemDefault()
        )
        return dateTime.format(timeFormatter)
    }

    fun formatDate(epochMillis: Long): String {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(epochMillis),
            ZoneId.systemDefault()
        )
        return dateTime.format(dateFormatter)
    }
}
