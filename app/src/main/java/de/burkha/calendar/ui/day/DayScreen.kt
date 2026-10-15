package de.burkha.calendar.ui.day

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.burkha.calendar.data.CalendarEvent
import de.burkha.calendar.ui.CalendarView
import de.burkha.calendar.ui.ViewSwitcher
import de.burkha.calendar.ui.theme.CalendarTheme
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Tagesansicht: Wochenstreifen, "Jetzt"-Karte und Zeitstrahl mit freier Zeit.
 *
 * Reine UI: Der Screen lädt nichts selbst und entscheidet nichts.
 * Er bekommt alles als Parameter und meldet Klicks per Lambda nach oben.
 *
 * @param selectedDate  der angezeigte Tag
 * @param events        alle Termine dieses Tages (gern unsortiert, ganztägige dürfen dabei sein)
 * @param onSelectDate  ein Tag im Wochenstreifen wurde angetippt
 * @param onEventClick  ein Termin wurde angetippt (später: Editor öffnen)
 * @param onAddClick    der Plus-Button wurde angetippt
 * @param viewSwitcher  optionaler Slot ganz oben, z. B. für den ViewSwitcher. null = nichts anzeigen.
 * @param now           aktuelle Uhrzeit. Für die Vorschau festlegbar, in der App einfach weglassen.
 */
@Composable
fun DayScreen(
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
    val isToday = selectedDate == today
    val timeline = remember(events, isToday, now) { buildTimeline(events, isToday, now) }
    val allDayEvents = events.filter { it.allDay }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // statusBarsPadding: Abstand zur Statusleiste (Uhrzeit, Netz), weil die App edge-to-edge zeichnet.
            DayHeader(
                date = selectedDate,
                isToday = isToday,
                action = viewSwitcher,
                modifier = Modifier.statusBarsPadding(),
            )
            WeekStrip(selected = selectedDate, today = today, onSelectDate = onSelectDate)

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                // Unten Platz für den Plus-Button und die Navigationsleiste von Android
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                LazyColumn(
                    contentPadding = PaddingValues(start = 12.dp, end = 20.dp, top = 20.dp, bottom = 104.dp + navBarBottom),
                ) {
                    if (isToday) {
                        item {
                            FocusCard(
                                info = focusInfo(events, now),
                                now = now,
                                modifier = Modifier.padding(start = 8.dp, bottom = 20.dp),
                            )
                        }
                    }
                    if (allDayEvents.isNotEmpty()) {
                        item { AllDaySection(allDayEvents, onEventClick) }
                    }
                    if (timeline.isEmpty() && allDayEvents.isEmpty()) {
                        item { EmptyDay() }
                    }
                    items(timeline) { entry ->
                        when (entry) {
                            is TimelineItem.Event -> EventRow(entry.event, entry.state, onEventClick)
                            is TimelineItem.Gap -> GapRow(entry.minutes)
                            is TimelineItem.Now -> NowRow(entry.time)
                            is TimelineItem.FreeFrom -> FreeFromRow(entry.time)
                        }
                    }
                }
            }
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
// Kopf und Wochenstreifen
// ---------------------------------------------------------------------------

@Composable
private fun DayHeader(
    date: LocalDate,
    isToday: Boolean,
    action: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isToday) "$weekday · Heute" else weekday,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = date.format(DATE_FORMAT),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (action != null) {
            // Rechts neben dem Datum, auf Höhe der Datumszeile
            Box(modifier = Modifier
                .width(180.dp)
                .padding(bottom = 2.dp)) {
                action()
            }
        }
    }
}

