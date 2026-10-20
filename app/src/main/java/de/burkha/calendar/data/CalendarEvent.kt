package de.burkha.calendar.data

import java.time.LocalDateTime

data class CalendarEvent(
    val id: Long,
    val calendarId: Long,
    var title: String,
    var start: LocalDateTime,
    var end: LocalDateTime,
    var allDay: Boolean,
    var location: String? = null,
    var color: Int,
    var description: String? = null,
    var isRecurring: Boolean = false,
    var seriesStart: LocalDateTime? = null
)

