package de.burkha.calendar.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import java.time.LocalDate
import kotlin.collections.mutableListOf
import java.time.LocalDateTime
import java.time.ZoneId.systemDefault
class CalendarRepository(private val contentResolver: ContentResolver) {
    /**
        Loads all available calendars from the android storage

        Requires permission READ_CALENDAR, otherwise throws SecurityException
     */
    fun getCalendars():List<CalendarInfo>{
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
            )
        val result = mutableListOf<CalendarInfo>()
        val cursor: Cursor? = contentResolver.query(uri, projection, null,null,null)

        cursor?.use{ c ->
            while(c.moveToNext()) {
                result.add(
                    CalendarInfo(
                        c.getLong(0),
                        c.getString(1),
                        c.getInt(2),
                        c.getString(3),
                        (c.getInt(4) == 1),
                        c.getInt(5) >=
                                CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
                    )
                )
            }
        }
        return result
    }

    /**
     * Loads all events from all calendars
     *
     * Using from and to (inclusive) the range of date can be restricted
     */
    fun getEvents(from: LocalDate, to: LocalDate): List<CalendarEvent>{
        // Determines in which order and what information for each event is fetched
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.RRULE,
            CalendarContract.Instances.ORIGINAL_ID,
            CalendarContract.Instances.DTSTART
        )

        // Convert the Date to a certain time in millis for the query to work
        val beginWindow: Long = toMillis(false, from.atStartOfDay())
        val endWindow: Long = toMillis(false, to.plusDays(1).atStartOfDay())

        val result = mutableListOf<CalendarEvent>()
        val cursor = CalendarContract.Instances.query(contentResolver, projection, beginWindow, endWindow)

        cursor?.use{ c ->
            while(c.moveToNext()){
                val allDay = c.getInt(5) == 1
                val start: LocalDateTime = toLocalDateTime(allDay, c.getLong(3))
                val end: LocalDateTime = toLocalDateTime(allDay, c.getLong(4))

                // Indicates whether an event is recurring by viewing RRULE and ORIGINAL_ID
                val recurring: Boolean =  !c.getString(9).isNullOrBlank()||
                            !c.getString(10).isNullOrBlank()

                // Date of the first event in a series, if event is not part of a series it is set
                // to the start of the event
                val seriesStart: LocalDateTime? = if (recurring)
                    toLocalDateTime(allDay, c.getLong(11)) else start

                result.add(
                    CalendarEvent(
                        c.getLong(0),
                        c.getLong(1),
                        c.getString(2) ?: "(Ohne Titel)",
                        start,
                        end,
                        allDay,
                        if (c.getString(6).isNullOrBlank()){null} else {c.getString(6)},
                        c.getInt(7),
                        if (c.getString(8).isNullOrBlank()) {null} else {c.getString(8)},
                        recurring,
                        seriesStart
                    )
                )
            }

        }
        return result
            .filter { it.end > toLocalDateTime(false, beginWindow) &&
                    it.start < toLocalDateTime(false, endWindow) }
            .sortedBy { it.start }
    }

    /**
     * Saves a new event from the draft to the Android calendar provider.
     *
     * @return the ID of the new event, or -1 if the insert failed
     */
    fun insertEvent(draft: EventDraft): Long{
        val newEventUri: Uri? = contentResolver.insert(CalendarContract.Events.CONTENT_URI, toContentValues(draft))
        return if (newEventUri != null)
            ContentUris.parseId(newEventUri) else -1
    }

    /**
     * Saves the changes on an existing event
     *
     * @return true if something has changed, false if the event is new or if nothing has changed
     */
    fun updateEvent(draft: EventDraft): Boolean{
        // Check if the event is completely new or an update
        val id = draft.id
        if (id == null){
            return false
        }else{
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
            val updatedRows: Int = contentResolver.update(uri, toContentValues(draft), null, null)
            return updatedRows != 0
        }
    }

    /**
     * Deletes an event given the eventId
     *
     * @return true if something gets deleted, false if nothing was deleted
     */
    fun deleteEvent(eventId: Long): Boolean{
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        return (contentResolver.delete(uri, null) != 0)
    }

    /**
     * Changes a single event of a recurring series
     *
     * @return true if the event was inserted successfully, false if it was not inserted
     */
    //TODO: Fix bug with double editing of recurring events
    fun insertException(seriesId: Long, originalStart: LocalDateTime, allDay: Boolean, draft: EventDraft): Boolean{
        val values = toContentValues(draft)
        // Those values need to be removed, as android does not allow them to be set again on a
        // recurring event => remain unchanged
        values.remove(CalendarContract.Events.DTEND)
        values.remove(CalendarContract.Events.CALENDAR_ID)
        values.remove(CalendarContract.Events.RRULE)

        // Needed to determine the end of the event as there is no DTEND in series
        values.put(CalendarContract.Events.DURATION,
            if (!draft.allDay){
                "P${draft.duration.seconds}S"
            }else{
                "P${draft.duration.toDays()}D"
            }
        )

        // Adds the regular start of the recurring event to the exception
        values.put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME,
            toMillis(allDay, originalStart))

        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_EXCEPTION_URI, seriesId)
        return contentResolver.insert(uri, values) != null
    }

    /**
     * Deletes a single event of a recurring series
     *
     * @return true if the event is canceled, false if not
     */
    fun cancelInstance(seriesId: Long, originalStart: LocalDateTime, allDay: Boolean): Boolean{
        val values = ContentValues()

        values.put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, toMillis(allDay, originalStart))
        // Status of the event is set to canceled so that it gets ignored by Instances
        values.put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CANCELED)
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_EXCEPTION_URI, seriesId)
        return contentResolver.insert(uri, values) != null
    }

    /**
     * Saves changes on all events of a recurring series
     *
     * @return true if the change was successfully, false if not
     */
    fun updateSeries(seriesId: Long, newSeriesStart: LocalDateTime, draft: EventDraft): Boolean{
        val values = toContentValues(draft)
        // Create URI for the series
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, seriesId)

        val proj = arrayOf(CalendarContract.Instances.RRULE)

        // Get the RRULE of the series as it is not specified in the draft
        val c2 = contentResolver.query(uri, proj, null, null)

        c2?.use {
            // If there is a RRULE, put it to values else return
            if (!c2.moveToFirst()) return false
            values.put(
                CalendarContract.Events.RRULE,
                c2.getString(0))
        }


        // Need to be removed in order for android to accept the change of a recurring event
        values.putNull(CalendarContract.Events.DTEND)
        values.remove(CalendarContract.Events.CALENDAR_ID)
        // Set new start of events of the series
        values.put(CalendarContract.Events.DTSTART, toMillis(draft.allDay, newSeriesStart))
        // Set end of the events of the series
        values.put(CalendarContract.Events.DURATION,
            if (!draft.allDay){
                "P${draft.duration.seconds}S"
            }else{
                "P${draft.duration.toDays()}D"
            }
        )
        return contentResolver.update(uri, values, null, null) == 1
    }

    /**
     * Helper method, converts an eventdraft into a ContentValue
     */
    private fun toContentValues(draft: EventDraft): ContentValues {
        // Create empty ContentValues
        val values = ContentValues()

        // Event is not recurring
        if (draft.recurrence == Recurrence.NONE){
            values.put(CalendarContract.Events.DTEND, toMillis(draft.allDay, draft.end))
            // Ensure that the event won't be detected as recurring by accident
            values.putNull(CalendarContract.Events.RRULE)
            values.putNull(CalendarContract.Events.DURATION)
        // Event is recurring
        } else{
            values.put(
                CalendarContract.Events.RRULE,
                when (draft.recurrence){
                    Recurrence.DAILY -> "FREQ=DAILY"
                    Recurrence.WEEKLY -> "FREQ=WEEKLY"
                    Recurrence.MONTHLY -> "FREQ=MONTHLY"
                    Recurrence.YEARLY -> "FREQ=YEARLY"
                    Recurrence.NONE -> null
                }
            )
            values.put(CalendarContract.Events.DURATION,
                if (!draft.allDay){
                    "P${draft.duration.seconds}S"
                }else{
                    "P${draft.duration.toDays()}D"
                }
            )
            values.putNull(CalendarContract.Events.DTEND)
        }

        // Common Values for both: recurring and non-recurring
        values.put(CalendarContract.Events.CALENDAR_ID, draft.calendarId)
        values.put(CalendarContract.Events.TITLE, draft.title)
        values.put(CalendarContract.Events.DTSTART, toMillis(draft.allDay, draft.start))
        values.put(CalendarContract.Events.ALL_DAY, if (draft.allDay) 1 else 0)
        values.put(CalendarContract.Events.EVENT_LOCATION, draft.location)
        values.put(CalendarContract.Events.DESCRIPTION, draft.note)
        values.put(CalendarContract.Events.EVENT_TIMEZONE, if (draft.allDay) "UTC" else systemDefault().id)

        return values
    }
}