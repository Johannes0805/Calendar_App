package de.burkha.calendar.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.burkha.calendar.data.CalendarEvent
import de.burkha.calendar.ui.theme.CalendarTheme
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * Der Zeitstrahl eines Tages: ganztägige Termine oben, darunter die Termine mit Punkt und Linie,
 * dazwischen "X frei", am heutigen Tag die rote "Jetzt"-Linie.
 *
 * Wird von Tag, Woche und Monat benutzt. Ist eine normale Column (nicht lazy),
 * damit man sie auch in eine scrollende Column stecken kann.
 *
 * @param day     welcher Tag gezeigt wird
 * @param events  Termine. Dürfen auch Termine anderer Tage enthalten, die werden hier rausgefiltert.
 * @param now     aktuelle Zeit. Bestimmt, was vergangen ist, was läuft und ob die "Jetzt"-Linie kommt.
 */
@Composable
fun DayTimeline(
    day: LocalDate,
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
    now: LocalDateTime = LocalDateTime.now(),
    highlightEventId: Long? = null,   // dieser Termin bekommt einen gestrichelten Rahmen (z. B. im Editor)
) {
    val dayEvents = remember(events, day) { events.filter { overlapsDay(it, day) } }
    val allDayEvents = dayEvents.filter { it.allDay }
    val timeline = remember(dayEvents, day, now) { buildTimeline(dayEvents, day, now) }

    Column(modifier = modifier) {
        if (allDayEvents.isNotEmpty()) {
            AllDaySection(allDayEvents, onEventClick)
        }
        if (timeline.isEmpty() && allDayEvents.isEmpty()) {
            EmptyDay()
        }
        timeline.forEach { entry ->
            when (entry) {
                is TimelineItem.Event -> EventRow(
                    event = entry.event,
                    state = entry.state,
                    highlighted = entry.event.id == highlightEventId,
                    onClick = onEventClick,
                )
                is TimelineItem.Gap -> GapRow(entry.minutes)
                is TimelineItem.Now -> NowRow(entry.time)
                is TimelineItem.FreeFrom -> FreeFromRow(entry.time)
            }
        }
    }
}

/**
 * Das "Blatt" unter Woche und Monat: abgerundete Fläche mit Überschrift ("Mi, 14. Oktober · 4,5 h verplant")
 * und dem Zeitstrahl darunter.
 *
 * Tipp: Gib dem Bildschirm dahinter dieselbe Farbe (surfaceContainerLow).
 * Dann geht das Blatt nahtlos bis nach unten, auch wenn wenig drin steht.
 *
 * @param title             eigene Überschrift. null = "Mi, 14. Oktober"
 * @param highlightEventId  wird an DayTimeline weitergereicht
 */
@Composable
fun DaySheet(
    date: LocalDate,
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
    now: LocalDateTime = LocalDateTime.now(),
    title: String? = null,
    highlightEventId: Long? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Zwei Hintergründe übereinander: erst ein Rechteck in der normalen Hintergrundfarbe
            // (füllt die Ecken hinter der Rundung), dann die abgerundete Fläche darüber.
            .background(MaterialTheme.colorScheme.surface)
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            )
            .padding(start = 12.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
    ) {
        DayHeading(
            date = date,
            events = events,
            now = now,
            title = title,
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
        )
        DayTimeline(
            day = date,
            events = events,
            onEventClick = onEventClick,
            now = now,
            highlightEventId = highlightEventId,
        )
    }
}

/**
 * Überschrift über einem Tag: links "Mi, 14. Oktober · Heute", rechts "4,5 h verplant".
 * Wird von DaySheet (Woche, Monat) und direkt von der Tagesansicht benutzt.
 *
 * @param title  eigene Überschrift statt des Datums (z. B. im Editor)
 */
@Composable
fun DayHeading(
    date: LocalDate,
    events: List<CalendarEvent>,
    modifier: Modifier = Modifier,
    now: LocalDateTime = LocalDateTime.now(),
    title: String? = null,
) {
    val minutes = remember(events, date) { plannedMinutes(date, events) }
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN).take(2)
    val isToday = date == now.toLocalDate()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title ?: ("$weekday, ${date.format(DAY_MONTH_FORMAT)}" + if (isToday) " · Heute" else ""),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = if (minutes > 0) "${formatHours(minutes)} verplant" else "frei",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Die einzelnen Zeilen
// ---------------------------------------------------------------------------