@Composable
private fun WeekStrip(selected: LocalDate, today: LocalDate, onSelectDate: (LocalDate) -> Unit) {
    val monday = selected.with(DayOfWeek.MONDAY)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (i in 0..6) {
            val day = monday.plusDays(i.toLong())
            val isSelected = day == selected
            val isToday = day == today
            val background = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            val numberColor = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(background)
                    .clickable { onSelectDate(day) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    // "Mo." -> "Mo"
                    text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN).take(2),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = numberColor,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// "Jetzt"-Karte (nur am heutigen Tag)
// ---------------------------------------------------------------------------

@Composable
private fun FocusCard(info: FocusInfo, now: LocalDateTime, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "JETZT · ${now.format(TIME_FORMAT)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (info.remaining != null) {
                    Text(
                        text = info.remaining,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = info.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (info.subtitle != null) {
                Text(
                    text = info.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (info.progress != null) {
                LinearProgressIndicator(
                    progress = { info.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    strokeCap = StrokeCap.Round,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            if (info.next != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(info.next.color)),
                    )
                    Text(
                        text = "Danach: ${info.next.title}" + (info.next.location?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Zeitstrahl
// ---------------------------------------------------------------------------

@Composable
private fun EventRow(event: CalendarEvent, state: EventState, onClick: (CalendarEvent) -> Unit) {
    val eventColor = Color(event.color)
    val container = lerp(MaterialTheme.colorScheme.surface, eventColor, 0.25f)
    val shape = RoundedCornerShape(18.dp)
    val details = "${event.start.format(TIME_FORMAT)} – ${event.end.format(TIME_FORMAT)}" +
        (event.location?.let { " · $it" } ?: "")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .alpha(if (state == EventState.PAST) 0.55f else 1f),
    ) {
        TimeLabel(text = event.start.format(TIME_FORMAT), modifier = Modifier.padding(top = 14.dp))
        Rail(dotColor = eventColor, dashed = false)
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
                .then(if (state == EventState.RUNNING) Modifier.border(2.dp, eventColor, shape) else Modifier)
                .clip(shape)
                .background(container)
                .clickable { onClick(event) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state == EventState.RUNNING) {
                Text(
                    text = "Läuft gerade",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun GapRow(minutes: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Spacer(modifier = Modifier.width(TIME_COLUMN_WIDTH))
        Rail(dotColor = null, dashed = true)
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .padding(top = 6.dp, bottom = 14.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "${formatDuration(minutes)} frei",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NowRow(time: LocalTime) {
    val red = MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = time.format(TIME_FORMAT),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = red,
            textAlign = TextAlign.End,
            modifier = Modifier
                .width(TIME_COLUMN_WIDTH)
                .padding(end = 10.dp),
        )
        Box(modifier = Modifier.width(16.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(red),
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(red),
        )
        Text(
            text = "Jetzt",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = red,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun FreeFromRow(time: LocalTime) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TimeLabel(text = time.format(TIME_FORMAT), muted = true)
        Box(modifier = Modifier.width(16.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Ab hier frei",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun AllDaySection(events: List<CalendarEvent>, onClick: (CalendarEvent) -> Unit) {
    Column(
        modifier = Modifier.padding(start = 8.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Ganztägig",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        events.forEach { event ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(lerp(MaterialTheme.colorScheme.surface, Color(event.color), 0.25f))
                    .clickable { onClick(event) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun EmptyDay() {
    Text(
        text = "Keine Termine – der Tag ist frei.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
    )
}

@Composable
private fun TimeLabel(text: String, modifier: Modifier = Modifier, muted: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (muted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = modifier
            .width(TIME_COLUMN_WIDTH)
            .padding(end = 10.dp),
    )
}

/** Die senkrechte Linie links mit optionalem Punkt. dashed = gestrichelt (für freie Zeit). */
@Composable
private fun Rail(dotColor: Color?, dashed: Boolean) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .width(16.dp)
            .fillMaxHeight()
            .drawBehind {
                val x = size.width / 2
                val effect = if (dashed) {
                    PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                } else {
                    null
                }
                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = effect,
                )
                if (dotColor != null) {
                    drawCircle(color = dotColor, radius = 6.dp.toPx(), center = Offset(x, 22.dp.toPx()))
                }
            },
    )
}

@Composable
private fun PlusIcon(color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val c = size.width / 2
        val l = 7.dp.toPx()
        val w = 2.dp.toPx()
        drawLine(color, Offset(c, c - l), Offset(c, c + l), w, StrokeCap.Round)
        drawLine(color, Offset(c - l, c), Offset(c + l, c), w, StrokeCap.Round)
    }
}

// ---------------------------------------------------------------------------
// Logik für die Anzeige (keine Daten laden, nur aufbereiten)
// ---------------------------------------------------------------------------

private val TIME_COLUMN_WIDTH = 54.dp
private const val MIN_GAP_MINUTES = 30L
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMAN)

private enum class EventState { PAST, RUNNING, UPCOMING }

/** Ein Eintrag im Zeitstrahl. "sealed" = es gibt genau diese vier Arten, sonst keine. */
private sealed interface TimelineItem {
    data class Event(val event: CalendarEvent, val state: EventState) : TimelineItem
    data class Gap(val minutes: Long) : TimelineItem
    data class Now(val time: LocalTime) : TimelineItem
    data class FreeFrom(val time: LocalTime) : TimelineItem
}

/** Macht aus der Terminliste die Reihenfolge, die der Zeitstrahl anzeigt. */
private fun buildTimeline(events: List<CalendarEvent>, isToday: Boolean, now: LocalDateTime): List<TimelineItem> {
    val timed = events.filter { !it.allDay }.sortedBy { it.start }
    if (timed.isEmpty()) return emptyList()

    val items = mutableListOf<TimelineItem>()
    var nowPlaced = !isToday          // an anderen Tagen gibt es keine "Jetzt"-Linie
    var latestEnd: LocalDateTime? = null

    for (event in timed) {
        val previousEnd = latestEnd
        if (previousEnd != null && event.start > previousEnd) {
            val gap = Duration.between(previousEnd, event.start).toMinutes()
            if (gap >= MIN_GAP_MINUTES) items.add(TimelineItem.Gap(gap))
        }
        if (!nowPlaced && now < event.start) {
            items.add(TimelineItem.Now(now.toLocalTime()))
            nowPlaced = true
        }
        val state = when {
            event.end <= now -> EventState.PAST
            event.start <= now -> EventState.RUNNING
            else -> EventState.UPCOMING
        }
        if (state == EventState.RUNNING) nowPlaced = true
        items.add(TimelineItem.Event(event, state))
        latestEnd = if (previousEnd == null || event.end > previousEnd) event.end else previousEnd
    }

    val end = latestEnd
    if (end != null && end.toLocalDate() == timed.first().start.toLocalDate()) {
        items.add(TimelineItem.FreeFrom(end.toLocalTime()))
    }
    if (!nowPlaced) items.add(TimelineItem.Now(now.toLocalTime()))
    return items
}

private class FocusInfo(
    val title: String,
    val subtitle: String?,
    val remaining: String?,
    val progress: Float?,
    val next: CalendarEvent?,
)

/** Was die "Jetzt"-Karte zeigt: laufender Termin, freie Zeit bis zum nächsten, oder "frei". */
private fun focusInfo(events: List<CalendarEvent>, now: LocalDateTime): FocusInfo {
    val timed = events.filter { !it.allDay }.sortedBy { it.start }

    val running = timed.firstOrNull { it.start <= now && now < it.end }
    if (running != null) {
        val total = Duration.between(running.start, running.end).toMinutes().coerceAtLeast(1)
        val done = Duration.between(running.start, now).toMinutes()
        return FocusInfo(
            title = running.title,
            subtitle = "bis ${running.end.format(TIME_FORMAT)}" + (running.location?.let { " · $it" } ?: ""),
            remaining = "noch ${formatDuration(Duration.between(now, running.end).toMinutes())}",
            progress = (done.toFloat() / total).coerceIn(0f, 1f),
            next = timed.firstOrNull { it.start >= running.end },
        )
    }

    val next = timed.firstOrNull { it.start > now }
    if (next != null) {
        val freeSince = timed.filter { it.end <= now }.maxOfOrNull { it.end }
        val progress = freeSince?.let {
            val total = Duration.between(it, next.start).toMinutes().coerceAtLeast(1)
            (Duration.between(it, now).toMinutes().toFloat() / total).coerceIn(0f, 1f)
        }
        return FocusInfo(
            title = "Frei bis ${next.start.format(TIME_FORMAT)}",
            subtitle = null,
            remaining = "noch ${formatDuration(Duration.between(now, next.start).toMinutes())}",
            progress = progress,
            next = next,
        )
    }

    return FocusInfo(
        title = if (timed.isEmpty()) "Heute ist nichts geplant" else "Rest des Tages frei",
        subtitle = null,
        remaining = null,
        progress = null,
        next = null,
    )
}

/** 165 -> "2 h 45 min", 60 -> "1 h", 40 -> "40 min" */
private fun formatDuration(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "$m min"
        m == 0L -> "$h h"
        else -> "$h h $m min"
    }
}

// ---------------------------------------------------------------------------
// Vorschau in Android Studio (Split-Ansicht)
// ---------------------------------------------------------------------------

private val previewDate = LocalDate.of(2026, 10, 14)
private val previewNow = LocalDateTime.of(2026, 10, 14, 11, 20)

private fun previewEvent(id: Long, title: String, from: LocalTime, to: LocalTime, location: String?, color: Long) =
    CalendarEvent(
        id = id,
        calendarId = 1,
        title = title,
        start = previewDate.atTime(from),
        end = previewDate.atTime(to),
        allDay = false,
        location = location,
        color = color.toInt(),
    )

private val previewEvents = listOf(
    previewEvent(1, "Vorlesung", LocalTime.of(8, 15), LocalTime.of(9, 45), "H11", 0xFF415F91),
    previewEvent(2, "Mittagessen", LocalTime.of(12, 30), LocalTime.of(13, 30), "Mensa", 0xFF8A4E8F),
    previewEvent(3, "Lerngruppe", LocalTime.of(16, 0), LocalTime.of(18, 0), "Bibliothek", 0xFF415F91),
)

@Preview(name = "Hell", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun DayScreenPreview() {
    CalendarTheme(dynamicColor = false) {
        DayScreen(
            selectedDate = previewDate,
            events = previewEvents,
            onSelectDate = {},
            onEventClick = {},
            onAddClick = {},
            viewSwitcher = { ViewSwitcher(selected = CalendarView.DAY, onSelect = {}) },
            now = previewNow,
        )
    }
}

@Preview(
    name = "Dunkel",
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DayScreenDarkPreview() {
    CalendarTheme(darkTheme = true, dynamicColor = false) {
        DayScreen(
            selectedDate = previewDate,
            events = previewEvents,
            onSelectDate = {},
            onEventClick = {},
            onAddClick = {},
            viewSwitcher = { ViewSwitcher(selected = CalendarView.DAY, onSelect = {}) },
            now = previewNow,
        )
    }
}
