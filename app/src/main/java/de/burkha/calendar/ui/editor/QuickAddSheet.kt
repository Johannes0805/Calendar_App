package de.burkha.calendar.ui.editor

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.burkha.calendar.data.CalendarInfo
import de.burkha.calendar.data.EventDraft
import de.burkha.calendar.ui.components.TIME_FORMAT
import de.burkha.calendar.ui.theme.CalendarTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Editor C: schnell einen neuen Termin anlegen, als Sheet von unten.
 *
 * Reine UI: Der Entwurf (draft) lebt im ViewModel. Jede Änderung kommt als
 * neuer Entwurf über onDraftChange zurück. Das Sheet speichert nichts selbst.
 *
 * @param draft          der aktuelle Entwurf
 * @param calendars      Kalender, in die man schreiben darf
 * @param onDraftChange  der Entwurf hat sich geändert (Titel getippt, Tag gewählt, ...)
 * @param onSave         "Speichern" gedrückt
 * @param onMoreDetails  "Ort, Notiz, Wiederholung …" gedrückt -> hier soll der große Editor aufgehen
 * @param onDismiss      Sheet wurde weggewischt oder daneben getippt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    draft: EventDraft,
    calendars: List<CalendarInfo>,
    onDraftChange: (EventDraft) -> Unit,
    onSave: () -> Unit,
    onMoreDetails: () -> Unit,
    onDismiss: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // skipPartiallyExpanded: gleich ganz aufklappen, nicht erst halb
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        QuickAddContent(
            draft = draft,
            calendars = calendars,
            onDraftChange = onDraftChange,
            onSave = onSave,
            onMoreDetails = onMoreDetails,
            today = today,
        )
    }
}

/**
 * Der Inhalt des Sheets. Extra Funktion, weil die Vorschau in Android Studio
 * kein ModalBottomSheet anzeigen kann (das ist ein eigenes Fenster), den Inhalt aber schon.
 */
@Composable
fun QuickAddContent(
    draft: EventDraft,
    calendars: List<CalendarInfo>,
    onDraftChange: (EventDraft) -> Unit,
    onSave: () -> Unit,
    onMoreDetails: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    autoFocus: Boolean = true,
) {
    // Diese drei sind reiner UI-Zustand (ist ein Dialog offen?), deshalb dürfen sie hier leben
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val titleFocus = remember { FocusRequester() }

    // Beim Öffnen direkt ins Titelfeld springen, damit die Tastatur sofort da ist
    LaunchedEffect(Unit) {
        if (autoFocus) titleFocus.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(
                text = "Neuer Termin",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            TextField(
                value = draft.title,
                onValueChange = { onDraftChange(draft.copy(title = it)) },
                placeholder = { Text("Was steht an?") },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus),
            )
        }

        // --- Wann ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Wann")
            DayChips(
                selected = draft.start.toLocalDate(),
                today = today,
                onPick = { onDraftChange(draft.withDate(it)) },
                onOther = { showDatePicker = true },
            )
            if (draft.allDay) {
                Text(
                    text = "ganztägig",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = draft.start.format(TIME_FORMAT),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { showTimePicker = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    Text(
                        text = "bis ${draft.end.format(TIME_FORMAT)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DURATIONS.forEach { (minutes, label) ->
                        FilterChip(
                            selected = draft.duration.toMinutes() == minutes,
                            onClick = { onDraftChange(draft.withDuration(minutes)) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        }

        // --- Kalender ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Kalender")
            // horizontalScroll: bei mehr als ~4 Kalendern kann man seitlich wischen
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                calendars.forEach { calendar ->
                    CalendarTile(
                        calendar = calendar,
                        selected = calendar.id == draft.calendarId,
                        onClick = { onDraftChange(draft.copy(calendarId = calendar.id)) },
                    )
                }
            }
        }

        TextButton(onClick = onMoreDetails) {
            Text("Ort, Notiz, Wiederholung …", fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onSave,
            // Ohne Titel oder Kalender kann man nicht speichern
            enabled = draft.title.isNotBlank() && draft.calendarId != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("Speichern", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showDatePicker) {
        DatePickerDialogFor(
            date = draft.start.toLocalDate(),
            onPick = { onDraftChange(draft.withDate(it)) },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showTimePicker) {
        TimePickerDialogFor(
            time = draft.start.toLocalTime(),
            onPick = { onDraftChange(draft.withStartTime(it)) },
            onDismiss = { showTimePicker = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Heute · Morgen · Übermorgen · Anderer Tag */
@Composable
private fun DayChips(
    selected: LocalDate,
    today: LocalDate,
    onPick: (LocalDate) -> Unit,
    onOther: () -> Unit,
) {
    val quickDays = listOf(today, today.plusDays(1), today.plusDays(2))
    val labels = listOf("Heute", "Morgen", shortDay(today.plusDays(2)))
    val isOther = selected !in quickDays

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // zip macht aus zwei Listen eine Liste von Paaren: (heute, "Heute"), (morgen, "Morgen"), ...
        quickDays.zip(labels).forEach { (day, label) ->
            FilterChip(
                selected = day == selected,
                onClick = { onPick(day) },
                label = { Text(label) },
            )
        }
        FilterChip(
            selected = isOther,
            onClick = onOther,
            // Ist ein anderer Tag gewählt, steht er direkt auf dem Chip
            label = { Text(if (isOther) shortDay(selected, withMonth = true) else "Anderer …") },
        )
    }
}

@Composable
private fun CalendarTile(calendar: CalendarInfo, selected: Boolean, onClick: () -> Unit) {
    val color = Color(calendar.color)
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .width(76.dp)
            .height(56.dp)
            .clip(shape)
            .background(
                if (selected) lerp(MaterialTheme.colorScheme.surface, color, 0.25f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            .border(2.dp, if (selected) color else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        CalendarDot(color = color, size = 14.dp)
        Text(
            text = calendar.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val DURATIONS = listOf(30L to "30 min", 60L to "1 h", 90L to "1,5 h", 120L to "2 h")

/** "Fr 16." oder mit Monat "Mo 26.10." */
private fun shortDay(date: LocalDate, withMonth: Boolean = false): String {
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN).take(2)
    return if (withMonth) "$weekday ${date.dayOfMonth}.${date.monthValue}." else "$weekday ${date.dayOfMonth}."
}

// ---------------------------------------------------------------------------
// Vorschau
// ---------------------------------------------------------------------------

internal val previewCalendars = listOf(
    CalendarInfo(1, "Uni", 0xFF415F91.toInt(), "radicale", true),
    CalendarInfo(2, "Privat", 0xFF8A4E8F.toInt(), "radicale", true),
    CalendarInfo(3, "Sport", 0xFF386A3E.toInt(), "radicale", true),
    CalendarInfo(4, "Arbeit", 0xFF8B5000.toInt(), "radicale", true),
)

@Preview(name = "Hell", showBackground = true, widthDp = 390)
@Preview(name = "Dunkel", showBackground = true, widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuickAddPreview() {
    val today = LocalDate.of(2026, 10, 14)
    CalendarTheme(dynamicColor = false) {
        Surface {
            QuickAddContent(
                draft = EventDraft(
                    calendarId = 2,
                    title = "Einkaufen",
                    start = today.atTime(14, 0),
                    end = today.atTime(15, 0),
                ),
                calendars = previewCalendars,
                onDraftChange = {},
                onSave = {},
                onMoreDetails = {},
                today = today,
                autoFocus = false,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