@Composable
private fun EventRow(
    event: CalendarEvent,
    state: EventState,
    highlighted: Boolean,
    onClick: (CalendarEvent) -> Unit,
) {
    val eventColor = Color(event.color)
    val container = lerp(MaterialTheme.colorScheme.surface, eventColor, 0.25f)
    val shape = RoundedCornerShape(18.dp)
    val details = "${event.start.format(TIME_FORMAT)} – ${event.end.format(TIME_FORMAT)}" +
        (event.location?.let { " · $it" } ?: "")
    val frame = when {
        highlighted -> Modifier.dashedBorder(eventColor, 18.dp)
        state == EventState.RUNNING -> Modifier.border(2.dp, eventColor, shape)
        else -> Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .alpha(if (state == EventState.PAST && !highlighted) 0.55f else 1f),
    ) {
        TimeLabel(text = event.start.format(TIME_FORMAT), modifier = Modifier.padding(top = 14.dp))
        Rail(dotColor = eventColor, dashed = false)
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
                .then(frame)
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
    val red = nowColor()
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
            PulsingDot(color = red, dotSize = 12.dp)
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

/**
 * Der "Jetzt"-Punkt mit einem ganz leichten Puls: Ein blasser Ring wächst langsam aus dem Punkt
 * heraus und verblasst dabei, dann eine kurze Pause, dann von vorn.
 *
 * Zum Feintunen: PULSE_* unten. Größer/kräftiger = auffälliger.
 */
@Composable
private fun PulsingDot(color: Color, dotSize: Dp) {
    // Eine Animation, die nie aufhört. progress läuft immer wieder von 0 nach 1.
    val transition = rememberInfiniteTransition(label = "jetzt")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // erst schnell, dann sanft auslaufend. delayMillis = Pause vor jedem neuen Puls
            animation = tween(durationMillis = PULSE_DURATION_MS, delayMillis = PULSE_PAUSE_MS, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "puls",
    )

    Box(
        modifier = Modifier
            .size(dotSize)
            // Der Ring wird HINTER den Punkt gemalt. drawBehind steht vor clip,
            // deshalb darf der Ring über den Punkt hinausragen.
            .drawBehind {
                val radius = size.minDimension / 2
                drawCircle(
                    color = color.copy(alpha = PULSE_START_ALPHA * (1f - progress)),
                    radius = radius * (1f + (PULSE_MAX_SCALE - 1f) * progress),
                )
            }
            .clip(CircleShape)
            .background(color),
    )
}

private const val PULSE_DURATION_MS = 1800   // wie lange ein Puls dauert
private const val PULSE_PAUSE_MS = 900       // Ruhe zwischen zwei Pulsen
private const val PULSE_START_ALPHA = 0.28f  // wie kräftig der Ring am Anfang ist (0..1)
private const val PULSE_MAX_SCALE = 2.3f     // wie groß der Ring am Ende wird (x Punktgröße)

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

/**
 * Gestrichelter Rahmen. Compose hat keinen fertigen, also malen wir ihn selbst:
 * drawWithContent = erst den normalen Inhalt zeichnen (drawContent), dann den Rahmen obendrauf.
 */
private fun Modifier.dashedBorder(color: Color, radius: Dp): Modifier = drawWithContent {
    drawContent()
    val stroke = 2.dp.toPx()
    drawRoundRect(
        color = color,
        // um die halbe Strichbreite nach innen, sonst wird die Hälfte der Linie abgeschnitten
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        ),
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

// ---------------------------------------------------------------------------
// Logik für die Anzeige (keine Daten laden, nur aufbereiten)
// ---------------------------------------------------------------------------

private val TIME_COLUMN_WIDTH = 54.dp
private const val MIN_GAP_MINUTES = 30L   // kürzere Lücken zeigen kein "X frei"

private enum class EventState { PAST, RUNNING, UPCOMING }

/** Ein Eintrag im Zeitstrahl. "sealed" = es gibt genau diese vier Arten, sonst keine. */
private sealed interface TimelineItem {
    data class Event(val event: CalendarEvent, val state: EventState) : TimelineItem
    data class Gap(val minutes: Long) : TimelineItem
    data class Now(val time: LocalTime) : TimelineItem
    data class FreeFrom(val time: LocalTime) : TimelineItem
}

/** Macht aus den Terminen eines Tages die Reihenfolge, die der Zeitstrahl anzeigt. */
private fun buildTimeline(events: List<CalendarEvent>, day: LocalDate, now: LocalDateTime): List<TimelineItem> {
    val timed = events.filter { !it.allDay }.sortedBy { it.start }
    if (timed.isEmpty()) return emptyList()

    val items = mutableListOf<TimelineItem>()
    var nowPlaced = day != now.toLocalDate()   // an anderen Tagen gibt es keine "Jetzt"-Linie
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

    // "Ab hier frei" nur, wenn der letzte Termin noch am selben Tag endet
    val end = latestEnd
    if (end != null && end.toLocalDate() == day) {
        items.add(TimelineItem.FreeFrom(end.toLocalTime()))
    }
    if (!nowPlaced) items.add(TimelineItem.Now(now.toLocalTime()))
    return items
}

// ---------------------------------------------------------------------------
// Vorschau
// ---------------------------------------------------------------------------

private val previewDay = LocalDate.of(2026, 10, 14)

private val previewEvents = listOf(
    CalendarEvent(1, 1, "Vorlesung", previewDay.atTime(8, 15), previewDay.atTime(9, 45), false, "H11", 0xFF415F91.toInt()),
    CalendarEvent(2, 1, "Mittagessen", previewDay.atTime(12, 30), previewDay.atTime(13, 30), false, "Mensa", 0xFF8A4E8F.toInt()),
    CalendarEvent(3, 1, "Lerngruppe", previewDay.atTime(16, 0), previewDay.atTime(18, 0), false, "Bibliothek", 0xFF415F91.toInt()),
)

@Preview(name = "Hell", showBackground = true, widthDp = 390)
@Preview(name = "Dunkel", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DaySheetPreview() {
    CalendarTheme(dynamicColor = false) {
        DaySheet(
            date = previewDay,
            events = previewEvents,
            onEventClick = {},
            now = previewDay.atTime(11, 20),
        )
    }
}
