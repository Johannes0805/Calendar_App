package de.burkha.calendar.ui.week

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
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
import de.burkha.calendar.ui.components.formatHours
import de.burkha.calendar.ui.components.nowColor
import de.burkha.calendar.ui.components.overlapsDay
import de.burkha.calendar.ui.components.plannedMinutes
import de.burkha.calendar.ui.theme.CalendarTheme
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Wochenansicht "Auslastungs-Balken": pro Tag ein Balken von 7 bis 22 Uhr,
 * die farbigen Stücke darin sind die Termine. Darunter Wochenzahlen und
 * der Zeitstrahl des gewählten Tags.
 *
 * @param selectedDate  der gewählte Tag (bestimmt auch, welche Woche zu sehen ist)
 * @param events        alle Termine dieser Woche, Montag bis Sonntag
 * @param onSelectDate  ein Tag wurde angetippt, oder die Pfeile (dann: gleicher Wochentag in der Nachbarwoche)
 * @param onEventClick  ein Termin im Zeitstrahl wurde angetippt
 * @param onAddClick    der Plus-Button wurde angetippt
 * @param viewSwitcher  optionaler Slot oben rechts, z. B. für den ViewSwitcher
 * @param now           aktuelle Zeit. Für die Vorschau festlegbar, in der App einfach weglassen.
 */
@Composable
fun WeekScreen(
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
    val monday = selectedDate.with(DayOfWeek.MONDAY)
    val days = remember(monday) { (0L..6L).map { monday.plusDays(it) } }
    val minutesByDay = remember(events, days) { days.associateWith { plannedMinutes(it, events) } }
    val stats = remember(events, days) { weekStats(events, days, minutesByDay) }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Gleicher Aufbau wie im Monat: Hintergrund = Farbe des Blatts unten
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
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 16.dp),
            ) {
                CalendarHeader(
                    date = selectedDate,
                    onPrevious = { onSelectDate(selectedDate.minusWeeks(1)) },
                    onNext = { onSelectDate(selectedDate.plusWeeks(1)) },
                    previousLabel = "Vorherige Woche",
                    nextLabel = "Nächste Woche",
                    action = viewSwitcher,
                    modifier = Modifier.statusBarsPadding(),
                )
                // Kalenderwoche nach ISO (Woche beginnt Montag, so wie in Deutschland üblich)
                HourTicks(weekNumber = monday.get(WeekFields.ISO.weekOfWeekBasedYear()))
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    days.forEach { day ->
                        DayBarRow(
                            day = day,
                            events = events,
                            minutes = minutesByDay[day] ?: 0L,
                            isSelected = day == selectedDate,
                            isToday = day == today,
                            isPast = day < today,
                            now = now,
                            onClick = { onSelectDate(day) },
                        )
                    }
                }
                StatsRow(stats)
            }

            DaySheet(
                date = selectedDate,
                events = events,
                onEventClick = onEventClick,
                now = now,
            )

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

/** Links "KW 42", daneben die kleinen Uhrzeiten (8, 12, 16, 20) über den Balken. */
@Composable
private fun HourTicks(weekNumber: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
    ) {
        Text(
            text = "KW $weekNumber",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(DAY_LABEL_WIDTH),
        )
        // BoxWithConstraints verrät uns die echte Breite (maxWidth), damit wir die Zahlen
        // an der richtigen Stelle über dem Balken platzieren können.
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(16.dp),
        ) {
            for (hour in TICK_HOURS) {
                val fraction = (hour - BAR_START_HOUR).toFloat() / (BAR_END_HOUR - BAR_START_HOUR)
                Text(
                    text = hour.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(24.dp)
                        .offset(x = maxWidth * fraction - 12.dp), // -12 = halbe Breite, damit die Zahl mittig sitzt
                )
            }
        }
        Spacer(modifier = Modifier.width(HOURS_WIDTH))
    }
}

// ---------------------------------------------------------------------------
// Eine Zeile pro Tag
// ---------------------------------------------------------------------------

