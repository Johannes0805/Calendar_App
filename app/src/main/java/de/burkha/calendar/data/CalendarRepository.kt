package de.burkha.calendar.data

import android.content.ContentResolver
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import java.time.LocalDate
import java.time.ZoneId
import kotlin.collections.mutableListOf
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoField

class CalendarRepository(private val contentResolver: ContentResolver) {
    fun getCalendars():List<CalendarInfo>{

        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.VISIBLE
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
                        (c.getInt(4) == 1)
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
            CalendarContract.Instances.DISPLAY_COLOR
        )

        val beginWindow: Long = from.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endWindow: Long = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

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
                        c.getInt(7)
                    )
                )
            }

        }
        return result
            .filter { it.end > windowStart && it.start < windowEnd }
            .sortedBy { it.start }
    }
}

