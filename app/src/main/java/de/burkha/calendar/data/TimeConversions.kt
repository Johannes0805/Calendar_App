package de.burkha.calendar.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

// This is a Class to simplify converting LocalDateTime to Milliseconds as it is required often
    fun zoneFor(allDay: Boolean): ZoneId{
        return if (allDay) ZoneOffset.UTC else ZoneId.systemDefault()
    }

    fun toMillis(allDay: Boolean, time: LocalDateTime): Long{
        val zone = zoneFor(allDay)
        return time.atZone(zone).toInstant().toEpochMilli()

    }

    fun toLocalDateTime(allDay: Boolean, time: Long): LocalDateTime{
        val zone = zoneFor(allDay)
        return Instant.ofEpochMilli(time).atZone(zone).toLocalDateTime()
    }