@Composable
private fun DayBarRow(
    day: LocalDate,
    events: List<CalendarEvent>,
    minutes: Long,
    isSelected: Boolean,
    isToday: Boolean,
    isPast: Boolean,
    now: LocalDateTime,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val numberColor = when {
        isToday -> cs.primary
        isPast -> cs.outline
        else -> cs.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier
                        .background(cs.surfaceContainerLowest)
                        .border(1.dp, cs.outlineVariant, shape)
                } else {
                    Modifier
                },
            )
            // selectable statt clickable: Bildschirmleser sagen dann auch "ausgewählt"
            .selectable(selected = isSelected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(DAY_LABEL_WIDTH)) {
            Text(
                text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN).take(2),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isToday) cs.primary else cs.outline,
            )
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = numberColor,
            )
        }
        DayBar(
            day = day,
            events = events,
            isToday = isToday,
            now = now,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (minutes > 0) formatHours(minutes) else "–",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(HOURS_WIDTH),
        )
    }
}

/**
 * Der Balken selbst. Gezeichnet mit drawBehind: Wir bekommen eine Zeichenfläche
 * (size = Breite und Höhe in Pixeln) und malen Rechtecke an die passenden Stellen.
 */
@Composable
private fun DayBar(
    day: LocalDate,
    events: List<CalendarEvent>,
    isToday: Boolean,
    now: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    val segments = remember(events, day) { barSegments(day, events) }
    val nowFraction = fractionOfBar(day, now)
    val nowLineColor = nowColor()

    Box(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .drawBehind {
                segments.forEach { segment ->
                    val past = segment.end <= now
                    drawRect(
                        color = segment.color.copy(alpha = if (past) 0.45f else 1f),
                        topLeft = Offset(x = size.width * segment.from, y = 0f),
                        size = Size(width = size.width * (segment.to - segment.from), height = size.height),
                    )
                }
                if (isToday) {
                    val x = size.width * nowFraction
                    drawLine(nowLineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
                }
            },
    )
}

@Composable
private fun StatsRow(stats: WeekStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatTile(value = stats.eventCount.toString(), label = "Termine", modifier = Modifier.weight(1f))
        StatTile(
            value = if (stats.minutes > 0) formatHours(stats.minutes) else "0 h",
            label = "verplant",
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = stats.busiestDay?.dayOfWeek?.getDisplayName(TextStyle.SHORT, Locale.GERMAN)?.take(2) ?: "–",
            label = "vollster Tag",
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------
// Rechnen (keine Daten laden, nur aufbereiten)
// ---------------------------------------------------------------------------

// Der Balken geht von 7 bis 22 Uhr. Termine außerhalb werden abgeschnitten (zählen aber bei den Stunden mit).
private const val BAR_START_HOUR = 7
private const val BAR_END_HOUR = 22
private val TICK_HOURS = listOf(8, 12, 16, 20)
private val DAY_LABEL_WIDTH = 48.dp
private val HOURS_WIDTH = 46.dp

/** Ein farbiges Stück im Balken. from/to sind Anteile von 0.0 (7 Uhr) bis 1.0 (22 Uhr). */
private class BarSegment(val from: Float, val to: Float, val color: Color, val end: LocalDateTime)

private class WeekStats(val eventCount: Int, val minutes: Long, val busiestDay: LocalDate?)

/** Wo liegt ein Zeitpunkt auf dem Balken dieses Tages? 0.0 = ganz links, 1.0 = ganz rechts. */
private fun fractionOfBar(day: LocalDate, time: LocalDateTime): Float {
    val minutesSinceMidnight = Duration.between(day.atStartOfDay(), time).toMinutes()
    val barStart = BAR_START_HOUR * 60
    val barLength = (BAR_END_HOUR - BAR_START_HOUR) * 60
    return ((minutesSinceMidnight - barStart).toFloat() / barLength).coerceIn(0f, 1f)
}

private fun barSegments(day: LocalDate, events: List<CalendarEvent>): List<BarSegment> =
    events
        .filter { !it.allDay && overlapsDay(it, day) }
        .map { event ->
            BarSegment(
                from = fractionOfBar(day, event.start),
                to = fractionOfBar(day, event.end),
                color = Color(event.color),
                end = event.end,
            )
        }
        .filter { it.to > it.from }   // ganz außerhalb von 7–22 Uhr -> nichts zu zeichnen

private fun weekStats(events: List<CalendarEvent>, days: List<LocalDate>, minutesByDay: Map<LocalDate, Long>): WeekStats {
    val busiest = days.maxByOrNull { minutesByDay[it] ?: 0L }
    return WeekStats(
        eventCount = events.count { event -> days.any { overlapsDay(event, it) } },
        minutes = minutesByDay.values.sum(),
        busiestDay = if (busiest != null && (minutesByDay[busiest] ?: 0L) > 0) busiest else null,
    )
}

// ---------------------------------------------------------------------------
// Vorschau in Android Studio (Split-Ansicht)
// ---------------------------------------------------------------------------

private val previewToday = LocalDate.of(2026, 10, 14)
private val previewNow = previewToday.atTime(11, 20)

private fun timed(id: Long, day: Int, title: String, from: String, to: String, color: Long, location: String? = null): CalendarEvent {
    val date = LocalDate.of(2026, 10, day)
    return CalendarEvent(
        id = id,
        calendarId = 1,
        title = title,
        start = LocalDateTime.of(date, java.time.LocalTime.parse(from)),
        end = LocalDateTime.of(date, java.time.LocalTime.parse(to)),
        allDay = false,
        location = location,
        color = color.toInt(),
    )
}

private const val BLUE = 0xFF415F91
private const val PINK = 0xFF8A4E8F
private const val GREEN = 0xFF386A3E
private const val ORANGE = 0xFF8B5000

private val previewEvents = listOf(
    timed(1, 12, "Vorlesung", "10:15", "11:45", BLUE, "H11"),
    timed(2, 12, "Sport", "18:00", "19:30", GREEN),
    timed(3, 13, "Übung", "14:15", "15:45", BLUE),
    timed(4, 14, "Vorlesung", "08:15", "09:45", BLUE, "H11"),
    timed(5, 14, "Mittagessen", "12:30", "13:30", PINK, "Mensa"),
    timed(6, 14, "Lerngruppe", "16:00", "18:00", BLUE, "Bibliothek"),
    timed(7, 15, "Arbeit", "09:00", "13:00", ORANGE),
    CalendarEvent(
        id = 8, calendarId = 1, title = "Geburtstag Mama",
        start = LocalDate.of(2026, 10, 16).atStartOfDay(),
        end = LocalDate.of(2026, 10, 17).atStartOfDay(),
        allDay = true, location = null, color = PINK.toInt(),
    ),
    timed(9, 16, "Vorlesung", "10:15", "11:45", BLUE, "H11"),
    timed(10, 17, "Sport", "10:00", "11:30", GREEN),
)

@Preview(name = "Hell", showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
private fun WeekScreenPreview() {
    CalendarTheme(dynamicColor = false) {
        WeekScreen(
            selectedDate = previewToday,
            events = previewEvents,
            onSelectDate = {},
            onEventClick = {},
            onAddClick = {},
            viewSwitcher = { ViewSwitcher(selected = CalendarView.WEEK, onSelect = {}) },
            now = previewNow,
        )
    }
}

@Preview(
    name = "Dunkel",
    showBackground = true,
    widthDp = 390,
    heightDp = 1100,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun WeekScreenDarkPreview() {
    CalendarTheme(darkTheme = true, dynamicColor = false) {
        WeekScreen(
            selectedDate = previewToday,
            events = previewEvents,
            onSelectDate = {},
            onEventClick = {},
            onAddClick = {},
            viewSwitcher = { ViewSwitcher(selected = CalendarView.WEEK, onSelect = {}) },
            now = previewNow,
        )
    }
}
