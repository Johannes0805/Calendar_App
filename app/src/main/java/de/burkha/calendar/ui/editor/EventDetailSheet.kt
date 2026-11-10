package de.burkha.calendar.ui.editor

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.burkha.calendar.data.CalendarInfo
import de.burkha.calendar.data.EventDraft
import de.burkha.calendar.ui.components.TIME_FORMAT
import de.burkha.calendar.ui.components.formatDuration
import de.burkha.calendar.ui.theme.CalendarTheme
import java.net.URLEncoder
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Detailansicht eines Termins: schiebt sich von unten über die Ansicht.
 * Erst ansehen, mit "Bearbeiten" geht es in den Editor.
 *
 * Reine UI wie der Editor: zeigt den Entwurf an und meldet Klicks nach oben.
 *
 * @param draft        der angetippte Termin (derselbe Entwurf, den der Editor bekommt)
 * @param calendars    ALLE Kalender, auch schreibgeschützte. Daraus kommen Name und Farbe.
 *                     Ist der Kalender des Termins nicht beschreibbar (canWrite = false),
 *                     gibt es keine Knöpfe, nur einen Hinweis.
 * @param onEdit       "Bearbeiten" -> in den Editor wechseln
 * @param onDelete     Löschen bestätigt. Bei Serien mit der Antwort auf "Nur diesen / Alle",
 *                     bei normalen Terminen immer EditScope.ALL (wie im Editor).
 * @param onDismiss    Sheet weggewischt, daneben getippt oder Zurück-Geste
 * @param onDuplicate  "Duplizieren". null = Knopf wird nicht angezeigt.
 * @param isRecurring  Termin gehört zu einer Serie (zeigt Hinweis, fragt beim Löschen nach)
 * @param isException  Termin wurde einzeln geändert (nur für den Hinweistext)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailSheet(
    draft: EventDraft,
    calendars: List<CalendarInfo>,
    onEdit: () -> Unit,
    onDelete: (EditScope) -> Unit,
    onDismiss: () -> Unit,
    onDuplicate: (() -> Unit)? = null,
    isRecurring: Boolean = false,
    isException: Boolean = false,
    now: LocalDateTime = LocalDateTime.now(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        EventDetailContent(
            draft = draft,
            calendars = calendars,
            onEdit = onEdit,
            onDelete = onDelete,
            onDuplicate = onDuplicate,
            isRecurring = isRecurring,
            isException = isException,
            now = now,
        )
    }
}

