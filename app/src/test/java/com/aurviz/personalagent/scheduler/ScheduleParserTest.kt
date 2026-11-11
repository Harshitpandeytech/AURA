package com.aurviz.personalagent.scheduler

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleParserTest {
    private val now = ZonedDateTime.of(2026, 11, 11, 8, 0, 0, 0, ZoneId.of("UTC"))

    @Test
    fun parsesDailyTime() {
        val parsed = ScheduleParser.parse("Every day at 9 AM, check important emails", now)
        assertEquals(Frequency.DAILY, parsed.frequency)
        assertEquals(9, parsed.time.hour)
        assertTrue(parsed.nextRunAt > now.toInstant().toEpochMilli())
    }

    @Test
    fun parsesWeeklyTime() {
        val parsed = ScheduleParser.parse("Every Monday at 10 AM, summarize mail", now)
        assertEquals(Frequency.WEEKLY, parsed.frequency)
        assertEquals(java.time.DayOfWeek.MONDAY, parsed.dayOfWeek)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAmbiguousSchedule() {
        ScheduleParser.parse("Check important emails", now)
    }
}
