package de.burkha.calendar.data
import java.time.LocalDateTime

/**
 * Event from CalendarContract.Instances
 */

data class CalendarEvent(
    /** CalendarContract.Instances.EVENT_ID */
    val id: Long,
    /** CalendarContract.Instances.CALENDAR_ID */
    val calendarId: Long,
    /** CalendarContract.Instances.TITLE */
    var title: String,
    /** CalendarContract.Instances.BEGIN */
    var start: LocalDateTime,
    /** CalendarContract.Instances.END */
    var end: LocalDateTime,
    /** CalendarContract.Instances.ALL_DAY */
    var allDay: Boolean,
    /** CalendarContract.Instances.EVENT_LOCATION */
    var location: String? = null,
    /** CalendarContract.Instances.DISPLAY_COLOR */
    var color: Int,
    /** CalendarContract.Instances.DESCRIPTION */
    var description: String? = null,
    /** CalendarContract.Instances.RRULE */
    var isRecurring: Boolean = false,

    /**
     * If the event is not recurring -> null
     * If the event is part of a series -> eventId
     * If the event is an exception -> id of original recurring event
     */
    var seriesId: Long? = null,

    /**
     * If the event is not recurring -> null
     * If the event is part of a series -> start
     * If the event is an exception -> original start of the event
     */
    var originalStart: LocalDateTime? = null,
)

