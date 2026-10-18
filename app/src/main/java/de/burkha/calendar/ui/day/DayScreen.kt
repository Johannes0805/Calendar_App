package de.burkha.calendar.ui.day

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.burkha.calendar.data.CalendarEvent
import de.burkha.calendar.ui.CalendarView
import de.burkha.calendar.ui.ViewSwitcher
import de.burkha.calendar.ui.components.CalendarHeader
import de.burkha.calendar.ui.components.DayHeading
import de.burkha.calendar.ui.components.DayTimeline
import de.burkha.calendar.ui.components.PlusIcon
import de.burkha.calendar.ui.components.TIME_FORMAT
import de.burkha.calendar.ui.components.formatDuration
import de.burkha.calendar.ui.theme.CalendarTheme
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * Tagesansicht: Wochenstreifen, "Jetzt"-Karte und Zeitstrahl mit freier Zeit.
 *
 * Reine UI: Der Screen lädt nichts selbst und entscheidet nichts.
 * Er bekommt alles als Parameter und meldet Klicks per Lambda nach oben.
 * Der Zeitstrahl selbst steckt in ui/components/DayTimeline.kt (teilen sich Tag, Woche, Monat).
 *
 * @param selectedDate  der angezeigte Tag
 * @param events        alle Termine dieses Tages (gern unsortiert, ganztägige dürfen dabei sein)
 * @param onSelectDate  ein Tag im Wochenstreifen wurde angetippt
 * @param onEventClick  ein Termin wurde angetippt (später: Editor öffnen)
 * @param onAddClick    der Plus-Button wurde angetippt
 * @param viewSwitcher  optionaler Slot oben rechts, z. B. für den ViewSwitcher. null = nichts anzeigen.
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // statusBarsPadding: Abstand zur Statusleiste (Uhrzeit, Netz), weil die App edge-to-edge zeichnet.
            CalendarHeader(
                date = selectedDate,
                onPrevious = { onSelectDate(selectedDate.minusDays(1)) },
                onNext = { onSelectDate(selectedDate.plusDays(1)) },
                previousLabel = "Vorheriger Tag",
                nextLabel = "Nächster Tag",
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
                    // Das Datum steht hier statt oben im Kopf, genau wie in Woche und Monat
                    item {
                        DayHeading(
                            date = selectedDate,
                            events = events,
                            now = now,
                            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
                        )
                    }
                    if (isToday) {
                        item {
                            FocusCard(
                                info = focusInfo(events, now),
                                now = now,
                                modifier = Modifier.padding(start = 8.dp, bottom = 20.dp),
                            )
                        }
                    }
                    item {
                        DayTimeline(
                            day = selectedDate,
                            events = events,
                            onEventClick = onEventClick,
                            now = now,
                        )
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
