package com.aurviz.personalagent.scheduler

import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

data class ParsedSchedule(
    val expression: String,
    val frequency: Frequency,
    val time: java.time.LocalTime,
    val dayOfWeek: DayOfWeek? = null,
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val nextRunAt: Long
)

enum class Frequency { DAILY, WEEKLY }

object ScheduleParser {
    private val timePattern = Regex("""(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""", RegexOption.IGNORE_CASE)

    fun parse(instruction: String, now: ZonedDateTime = ZonedDateTime.now()): ParsedSchedule {
        require(instruction.isNotBlank()) { "Scheduling instruction cannot be blank" }
        val normalized = instruction.lowercase(Locale.US)
        val frequency = when {
            normalized.contains("every day") || normalized.contains("daily") ||
                normalized.contains("every evening") || normalized.contains("every morning") -> Frequency.DAILY
            normalized.contains("every monday") || normalized.contains("every tuesday") ||
                normalized.contains("every wednesday") || normalized.contains("every thursday") ||
                normalized.contains("every friday") || normalized.contains("every saturday") ||
                normalized.contains("every sunday") -> Frequency.WEEKLY
            else -> throw IllegalArgumentException(
                "Use a daily or weekly schedule, for example 'every day at 9 AM'."
            )
        }
        val day = DayOfWeek.entries.firstOrNull {
            normalized.contains("every ${it.name.lowercase(Locale.US)}")
        }
        val explicitTime = timePattern.find(normalized)?.let { match ->
            var hour = match.groupValues[1].toInt()
            val minute = match.groupValues[2].ifBlank { "0" }.toInt()
            when (match.groupValues[3].lowercase(Locale.US)) {
                "pm" -> if (hour < 12) hour += 12
                "am" -> if (hour == 12) hour = 0
            }
            require(hour in 0..23 && minute in 0..59) { "Invalid schedule time." }
            java.time.LocalTime.of(hour, minute)
        }
        val fallbackTime = when {
            normalized.contains("morning") -> java.time.LocalTime.of(9, 0)
            normalized.contains("evening") -> java.time.LocalTime.of(20, 0)
            else -> null
        }
        val time = explicitTime ?: fallbackTime
            ?: throw IllegalArgumentException("Specify a time, for example 'at 9 AM'.")
        val next = nextRun(now, frequency, time, day)
        return ParsedSchedule(
            expression = normalized,
            frequency = frequency,
            time = time,
            dayOfWeek = day,
            nextRunAt = next.toInstant().toEpochMilli()
        )
    }

    private fun nextRun(
        now: ZonedDateTime,
        frequency: Frequency,
        time: java.time.LocalTime,
        day: DayOfWeek?
    ): ZonedDateTime {
        var candidate = now.with(time).withSecond(0).withNano(0)
        if (frequency == Frequency.WEEKLY) {
            val target = day ?: throw IllegalArgumentException("Specify a weekday for a weekly schedule.")
            var days = (target.value - now.dayOfWeek.value + 7) % 7
            if (days == 0 && !candidate.isAfter(now)) days = 7
            candidate = candidate.plusDays(days.toLong())
        } else if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }
}
