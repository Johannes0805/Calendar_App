package de.burkha.calendar.ui.components

import de.burkha.calendar.data.CalendarEvent
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Kleine Rechen- und Formatierhelfer, die Tag, Woche und Monat gemeinsam benutzen.
// "internal" = überall in deiner App sichtbar, aber nicht außerhalb davon.

internal val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
internal val DAY_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN)

/** Liegt der Termin (auch nur teilweise) an diesem Tag? */
internal fun overlapsDay(event: CalendarEvent, day: LocalDate): Boolean =
    event.end > day.atStartOfDay() && event.start < day.plusDays(1).atStartOfDay()

/** Termine eines Tages: ganztägige zuerst, dann nach Uhrzeit. */
internal fun eventsOn(day: LocalDate, events: List<CalendarEvent>): List<CalendarEvent> =
    events.filter { overlapsDay(it, day) }
        .sortedWith(compareBy<CalendarEvent>({ !it.allDay }, { it.start }))

/** Verplante Minuten an einem Tag. Termine über Mitternacht zählen nur mit ihrem Anteil an diesem Tag. */
internal fun plannedMinutes(day: LocalDate, events: List<CalendarEvent>): Long {
    val dayStart = day.atStartOfDay()
    val dayEnd = day.plusDays(1).atStartOfDay()
    return events.filter { !it.allDay }.sumOf { event ->
        val from = if (event.start > dayStart) event.start else dayStart
        val to = if (event.end < dayEnd) event.end else dayEnd
        if (to > from) Duration.between(from, to).toMinutes() else 0L
    }
}

/** 165 -> "2 h 45 min", 60 -> "1 h", 40 -> "40 min" */
internal fun formatDuration(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "$m min"
        m == 0L -> "$h h"
        else -> "$h h $m min"
    }
}

/** 270 -> "4,5 h", 120 -> "2 h" (auf halbe Stunden gerundet) */
internal fun formatHours(minutes: Long): String {
    val halfHours = Math.round(minutes / 30.0)
    return if (halfHours % 2 == 0L) "${halfHours / 2} h" else "${halfHours / 2},5 h"
}
