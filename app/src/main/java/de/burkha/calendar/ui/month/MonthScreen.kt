package de.burkha.calendar.ui.month

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.burkha.calendar.data.CalendarEvent
import de.burkha.calendar.ui.CalendarView
import de.burkha.calendar.ui.ViewSwitcher
import de.burkha.calendar.ui.components.CalendarHeader
import de.burkha.calendar.ui.components.DaySheet
import de.burkha.calendar.ui.components.PlusIcon
import de.burkha.calendar.ui.components.StatTile
import de.burkha.calendar.ui.components.overlapsDay
import de.burkha.calendar.ui.components.plannedMinutes
import de.burkha.calendar.ui.theme.CalendarTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Monatsansicht "Auslastung": Jeder Tag ist danach eingefärbt, wie viel Zeit verplant ist.
 * Darunter Monatszahlen und der Zeitstrahl des gewählten Tags (derselbe wie in der Tagesansicht).
 *
 * Reine UI: Der angezeigte Monat ergibt sich aus selectedDate.
 *
 * @param selectedDate  der gewählte Tag (bestimmt auch, welcher Monat zu sehen ist)
 * @param events        alle Termine im sichtbaren Raster, also vom ersten Montag bis zum letzten Sonntag
 * @param onSelectDate  ein Tag wurde angetippt, oder die Pfeile wurden benutzt (dann: gleicher Tag im Nachbarmonat)
 * @param onEventClick  ein Termin im Zeitstrahl wurde angetippt
 * @param onAddClick    der Plus-Button wurde angetippt
 * @param viewSwitcher  optionaler Slot oben rechts, z. B. für den ViewSwitcher
 * @param now           aktuelle Zeit. Für die Vorschau festlegbar, in der App einfach weglassen.
 */
@Composable
fun MonthScreen(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    onSelectDate: (LocalDate) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewSwitcher: (@Composable () -> Unit)? = null,
    now: LocalDateTime = LocalDateTime.now(),
) {
    val today = now.toLocalDate()
    val month = YearMonth.from(selectedDate)
    val gridDays = remember(month) { gridDaysOf(month) }
    val minutesByDay = remember(events, gridDays) { gridDays.associateWith { plannedMinutes(it, events) } }
    val allDayDays = remember(events, gridDays) { gridDays.filter { day -> hasAllDayOn(day, events) }.toSet() }
    val stats = remember(events, month) { monthStats(events, month) }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Hintergrund in der Farbe des Blatts unten: so geht das Blatt nahtlos bis zum Bildschirmrand.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Oberer Teil in der normalen Hintergrundfarbe
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 16.dp),
            ) {
                CalendarHeader(
                    date = selectedDate,
                    onPrevious = { onSelectDate(selectedDate.minusMonths(1)) },
                    onNext = { onSelectDate(selectedDate.plusMonths(1)) },
                    previousLabel = "Vorheriger Monat",
                    nextLabel = "Nächster Monat",
                    action = viewSwitcher,
                    modifier = Modifier.statusBarsPadding(),
                )
                WeekdayRow()
                MonthGrid(
                    days = gridDays,
                    month = month,
                    selectedDate = selectedDate,
                    today = today,
                    minutesByDay = minutesByDay,
                    allDayDays = allDayDays,
                    onSelectDate = onSelectDate,
                )
                Legend()
                StatsRow(stats)
            }

            // Der Zeitstrahl des gewählten Tags (aus ui/components)
            DaySheet(
                date = selectedDate,
                events = events,
                onEventClick = onEventClick,
                now = now,
            )

            // Platz, damit der Plus-Button nichts verdeckt
            Spacer(modifier = Modifier.height(96.dp + navBarBottom))
        }

        FloatingActionButton(
            onClick = onAddClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            PlusIcon(color = LocalContentColor.current)
        }
    }
}

// ---------------------------------------------------------------------------
// Kopf
// ---------------------------------------------------------------------------

@Composable
private fun WeekdayRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (day in DayOfWeek.values()) {
            val weekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.GERMAN).take(2),
                style = MaterialTheme.typography.labelMedium,
                color = if (weekend) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Raster
// ---------------------------------------------------------------------------

