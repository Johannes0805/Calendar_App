package de.burkha.calendar.data

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Ein Termin, der gerade im Editor bearbeitet wird. Noch nicht gespeichert.
 *
 * Absichtlich getrennt von CalendarEvent: Hier darf alles "halb fertig" sein
 * (leerer Titel, noch kein Kalender gewählt). Erst beim Speichern wird daraus ein echter Termin.
 *
 * Alle with...-Funktionen ändern NICHTS am Objekt, sondern geben eine veränderte Kopie zurück
 * (copy). Genau so will Compose das: neuer Zustand = neues Objekt -> neu zeichnen.
 *
 * @param id     null = neuer Termin, sonst die Event-ID des Termins, der bearbeitet wird
 * @param end    bei ganztägigen Terminen der FOLGETAG um 00:00 (so speichert Android das auch)
 * @param recurrence  ob und wie sich der Termin wiederholt. start/end beschreiben dann den ERSTEN Termin.
 * @param recurrenceEnd  letzter Tag, an dem die Serie noch stattfinden darf (inklusive). null = endet nie.
 *                       Spielt nur eine Rolle, wenn recurrence nicht NONE ist.
 */
data class EventDraft(
    val id: Long? = null,
    val calendarId: Long? = null,
    val title: String = "",
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean = false,
    val location: String = "",
    val note: String = "",
    val recurrence: Recurrence = Recurrence.NONE,
    val recurrenceEnd: LocalDate? = null,
) {
    val isNew: Boolean get() = id == null
    val duration: Duration get() = Duration.between(start, end)

    /** Anderer Tag, gleiche Uhrzeit und gleiche Dauer. */
    fun withDate(date: LocalDate): EventDraft {
        val newStart = date.atTime(start.toLocalTime())
        return copy(start = newStart, end = newStart.plus(duration))
    }

    /** Neuer Beginn. Die Dauer bleibt gleich, das Ende rutscht also mit. */
    fun withStartTime(time: LocalTime): EventDraft {
        val newStart = start.toLocalDate().atTime(time)
        return copy(start = newStart, end = newStart.plus(duration))
    }

    /** Neues Ende. Liegt es vor dem Beginn, ist der nächste Tag gemeint (23:00 bis 01:00). */
    fun withEndTime(time: LocalTime): EventDraft {
        var newEnd = start.toLocalDate().atTime(time)
        if (newEnd <= start) newEnd = newEnd.plusDays(1)
        return copy(end = newEnd)
    }

    fun withDuration(minutes: Long): EventDraft = copy(end = start.plusMinutes(minutes))

    fun withAllDay(allDay: Boolean): EventDraft {
        val day = start.toLocalDate()
        return if (allDay) {
            copy(allDay = true, start = day.atStartOfDay(), end = day.plusDays(1).atStartOfDay())
        } else {
            copy(allDay = false, start = day.atTime(DEFAULT_START), end = day.atTime(DEFAULT_START).plusHours(1))
        }
    }

    /** Für die Vorschau im Zeitstrahl: so würde der Termin aussehen. */
    fun asPreviewEvent(color: Int): CalendarEvent = CalendarEvent(
        id = id ?: PREVIEW_ID,
        calendarId = calendarId ?: -1L,
        title = title.ifBlank { "Neuer Termin" },
        start = start,
        end = end,
        allDay = allDay,
        location = location.ifBlank { null },
        color = color,
    )

    // "companion object" = was in Java "static" wäre: gehört zur Klasse, nicht zu einem Objekt.
    // Aufruf: EventDraft.newEvent(...)
    companion object {
        const val PREVIEW_ID = -1L
        private val DEFAULT_START: LocalTime = LocalTime.of(9, 0)

        /**
         * Leerer Entwurf für einen neuen Termin, 1 Stunde lang.
         * Heute: ab der nächsten vollen Stunde. Andere Tage: ab 9 Uhr.
         */
        fun newEvent(date: LocalDate, calendarId: Long?, now: LocalDateTime = LocalDateTime.now()): EventDraft {
            val start = if (date == now.toLocalDate() && now.hour < 23) {
                now.toLocalDate().atTime(now.hour + 1, 0)
            } else {
                date.atTime(DEFAULT_START)
            }
            return EventDraft(calendarId = calendarId, start = start, end = start.plusHours(1))
        }
    }
}