/** Der Inhalt des Sheets. Extra, damit die Vorschau in Android Studio ihn ohne Sheet zeigen kann. */
@Composable
internal fun EventDetailContent(
    draft: EventDraft,
    calendars: List<CalendarInfo>,
    onEdit: () -> Unit,
    onDelete: (EditScope) -> Unit,
    modifier: Modifier = Modifier,
    onDuplicate: (() -> Unit)? = null,
    isRecurring: Boolean = false,
    isException: Boolean = false,
    now: LocalDateTime = LocalDateTime.now(),
) {
    val colors = MaterialTheme.colorScheme
    val calendar = calendars.firstOrNull { it.id == draft.calendarId }
    val calendarColor = calendar?.let { Color(it.color) } ?: colors.primary
    val canEdit = calendar?.canWrite == true

    // Reiner UI-Zustand: Rückfrage beim Löschen
    var confirmDelete by remember { mutableStateOf(false) }   // Einzeltermin: rote Zeile im Sheet
    var askScope by remember { mutableStateOf(false) }        // Serie: "Nur diesen / Ganze Serie"

    Column(
        modifier = modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Kopf: Farbstreifen, Kalender, Status, Titel, Zeit
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(calendarColor, RoundedCornerShape(3.dp)),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = calendar?.name ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = calendarColor,
                        modifier = Modifier.weight(1f),
                    )
                    StatusChip(statusText(draft, now), isPast = !draft.end.isAfter(now))
                }
                Text(
                    text = draft.title.ifBlank { "(Ohne Titel)" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onSurface,
                )
                Text(
                    text = whenText(draft),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
                if (isRecurring) {
                    Text(
                        text = if (isException) "Einzeln geändert · gehört zu einer Serie" else "Gehört zu einer Serie",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.outline,
                    )
                }
            }
        }

        // Ort und Notiz, nur wenn es etwas zu zeigen gibt
        val hasLocation = draft.location.isNotBlank()
        val hasNote = draft.note.isNotBlank()
        if (hasLocation || hasNote) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerLow, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                if (hasLocation) LocationRow(draft.location)
                if (hasLocation && hasNote) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.surfaceContainerHighest))
                }
                if (hasNote) {
                    Row(
                        modifier = Modifier.padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        LineIcon(colors.onSurfaceVariant, drawNotes)
                        Text(draft.note, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    }
                }
            }
        }

        // Unten: Knöpfe, Löschen-Rückfrage oder Hinweis bei schreibgeschützten Kalendern
        when {
            !canEdit -> Text(
                text = "Nur lesbar · ${calendar?.name ?: "Kalender"} kann in dieser App nicht geändert werden",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            confirmDelete -> DeleteConfirmRow(
                title = draft.title,
                onCancel = { confirmDelete = false },
                onConfirm = {
                    confirmDelete = false
                    onDelete(EditScope.ALL)
                },
            )
            else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionTile("Bearbeiten", drawPencil, colors.primary, colors.onPrimary, onEdit, Modifier.weight(1f))
                if (onDuplicate != null) {
                    ActionTile(
                        "Duplizieren", drawCopy, colors.secondaryContainer, colors.onSecondaryContainer,
                        onDuplicate, Modifier.weight(1f),
                    )
                }
                ActionTile(
                    "Löschen", drawTrash, colors.errorContainer, colors.onErrorContainer,
                    onClick = { if (isRecurring) askScope = true else confirmDelete = true },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (askScope) {
        ScopeDialog(
            title = "Wiederholenden Termin löschen",
            thisLabel = "Nur diesen Termin",
            thisHint = "Nur dieses eine Mal fällt er weg",
            allLabel = "Die ganze Serie",
            allHint = "Alle Termine der Serie werden gelöscht",
            destructive = true,
            onPick = { scope ->
                askScope = false
                onDelete(scope)
            },
            onDismiss = { askScope = false },
        )
    }
}

// ---------------------------------------------------------------------------
// Bausteine
// ---------------------------------------------------------------------------

@Composable
private fun StatusChip(text: String, isPast: Boolean) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = if (isPast) colors.onSurfaceVariant else colors.onTertiaryContainer,
        modifier = Modifier
            .background(
                if (isPast) colors.surfaceContainerHighest else colors.tertiaryContainer,
                RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun LocationRow(location: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier.heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LineIcon(MaterialTheme.colorScheme.onSurfaceVariant, drawPin)
        Text(
            text = location,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = {
            // Öffnet die Karten-App des Handys mit dem Ort als Suche.
            // try/catch: falls keine Karten-App installiert ist, passiert einfach nichts.
            val query = URLEncoder.encode(location, "UTF-8")
            try {
                uriHandler.openUri("geo:0,0?q=$query")
            } catch (e: Exception) {
                // keine App für geo:-Links
            }
        }) { Text("Route", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ActionTile(
    label: String,
    icon: IconDrawing,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = background,
        contentColor = foreground,
        modifier = modifier.height(64.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            LineIcon(foreground, icon, size = 20.dp)
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DeleteConfirmRow(title: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.errorContainer, RoundedCornerShape(18.dp))
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "„${title.ifBlank { "Termin" }}“ wirklich löschen?",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onCancel) { Text("Nein", color = colors.onErrorContainer, fontWeight = FontWeight.Bold) }
        Surface(onClick = onConfirm, shape = RoundedCornerShape(19.dp), color = colors.error) {
            Text(
                "Löschen",
                color = colors.onError,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Texte
// ---------------------------------------------------------------------------

private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN)

/** "Mi, 14. Okt." */
private fun shortDate(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN).removeSuffix(".") + ", " + date.format(SHORT_DATE)

/**
 * Wann der Termin ist, in einer Zeile:
 * "Mi, 14. Okt. · 16:00 – 18:00 · 2 h", "Mi, 14. Okt. · ganztägig",
 * oder über mehrere Tage "Mi, 14. Okt. 22:00 – Do, 15. Okt. 02:00".
 */
private fun whenText(draft: EventDraft): String {
    val startDay = draft.start.toLocalDate()
    if (draft.allDay) {
        // Ganztägig endet um Mitternacht des Folgetags, der letzte Tag ist also einer davor
        val endDay = if (draft.end.toLocalTime() == LocalTime.MIDNIGHT && draft.end.toLocalDate() > startDay) {
            draft.end.toLocalDate().minusDays(1)
        } else {
            draft.end.toLocalDate()
        }
        return if (endDay > startDay) {
            "${shortDate(startDay)} – ${shortDate(endDay)} · ganztägig"
        } else {
            "${shortDate(startDay)} · ganztägig"
        }
    }
    val minutes = Duration.between(draft.start, draft.end).toMinutes()
    return if (draft.end.toLocalDate() == startDay) {
        "${shortDate(startDay)} · ${draft.start.format(TIME_FORMAT)} – ${draft.end.format(TIME_FORMAT)} · ${formatDuration(minutes)}"
    } else {
        "${shortDate(startDay)} ${draft.start.format(TIME_FORMAT)} – ${shortDate(draft.end.toLocalDate())} ${draft.end.format(TIME_FORMAT)}"
    }
}

/** "in 4 h 40 min", "morgen", "in 3 Tagen", "läuft gerade", "vorbei" … */
private fun statusText(draft: EventDraft, now: LocalDateTime): String {
    val today = now.toLocalDate()
    val startDay = draft.start.toLocalDate()
    return when {
        !draft.end.isAfter(now) -> "vorbei"
        !draft.start.isAfter(now) -> if (draft.allDay) "heute" else "läuft gerade"
        startDay == today && !draft.allDay -> "in " + formatDuration(Duration.between(now, draft.start).toMinutes().coerceAtLeast(1))
        startDay == today -> "heute"
        startDay == today.plusDays(1) -> "morgen"
        else -> "in ${ChronoUnit.DAYS.between(today, startDay)} Tagen"
    }
}

// ---------------------------------------------------------------------------
// Symbole: einfache Linien-Icons, gezeichnet auf einem 24er-Raster wie SVG
// ---------------------------------------------------------------------------

/** Zeichnet ein Symbol auf einem 24×24-Raster, mit Linie (Stroke) und Farbe. */
private typealias IconDrawing = DrawScope.(Stroke, Color) -> Unit

@Composable
private fun LineIcon(color: Color, draw: IconDrawing, size: Dp = 22.dp) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = Stroke(width = 1.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // Alles auf 24 Einheiten zeichnen und dann auf die echte Größe skalieren
        scale(this.size.width / 24f, pivot = Offset.Zero) {
            draw(stroke, color)
        }
    }
}

private fun DrawScope.line(stroke: Stroke, color: Color, build: Path.() -> Unit) =
    drawPath(Path().apply(build), color, style = stroke)

private val drawPencil: IconDrawing = { stroke, color ->
    line(stroke, color) { moveTo(4f, 20f); lineTo(8f, 20f); lineTo(18f, 10f); lineTo(14f, 6f); lineTo(4f, 16f); close() }
    line(stroke, color) { moveTo(13.5f, 6.5f); lineTo(17.5f, 10.5f) }
}

private val drawCopy: IconDrawing = { stroke, color ->
    line(stroke, color) { addRoundRect(RoundRect(8f, 8f, 20f, 20f, 2.5f, 2.5f)) }
    line(stroke, color) { moveTo(16f, 8f); lineTo(16f, 4f); lineTo(4f, 4f); lineTo(4f, 16f); lineTo(8f, 16f) }
}

private val drawTrash: IconDrawing = { stroke, color ->
    line(stroke, color) { moveTo(5f, 7f); lineTo(19f, 7f) }
    line(stroke, color) { moveTo(10f, 7f); lineTo(10f, 4.5f); lineTo(14f, 4.5f); lineTo(14f, 7f) }
    line(stroke, color) { moveTo(7f, 7f); lineTo(8f, 20f); lineTo(16f, 20f); lineTo(17f, 7f) }
}

private val drawPin: IconDrawing = { stroke, color ->
    line(stroke, color) {
        moveTo(12f, 21f)
        cubicTo(12f, 21f, 5.5f, 15.3f, 5.5f, 10f)
        arcTo(Rect(5.5f, 3.5f, 18.5f, 16.5f), 180f, 180f, false)
        cubicTo(18.5f, 15.3f, 12f, 21f, 12f, 21f)
        close()
    }
    drawCircle(color, radius = 2.3f, center = Offset(12f, 10f), style = stroke)
}

private val drawNotes: IconDrawing = { stroke, color ->
    line(stroke, color) { moveTo(5f, 6f); lineTo(19f, 6f) }
    line(stroke, color) { moveTo(5f, 11f); lineTo(19f, 11f) }
    line(stroke, color) { moveTo(5f, 16f); lineTo(14f, 16f) }
}

// ---------------------------------------------------------------------------
// Vorschau
// ---------------------------------------------------------------------------

private val detailPreviewDay = LocalDate.of(2026, 10, 14)

private val detailPreviewCalendars = previewCalendars + CalendarInfo(5, "Feiertage", 0xFF74777F.toInt(), "local", false)

private val detailPreviewDraft = EventDraft(
    id = 11,
    calendarId = 1,
    title = "Lerngruppe",
    start = detailPreviewDay.atTime(16, 0),
    end = detailPreviewDay.atTime(18, 0),
    location = "Bibliothek",
    note = "Kapitel 3 und 4 vorbereiten, Laptop mitnehmen",
)

@Preview(name = "Detail", showBackground = true, widthDp = 390)
@Composable
private fun EventDetailPreview() {
    CalendarTheme(dynamicColor = false) {
        Surface {
            EventDetailContent(
                draft = detailPreviewDraft,
                calendars = detailPreviewCalendars,
                onEdit = {},
                onDelete = {},
                onDuplicate = {},
                now = detailPreviewDay.atTime(11, 20),
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@Preview(name = "Detail, Serie, dunkel", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EventDetailSeriesDarkPreview() {
    CalendarTheme(dynamicColor = false) {
        Surface {
            EventDetailContent(
                draft = detailPreviewDraft.copy(note = ""),
                calendars = detailPreviewCalendars,
                onEdit = {},
                onDelete = {},
                isRecurring = true,
                now = detailPreviewDay.atTime(16, 30),
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@Preview(name = "Detail, nur lesbar", showBackground = true, widthDp = 390)
@Composable
private fun EventDetailReadOnlyPreview() {
    CalendarTheme(dynamicColor = false) {
        Surface {
            EventDetailContent(
                draft = EventDraft(
                    id = 99,
                    calendarId = 5,
                    title = "Allerheiligen",
                    start = LocalDate.of(2026, 11, 1).atStartOfDay(),
                    end = LocalDate.of(2026, 11, 2).atStartOfDay(),
                    allDay = true,
                ),
                calendars = detailPreviewCalendars,
                onEdit = {},
                onDelete = {},
                now = detailPreviewDay.atTime(11, 20),
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}