@Composable
private fun MonthGrid(
    days: List<LocalDate>,
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    minutesByDay: Map<LocalDate, Long>,
    allDayDays: Set<LocalDate>,
    onSelectDate: (LocalDate) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        inMonth = YearMonth.from(day) == month,
                        minutes = minutesByDay[day] ?: 0L,
                        hasAllDay = day in allDayDays,
                        isSelected = day == selectedDate,
                        isToday = day == today,
                        onClick = { onSelectDate(day) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    inMonth: Boolean,
    minutes: Long,
    hasAllDay: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (background, textColor) = levelColors(minutes, inMonth)
    val ringShape = RoundedCornerShape(16.dp)
    val cellShape = RoundedCornerShape(13.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, ringShape) else Modifier,
            )
            .padding(3.dp)
            .clip(cellShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = textColor,
            )
            if (isToday) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(textColor),
                )
            }
        }
        if (hasAllDay && inMonth) {
            // Kleiner Punkt oben rechts = an diesem Tag gibt es einen ganztägigen Termin
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
            )
        }
    }
}

/**
 * Farbe je nach verplanter Zeit. Hier stellst du die Stufen ein.
 * Rückgabe: Hintergrund und passende Textfarbe.
 */
@Composable
private fun levelColors(minutes: Long, inMonth: Boolean): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    if (!inMonth) return Color.Transparent to cs.outline
    val background = when {
        minutes == 0L -> return cs.surfaceContainerHigh to cs.onSurfaceVariant
        minutes <= 120 -> cs.primaryContainer                          // bis 2 h
        minutes <= 240 -> lerp(cs.primaryContainer, cs.primary, 0.45f) // bis 4 h
        else -> cs.primary                                             // mehr als 4 h
    }
    // Helle Fläche -> dunkle Schrift, dunkle Fläche -> helle Schrift
    val text = if (background.luminance() > 0.4f) Color(0xFF111318) else Color.White
    return background to text
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("frei", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(0L, 60L, 180L, 300L).forEach { minutes ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(levelColors(minutes, inMonth = true).first),
            )
        }
        Text("voll", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatsRow(stats: MonthStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatTile(value = stats.eventCount.toString(), label = "Termine", modifier = Modifier.weight(1f))
        StatTile(value = "${Math.round(stats.minutes / 60.0)} h", label = "verplant", modifier = Modifier.weight(1f))
        StatTile(value = stats.freeDays.toString(), label = "freie Tage", modifier = Modifier.weight(1f))
    }
}

// ---------------------------------------------------------------------------
// Rechnen (keine Daten laden, nur aufbereiten)
// ---------------------------------------------------------------------------

private class MonthStats(val eventCount: Int, val minutes: Long, val freeDays: Int)

/** Alle Tage im Raster: vom Montag vor dem 1. bis zum Sonntag nach dem Monatsende. */
private fun gridDaysOf(month: YearMonth): List<LocalDate> {
    val start = month.atDay(1).with(DayOfWeek.MONDAY)
    val end = month.atEndOfMonth().with(DayOfWeek.SUNDAY)
    return generateSequence(start) { it.plusDays(1) }
        .takeWhile { !it.isAfter(end) }
        .toList()
}

private fun hasAllDayOn(day: LocalDate, events: List<CalendarEvent>): Boolean =
    events.any { it.allDay && overlapsDay(it, day) }

private fun monthStats(events: List<CalendarEvent>, month: YearMonth): MonthStats {
    val monthStart = month.atDay(1).atStartOfDay()
    val monthEnd = month.plusMonths(1).atDay(1).atStartOfDay()
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    return MonthStats(
        eventCount = events.count { it.end > monthStart && it.start < monthEnd },
        minutes = days.sumOf { plannedMinutes(it, events) },
        freeDays = days.count { day -> events.none { overlapsDay(it, day) } },
    )
}

// ---------------------------------------------------------------------------
// Vorschau in Android Studio (Split-Ansicht)
// ---------------------------------------------------------------------------

private val previewToday = LocalDate.of(2026, 10, 14)
private val previewNow = previewToday.atTime(11, 20)

