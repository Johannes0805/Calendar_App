package de.burkha.calendar.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlin.collections.mutableListOf
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZoneId.systemDefault
class CalendarRepository(private val contentResolver: ContentResolver) {

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


    fun getEvents(from: LocalDate, to: LocalDate): List<CalendarEvent>{
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

        val beginWindow: Long = from.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endWindow: Long = to.plusDays(1).atStartOfDay(systemDefault()).toInstant().toEpochMilli()

        val windowStart = from.atStartOfDay()
        val windowEnd = to.plusDays(1).atStartOfDay()

        val result = mutableListOf<CalendarEvent>()
        val cursor = CalendarContract.Instances.query(contentResolver, projection, beginWindow, endWindow)
        Log.d("CalendarRepo", "Zeitraum: $beginWindow – $endWindow, Treffer: ${cursor?.count}")

        cursor?.use{ c ->
            while(c.moveToNext()){

                val allDay = c.getInt(5) == 1

                val start: LocalDateTime = toLocalDateTime(allDay, c.getLong(3))
                val end: LocalDateTime = toLocalDateTime(allDay, c.getLong(4))

                val recurring: Boolean = !c.getString(9).isNullOrBlank() || !c.getString(10).isNullOrBlank()

                val seriesStart: LocalDateTime? = if (recurring) toLocalDateTime(allDay, c.getLong(11)) else start

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
            .filter { it.end > windowStart && it.start < windowEnd }
            .sortedBy { it.start }
    }

    // Inserts a draft into android storage
    fun insertEvent(draft: EventDraft): Long{
        val newEventUri: Uri? = contentResolver.insert(CalendarContract.Events.CONTENT_URI, toContentValues(draft))
        return if (newEventUri != null)
            ContentUris.parseId(newEventUri) else -1
    }


    fun updateEvent(draft: EventDraft): Boolean{
        val conValue = toContentValues(draft)
        val id = draft.id
        if (id == null){
            return false
        }else{
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
            val updatedRows: Int = contentResolver.update(uri, conValue, null, null)
            return updatedRows != 0
        }
    }

    fun deleteEvent(eventId: Long){
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        contentResolver.delete(uri, null)
    }

    // Used if only one event of a recurring one is changed
    fun insertException(seriesId: Long, originalStart: LocalDateTime, allDay: Boolean, draft: EventDraft): Boolean{
        val values = toContentValues(draft)
        values.remove(CalendarContract.Events.DTEND)
        values.remove(CalendarContract.Events.CALENDAR_ID)
        values.remove(CalendarContract.Events.RRULE)
        values.put(CalendarContract.Events.DURATION,
            if (!draft.allDay){
                "P${draft.duration.seconds}S"
            }else{
                "P${draft.duration.toDays()}D"
            }
        )
        values.put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, toMillis(allDay, originalStart))
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_EXCEPTION_URI, seriesId)
        val result = contentResolver.insert(uri, values)
        return result != null
    }

    // Used if an event of a recurring one gets deleted
    fun cancelInstance(seriesId: Long, originalStart: LocalDateTime, allDay: Boolean): Boolean{
        val values = ContentValues()

        values.remove(CalendarContract.Events.DTEND)
        values.remove(CalendarContract.Events.CALENDAR_ID)
        values.remove(CalendarContract.Events.RRULE)

        values.put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, toMillis(allDay, originalStart))
        values.put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CANCELED)
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_EXCEPTION_URI, seriesId)
        val result = contentResolver.insert(uri, values)
        return result != null
    }

    // Used if all events of a recurring one are changed
    fun updateSeries(seriesId: Long, newSeriesStart: LocalDateTime, draft: EventDraft): Boolean{
        val values = toContentValues(draft)
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, seriesId)
        val proj = arrayOf(CalendarContract.Instances.RRULE)

        // RRULE needs to be set again, can not be removed as it would
        val c2 = contentResolver.query(uri, proj, null, null)

        c2?.use {
            if (!c2.moveToFirst()) return false
            values.put(
                CalendarContract.Events.RRULE,
                c2.getString(0))
        }

        //values.remove(CalendarContract.Events.RRULE)
        // Need to be removed in order for android to accept the change of a recurring event
        values.putNull(CalendarContract.Events.DTEND)
        values.remove(CalendarContract.Events.CALENDAR_ID)

        values.put(CalendarContract.Events.DURATION,
            if (!draft.allDay){
                "P${draft.duration.seconds}S"
            }else{
                "P${draft.duration.toDays()}D"
            }
        )
        values.put(CalendarContract.Events.DTSTART, toMillis(draft.allDay, newSeriesStart))

        val result = contentResolver.update(uri, values, null, null)
        return result == 1
    }

    // Helfer für insert und update
    private fun toContentValues(draft: EventDraft): ContentValues {

        val values = ContentValues()

        if (draft.recurrence == Recurrence.NONE){
            values.put(CalendarContract.Events.DTEND, toMillis(draft.allDay, draft.end))
            values.putNull(CalendarContract.Events.RRULE)
            values.putNull(CalendarContract.Events.DURATION)
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

        values.put(CalendarContract.Events.CALENDAR_ID, draft.calendarId)
        values.put(CalendarContract.Events.TITLE, draft.title)
        values.put(CalendarContract.Events.DTSTART, toMillis(draft.allDay, draft.start))
        // values.put(CalendarContract.Events.DTEND, toMillis(draft.allDay, draft.end))
        values.put(CalendarContract.Events.ALL_DAY, if (draft.allDay) 1 else 0)
        values.put(CalendarContract.Events.EVENT_LOCATION, draft.location)
        values.put(CalendarContract.Events.DESCRIPTION, draft.note)
        values.put(CalendarContract.Events.EVENT_TIMEZONE, if (draft.allDay) "UTC" else systemDefault().id)

        return values
    }
}