package de.burkha.calendar.data

import java.time.LocalDateTime

data class CalendarEvent(
    val id: Long,
    val calendarId: Long,
    var title: String,
    var start: LocalDateTime,
    var end: LocalDateTime,
    var allDay: Boolean,
    var location: String?,
    var color: Int
)