private fun timed(id: Long, day: Int, title: String, from: LocalTime, to: LocalTime, color: Long, location: String? = null) =
    CalendarEvent(
        id = id,
        calendarId = 1,
        title = title,
        start = LocalDateTime.of(LocalDate.of(2026, 10, day), from),
        end = LocalDateTime.of(LocalDate.of(2026, 10, day), to),
        allDay = false,
        location = location,
        color = color.toInt(),
    )

private fun allDay(id: Long, day: Int, title: String, color: Long) =
    CalendarEvent(
        id = id,
        calendarId = 1,
        title = title,
        start = LocalDate.of(2026, 10, day).atStartOfDay(),
        end = LocalDate.of(2026, 10, day).plusDays(1).atStartOfDay(),
        allDay = true,
        location = null,
        color = color.toInt(),
    )

private const val BLUE = 0xFF415F91
private const val PINK = 0xFF8A4E8F
private const val GREEN = 0xFF386A3E
private const val ORANGE = 0xFF8B5000

private val previewEvents = listOf(
    allDay(1, 2, "Semesterbeginn", BLUE),
    timed(2, 5, "Zahnarzt", LocalTime.of(10, 30), LocalTime.of(11, 15), PINK),
    timed(3, 7, "Sport", LocalTime.of(18, 0), LocalTime.of(19, 30), GREEN),
    timed(4, 9, "Arbeit", LocalTime.of(9, 0), LocalTime.of(13, 0), ORANGE),
    timed(5, 9, "Kino", LocalTime.of(20, 0), LocalTime.of(22, 30), PINK),
    timed(6, 12, "Vorlesung", LocalTime.of(10, 15), LocalTime.of(11, 45), BLUE),
    timed(7, 12, "Sport", LocalTime.of(18, 0), LocalTime.of(19, 30), GREEN),
    timed(8, 13, "Übung", LocalTime.of(14, 15), LocalTime.of(15, 45), BLUE),
    timed(9, 14, "Vorlesung", LocalTime.of(8, 15), LocalTime.of(9, 45), BLUE, "H11"),
    timed(10, 14, "Mittagessen", LocalTime.of(12, 30), LocalTime.of(13, 30), PINK, "Mensa"),
    timed(11, 14, "Lerngruppe", LocalTime.of(16, 0), LocalTime.of(18, 0), BLUE, "Bibliothek"),
    timed(12, 15, "Arbeit", LocalTime.of(9, 0), LocalTime.of(13, 0), ORANGE),
    allDay(13, 16, "Geburtstag Mama", PINK),
    timed(14, 16, "Vorlesung", LocalTime.of(10, 15), LocalTime.of(11, 45), BLUE),
    timed(15, 22, "Arbeit", LocalTime.of(9, 0), LocalTime.of(13, 0), ORANGE),
    timed(16, 22, "Sport", LocalTime.of(18, 0), LocalTime.of(19, 30), GREEN),
    allDay(17, 24, "Wochenende weg", PINK),
    allDay(18, 25, "Wochenende weg", PINK),
    timed(19, 30, "Arbeit", LocalTime.of(9, 0), LocalTime.of(13, 0), ORANGE),
)

@Preview(name = "Hell", showBackground = true, widthDp = 390, heightDp = 1200)
@Composable
private fun MonthScreenPreview() {
    CalendarTheme(dynamicColor = false) {
        MonthScreen(
            selectedDate = previewToday,
            events = previewEvents,
            onSelectDate = {},
            onEventClick = {},
            onAddClick = {},
            viewSwitcher = { ViewSwitcher(selected = CalendarView.MONTH, onSelect = {}) },
            now = previewNow,
        )
    }
}

@Preview(
    name = "Dunkel",
    showBackground = true,
    widthDp = 390,
    heightDp = 1200,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MonthScreenDarkPreview() {
    CalendarTheme(darkTheme = true, dynamicColor = false) {
        MonthScreen(
            selectedDate = previewToday,
            events = previewEvents,
            onSelectDate = {},
            onEventClick = {},
            onAddClick = {},
            viewSwitcher = { ViewSwitcher(selected = CalendarView.MONTH, onSelect = {}) },
            now = previewNow,
        )
    }
}
