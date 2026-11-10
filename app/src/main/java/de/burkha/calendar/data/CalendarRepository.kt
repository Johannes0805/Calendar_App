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
import java.time.Duration
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


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
            CalendarContract.Instances.EVENT_ID,        // 0
            CalendarContract.Instances.CALENDAR_ID,     // 1
            CalendarContract.Instances.TITLE,           // 2
            CalendarContract.Instances.BEGIN,           // 3
            CalendarContract.Instances.END,             // 4
            CalendarContract.Instances.ALL_DAY,         // 5
            CalendarContract.Instances.EVENT_LOCATION,  // 6
            CalendarContract.Instances.DISPLAY_COLOR,   // 7
            CalendarContract.Instances.DESCRIPTION,     // 8
            CalendarContract.Instances.RRULE,           // 9
            CalendarContract.Instances.ORIGINAL_ID,     // 10
            CalendarContract.Instances.ORIGINAL_INSTANCE_TIME, // 11
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

                val originalId = c.getLong(10)

                val isException: Boolean = !c.isNull(10)
                val isSeries: Boolean = !c.getString(9).isNullOrBlank()

                val originalStart: LocalDateTime? = when {
                    isException -> toLocalDateTime(allDay, c.getLong(11))
                    isSeries -> start
                    else -> null
                }

                val seriesId: Long? = when {
                    isException -> originalId
                    isSeries -> c.getLong(0)
                    else -> null
                }

                result.add(
                    CalendarEvent(
                        id =c.getLong(0),
                        calendarId = c.getLong(1),
                        title = c.getString(2) ?: "(Ohne Titel)",
                        start = start,
                        end = end,
                        allDay = allDay,
                        location = if (c.getString(6).isNullOrBlank()){null} else
                        {c.getString(6)},
                        color = c.getInt(7),
                        description = if (c.getString(8).isNullOrBlank()) {null} else
                        {c.getString(8)},
                        isRecurring = seriesId != null,
                        seriesId = seriesId,
                        originalStart = originalStart
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
     *  Saves changes on an existing exception
     *
     *  @return true if updated successfully, false if not
     */
    fun updateException(draft: EventDraft) : Boolean{
            val values = toContentValues(draft)
            values.remove(CalendarContract.Events.CALENDAR_ID)
            values.putNull(CalendarContract.Events.DURATION)
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, draft.id!!)
            return contentResolver.update(uri, values, null, null) == 1
    }


    fun cancelException(draft: EventDraft) : Boolean{
        val values = toContentValues(draft)
        values.remove(CalendarContract.Events.CALENDAR_ID)
        values.putNull(CalendarContract.Events.DURATION)
        values.put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CANCELED)
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, draft.id!!)
        return contentResolver.update(uri, values, null, null) == 1
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
    fun updateSeries(seriesId: Long, shift: Duration, draft: EventDraft): Boolean{
        val values = toContentValues(draft)
        // Create URI for the series
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, seriesId)

        val proj = arrayOf(
            CalendarContract.Events.RRULE,
            CalendarContract.Events.DTSTART
        )

        // Get the RRULE of the series as it is not specified in the draft
        val c2 = contentResolver.query(uri, proj, null, null)

        c2?.use {
            // If there is a RRULE, put it to values else return
            if (!c2.moveToFirst() || c2.isNull(1)) return false
            val rrule = c2.getString(0) ?: return false
            values.put(
                CalendarContract.Events.RRULE,
                rrule)
            values.put(CalendarContract.Events.DTSTART, toMillis(draft.allDay,
                toLocalDateTime(draft.allDay,c2.getLong(1)) + shift))
        } ?: return false


        // Need to be removed in order for android to accept the change of a recurring event
        values.putNull(CalendarContract.Events.DTEND)
        values.remove(CalendarContract.Events.CALENDAR_ID)
        // Set new start of events of the series

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
            var until = ""
            if (draft.recurrenceEnd != null){
                if (draft.allDay){
                    val form = DateTimeFormatter.ofPattern("yyyyMMdd")
                    until = ";UNTIL=" + draft.recurrenceEnd.format(form)
                }else{
                    val form = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                    until = ";UNTIL=" + draft.recurrenceEnd.atTime(23,59,59
                    ).atZone(systemDefault()).withZoneSameInstant(ZoneOffset.UTC).format(form)
                }

            }

            values.put(
                CalendarContract.Events.RRULE,
                when (draft.recurrence){
                    Recurrence.NONE -> null
                    Recurrence.DAILY -> "FREQ=DAILY$until"
                    Recurrence.WEEKLY -> "FREQ=WEEKLY$until"
                    Recurrence.MONTHLY -> "FREQ=MONTHLY$until"
                    Recurrence.YEARLY -> "FREQ=YEARLY$until"
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