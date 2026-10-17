package de.burkha.calendar.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
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
            CalendarContract.Instances.RRULE
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
                val start: LocalDateTime
                val end: LocalDateTime

                if(allDay){
                    start = Instant.ofEpochMilli(c.getLong(3))
                        .atZone(ZoneOffset.UTC)
                        .toLocalDateTime()
                    end = Instant.ofEpochMilli(c.getLong(4))
                        .atZone(ZoneOffset.UTC)
                        .toLocalDateTime()
                }else{
                    start = Instant.ofEpochMilli(c.getLong(3))
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                    end = Instant.ofEpochMilli(c.getLong(4))
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                }

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
                        c.getInt(9) == 1
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
        if (id== null){
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

    // Helfer für insert und update
    private fun toContentValues(draft: EventDraft): ContentValues {

        val values: ContentValues = ContentValues()

        values.put(CalendarContract.Events.CALENDAR_ID, draft.calendarId)
        values.put(CalendarContract.Events.TITLE, draft.title)
        values.put(CalendarContract.Events.DTSTART, toMillis(draft.allDay, draft.start))
        values.put(CalendarContract.Events.DTEND, toMillis(draft.allDay, draft.end))
        values.put(CalendarContract.Events.ALL_DAY, if (draft.allDay) 1 else 0)
        values.put(CalendarContract.Events.EVENT_LOCATION, draft.location)
        values.put(CalendarContract.Events.DESCRIPTION, draft.note)
        values.put(CalendarContract.Events.EVENT_TIMEZONE, if (draft.allDay) "UTC" else systemDefault().id)

        return values
    }
}

