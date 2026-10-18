package de.burkha.calendar.ui.editor

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.burkha.calendar.data.CalendarEvent
import de.burkha.calendar.data.CalendarInfo
import de.burkha.calendar.data.EventDraft
import de.burkha.calendar.data.Recurrence
import de.burkha.calendar.ui.components.DaySheet
import de.burkha.calendar.ui.components.TIME_FORMAT
import de.burkha.calendar.ui.theme.CalendarTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.format.TextStyle as DateTextStyle

/**
 * Editor B: der Termin als Satz ("Lerngruppe am Mi, 14. Okt. von 16:00 bis 18:00 in Bibliothek"),
 * darunter der Zeitstrahl des Tages mit dem Termin gestrichelt drin.
 *
 * Reine UI wie QuickAddSheet: Entwurf rein, Änderungen per onDraftChange raus.
 *
 * @param dayEvents  Termine an dem Tag, für die Vorschau unten. Der bearbeitete Termin selbst darf
 *                   dabei sein, er wird durch den Entwurf ersetzt.
 * @param onSave       "Speichern" gedrückt. Bei Serien erst nach der Frage "Nur diesen / Alle",
 *                     die Antwort steckt im EditScope. Bei normalen Terminen immer ALL.
 * @param onDelete     wie onSave, nur fürs Löschen. null = kein Löschen-Knopf (z. B. bei neuen Terminen)
 * @param isRecurring  true = der Termin gehört zu einer Serie. Dann wird beim Speichern/Löschen
 *                     nachgefragt, und Kalender und Wiederholung lassen sich nicht ändern.
 * @param locked       true = alles nur zum Ansehen (z. B. für Kalender ohne Schreibrecht)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventEditorScreen(
    draft: EventDraft,
    calendars: List<CalendarInfo>,
    dayEvents: List<CalendarEvent>,
    onDraftChange: (EventDraft) -> Unit,
    onSave: (EditScope) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: ((EditScope) -> Unit)? = null,
    isRecurring: Boolean = false,
    locked: Boolean = false,
    now: LocalDateTime = LocalDateTime.now(),
) {
    // Reiner UI-Zustand: welcher Dialog ist offen, ist das Notizfeld aufgeklappt
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var showCalendarMenu by remember { mutableStateOf(false) }
    var showRecurrenceMenu by remember { mutableStateOf(false) }
    var showRecurrenceEndMenu by remember { mutableStateOf(false) }
    var showRecurrenceEndPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSaveScope by remember { mutableStateOf(false) }
    var noteOpen by remember { mutableStateOf(draft.note.isNotEmpty()) }

    val enabled = !locked
    val calendar = calendars.firstOrNull { it.id == draft.calendarId }
    val calendarColor = calendar?.let { Color(it.color) } ?: MaterialTheme.colorScheme.primary
    val day = draft.start.toLocalDate()
    // Andere Termine des Tages + der Entwurf (statt der alten Version des Termins)
    val previewEvents = remember(dayEvents, draft, calendarColor) {
        dayEvents.filter { it.id != draft.id } + draft.asPreviewEvent(calendarColor.toArgb())
    }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        // Kopfzeile bleibt oben stehen, der Rest scrollt
        EditorTopBar(
            canSave = enabled && draft.title.isNotBlank() && draft.calendarId != null,
            showSave = enabled,
            // Bei Serien erst fragen, sonst direkt speichern
            onSave = { if (isRecurring) showSaveScope = true else onSave(EditScope.ALL) },
            onClose = onClose,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 12.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (locked) LockedHint(modifier = Modifier.padding(start = 12.dp))

                TextField(
                    value = draft.title,
                    onValueChange = { onDraftChange(draft.copy(title = it)) },
                    enabled = enabled,
                    placeholder = { Text("Was steht an?") },
                    textStyle = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        // Unterstrich nur beim Tippen zeigen
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                // --- Der Satz. FlowRow = wie Row, bricht aber in die nächste Zeile um, wenn es zu breit wird ---
                Column(
                    modifier = Modifier.padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Word("am", Modifier.align(Alignment.CenterVertically))
                        SentenceChip(text = dateLabel(day), enabled = enabled, onClick = { showDatePicker = true })
                        if (draft.allDay) Word("ganztägig", Modifier.align(Alignment.CenterVertically))
                    }
                    if (!draft.allDay) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Word("von", Modifier.align(Alignment.CenterVertically))
                            SentenceChip(
                                text = draft.start.format(TIME_FORMAT),
                                enabled = enabled,
                                onClick = { showStartPicker = true },
                            )
                            Word("bis", Modifier.align(Alignment.CenterVertically))
                            SentenceChip(
                                text = endLabel(draft),
                                enabled = enabled,
                                onClick = { showEndPicker = true },
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Word("in", Modifier.align(Alignment.CenterVertically))
                        LocationField(
                            value = draft.location,
                            enabled = enabled,
                            onValueChange = { onDraftChange(draft.copy(location = it)) },
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Word("im Kalender", Modifier.align(Alignment.CenterVertically))
                        // Box, damit das Aufklappmenü direkt am Chip hängt
                        Box {
                            SentenceChip(
                                text = calendar?.name ?: "wählen",
                                // Einen einzelnen Serientermin kann man nicht in einen anderen Kalender legen
                                enabled = enabled && !isRecurring,
                                onClick = { showCalendarMenu = true },
                                dotColor = calendar?.let { Color(it.color) },
                                containerColor = lerp(MaterialTheme.colorScheme.surface, calendarColor, 0.25f),
                            )
                            CalendarMenu(
                                expanded = showCalendarMenu,
                                calendars = calendars,
                                onPick = { onDraftChange(draft.copy(calendarId = it.id)) },
                                onDismiss = { showCalendarMenu = false },
                            )
                        }
                    }
                    if (isRecurring) {
                        // Die Regel einer bestehenden Serie ändern wir (noch) nicht, nur Hinweis
                        Word("gehört zu einer Serie")
                    } else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // "wiederholt sich jeden Mittwoch"
                        Word("wiederholt sich", Modifier.align(Alignment.CenterVertically))
                        Box {
                            SentenceChip(
                                text = recurrenceLabel(draft.recurrence, day),
                                enabled = enabled,
                                onClick = { showRecurrenceMenu = true },
                            )
                            DropdownMenu(
                                expanded = showRecurrenceMenu,
                                onDismissRequest = { showRecurrenceMenu = false },
                            ) {
                                // entries = alle Werte des enums, in der Reihenfolge, in der sie dort stehen
                                Recurrence.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(recurrenceLabel(option, day)) },
                                        onClick = {
                                            // Bei "nie" und "jährlich" gibt es kein Ende-Feld, also Ende zurücksetzen,
                                            // damit nicht unsichtbar ein altes Enddatum mitgespeichert wird
                                            val keepEnd = endPresets(option).isNotEmpty()
                                            onDraftChange(
                                                draft.copy(
                                                    recurrence = option,
                                                    recurrenceEnd = if (keepEnd) draft.recurrenceEnd else null,
                                                ),
                                            )
                                            showRecurrenceMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    // "endet am 21. April 2027" (nur bei täglich, wöchentlich, monatlich)
                    val presets = endPresets(draft.recurrence)
                    if (!isRecurring && presets.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Word("endet", Modifier.align(Alignment.CenterVertically))
                            Box {
                                SentenceChip(
                                    text = draft.recurrenceEnd?.let { "am " + it.format(LONG_DATE_FORMAT) } ?: "nie",
                                    enabled = enabled,
                                    onClick = { showRecurrenceEndMenu = true },
                                )
                                DropdownMenu(
                                    expanded = showRecurrenceEndMenu,
                                    onDismissRequest = { showRecurrenceEndMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("nie") },
                                        onClick = {
                                            onDraftChange(draft.copy(recurrenceEnd = null))
                                            showRecurrenceEndMenu = false
                                        },
                                    )
                                    // Schnellauswahl: "6 Monate · bis 21.04.2027"
                                    presets.forEach { (label, period) ->
                                        val end = day.plus(period)
                                        DropdownMenuItem(
                                            text = { Text("$label · bis ${end.format(SHORT_DATE_FORMAT)}") },
                                            onClick = {
                                                onDraftChange(draft.copy(recurrenceEnd = end))
                                                showRecurrenceEndMenu = false
                                            },
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Datum wählen …") },
                                        onClick = {
                                            showRecurrenceEndMenu = false
                                            showRecurrenceEndPicker = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // --- Schalter unter dem Satz ---
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = draft.allDay,
                        onClick = { onDraftChange(draft.withAllDay(!draft.allDay)) },
                        enabled = enabled,
                        label = { Text("Ganztägig") },
                    )
                    if (!noteOpen) {
                        FilterChip(
                            selected = false,
                            onClick = { noteOpen = true },
                            enabled = enabled,
                            label = { Text("+ Notiz") },
                        )
                    }
                }
                if (noteOpen) {
                    OutlinedTextField(
                        value = draft.note,
                        onValueChange = { onDraftChange(draft.copy(note = it)) },
                        enabled = enabled,
                        label = { Text("Notiz") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                    )
                }
            }

            // --- Vorschau: der Tag mit dem Termin drin ---
            DaySheet(
                date = day,
                events = previewEvents,
                onEventClick = {},
                now = now,
                title = "So sieht dein ${day.dayOfWeek.getDisplayName(DateTextStyle.FULL, Locale.GERMAN)} aus",
                highlightEventId = draft.id ?: EventDraft.PREVIEW_ID,
            )

            if (onDelete != null && enabled) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.padding(start = 12.dp),
                ) {
                    Text("Termin löschen", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp + navBarBottom))
        }
    }

    // --- Dialoge. Stehen außerhalb vom Layout, weil sie sowieso als eigenes Fenster aufgehen ---
    if (showDatePicker) {
        DatePickerDialogFor(
            date = day,
            onPick = { onDraftChange(draft.withDate(it)) },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showStartPicker) {
        TimePickerDialogFor(
            time = draft.start.toLocalTime(),
            onPick = { onDraftChange(draft.withStartTime(it)) },
            onDismiss = { showStartPicker = false },
        )
    }
    if (showEndPicker) {
        TimePickerDialogFor(
            time = draft.end.toLocalTime(),
            onPick = { onDraftChange(draft.withEndTime(it)) },
            onDismiss = { showEndPicker = false },
        )
    }
    if (showRecurrenceEndPicker) {
        DatePickerDialogFor(
            date = draft.recurrenceEnd ?: day.plusMonths(1),
            // Ein Ende vor dem ersten Termin ergibt keinen Sinn, dann nehmen wir den ersten Termin selbst
            onPick = { onDraftChange(draft.copy(recurrenceEnd = maxOf(it, day))) },
            onDismiss = { showRecurrenceEndPicker = false },
        )
    }
    if (showSaveScope) {
        ScopeDialog(
            title = "Wiederholenden Termin ändern",
            thisLabel = "Nur diesen Termin",
            allLabel = "Alle Termine der Serie",
            destructive = false,
            onPick = { scope ->
                showSaveScope = false
                onSave(scope)
            },
            onDismiss = { showSaveScope = false },
        )
    }
    if (showDeleteConfirm && onDelete != null && isRecurring) {
        ScopeDialog(
            title = "Wiederholenden Termin löschen",
            thisLabel = "Nur diesen Termin",
            allLabel = "Alle Termine der Serie",
            destructive = true,
            onPick = { scope ->
                showDeleteConfirm = false
                onDelete(scope)
            },
            onDismiss = { showDeleteConfirm = false },
        )
    } else if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Termin löschen?") },
            text = { Text("„${draft.title}“ wird auch auf deinem Server gelöscht.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(EditScope.ALL)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen") } },
        )
    }
}

// ---------------------------------------------------------------------------
// Bausteine
// ---------------------------------------------------------------------------

/**
 * Die Frage "Nur diesen oder alle?". Zwei große Knöpfe untereinander statt der üblichen
 * kleinen Dialog-Knöpfe, damit man nicht aus Versehen den falschen trifft.
 *
 * @param destructive  true = Löschen. Dann ist "alle" rot, weil man damit die ganze Serie verliert.
 */
@Composable
private fun ScopeDialog(
    title: String,
    thisLabel: String,
    allLabel: String,
    destructive: Boolean,
    onPick: (EditScope) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { onPick(EditScope.THIS) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(thisLabel) }
                OutlinedButton(
                    onClick = { onPick(EditScope.ALL) },
                    colors = if (destructive) {
                        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(allLabel) }
            }
        },
        // AlertDialog will einen confirmButton. Unsere "Bestätigung" sind die zwei Knöpfe oben,
        // hier steht deshalb nur "Abbrechen".
        confirmButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun EditorTopBar(canSave: Boolean, showSave: Boolean, onSave: () -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            CloseIcon(color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.weight(1f))
        if (showSave) {
            Button(onClick = onSave, enabled = canSave) {
                Text("Speichern")
            }
        }
    }
}

/** Ein normales Wort im Satz ("am", "von", ...). */
@Composable
private fun Word(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = sentenceStyle(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Ein antippbarer Teil des Satzes, z. B. "16:00". */
@Composable
private fun SentenceChip(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    dotColor: Color? = null,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (dotColor != null) CalendarDot(color = dotColor)
        Text(
            text = text,
            style = sentenceStyle().copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/**
 * Das Ortsfeld sieht aus wie ein Chip, man tippt aber direkt hinein.
 * BasicTextField = Textfeld ganz ohne Material-Aussehen. Wie es aussieht, bestimmt decorationBox:
 * Wir malen den Chip-Hintergrund und den grauen Platzhalter, innerTextField() ist der eigentliche Text.
 */
@Composable
private fun LocationField(value: String, enabled: Boolean, onValueChange: (String) -> Unit) {
    val textColor = MaterialTheme.colorScheme.onSecondaryContainer
    val style = sentenceStyle().copy(fontWeight = FontWeight.Bold, color = textColor)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                if (value.isEmpty()) {
                    Text("Ort hinzufügen", style = style.copy(color = textColor.copy(alpha = 0.55f)))
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun LockedHint(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Diesen Termin kannst du hier nur ansehen, nicht ändern.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun CloseIcon(color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val a = 6.dp.toPx()
        val b = size.width - a
        val w = 2.dp.toPx()
        drawLine(color, Offset(a, a), Offset(b, b), w, StrokeCap.Round)
        drawLine(color, Offset(b, a), Offset(a, b), w, StrokeCap.Round)
    }
}

@Composable
private fun sentenceStyle(): TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)

/** "Mi, 14. Okt." */
private fun dateLabel(date: LocalDate): String {
    val weekday = date.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.GERMAN).take(2)
    val month = date.month.getDisplayName(DateTextStyle.SHORT, Locale.GERMAN)
    return "$weekday, ${date.dayOfMonth}. $month"
}

/**
 * Text für die Wiederholung, passend zum Datum des Termins:
 * nie / täglich / jeden Mittwoch / jeden Monat am 14. / jedes Jahr am 14. Oktober
 */
private fun recurrenceLabel(recurrence: Recurrence, date: LocalDate): String = when (recurrence) {
    Recurrence.NONE -> "nie"
    Recurrence.DAILY -> "täglich"
    Recurrence.WEEKLY -> "jeden " + date.dayOfWeek.getDisplayName(DateTextStyle.FULL, Locale.GERMAN)
    Recurrence.MONTHLY -> "jeden Monat am ${date.dayOfMonth}."
    Recurrence.YEARLY -> "jedes Jahr am ${date.dayOfMonth}. " + date.month.getDisplayName(DateTextStyle.FULL, Locale.GERMAN)
}

/**
 * Schnellauswahl fürs Serienende, passend zur Wiederholung.
 * Leere Liste = kein Ende-Feld anzeigen (bei "nie" und "jährlich").
 * Hier kannst du Optionen ergänzen oder ändern, z. B. "Semesterende".
 */
private fun endPresets(recurrence: Recurrence): List<Pair<String, Period>> = when (recurrence) {
    Recurrence.DAILY -> listOf("1 Woche" to Period.ofWeeks(1), "1 Monat" to Period.ofMonths(1), "3 Monate" to Period.ofMonths(3))
    Recurrence.WEEKLY -> listOf("1 Monat" to Period.ofMonths(1), "6 Monate" to Period.ofMonths(6), "1 Jahr" to Period.ofYears(1))
    Recurrence.MONTHLY -> listOf("6 Monate" to Period.ofMonths(6), "1 Jahr" to Period.ofYears(1), "2 Jahre" to Period.ofYears(2))
    Recurrence.NONE, Recurrence.YEARLY -> emptyList()
}

private val LONG_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)
private val SHORT_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)

/** "18:00", oder "Do 01:00", wenn der Termin über Mitternacht geht */
private fun endLabel(draft: EventDraft): String {
    val time = draft.end.format(TIME_FORMAT)
    if (draft.end.toLocalDate() == draft.start.toLocalDate()) return time
    val weekday = draft.end.dayOfWeek.getDisplayName(DateTextStyle.SHORT, Locale.GERMAN).take(2)
    return "$weekday $time"
}

// ---------------------------------------------------------------------------
// Vorschau
// ---------------------------------------------------------------------------

private val previewDay = LocalDate.of(2026, 10, 14)

private val previewDayEvents = listOf(
    CalendarEvent(1, 1, "Vorlesung", previewDay.atTime(8, 15), previewDay.atTime(9, 45), false, "H11", 0xFF415F91.toInt()),
    CalendarEvent(2, 2, "Mittagessen", previewDay.atTime(12, 30), previewDay.atTime(13, 30), false, "Mensa", 0xFF8A4E8F.toInt()),
    CalendarEvent(11, 1, "Lerngruppe", previewDay.atTime(16, 0), previewDay.atTime(18, 0), false, "Bibliothek", 0xFF415F91.toInt()),
)

private val previewDraft = EventDraft(
    id = 11,
    calendarId = 1,
    title = "Lerngruppe",
    start = previewDay.atTime(16, 0),
    end = previewDay.atTime(18, 30),
    location = "Bibliothek",
)

@Preview(name = "Hell", showBackground = true, widthDp = 390, heightDp = 1000)
@Preview(name = "Dunkel", showBackground = true, widthDp = 390, heightDp = 1000, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EventEditorPreview() {
    CalendarTheme(dynamicColor = false) {
        EventEditorScreen(
            draft = previewDraft,
            calendars = previewCalendars,
            dayEvents = previewDayEvents,
            onDraftChange = {},
            onSave = {},
            onClose = {},
            onDelete = {},
            now = previewDay.atTime(11, 20),
        )
    }
}

@Preview(name = "Serie", showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun EventEditorRecurringPreview() {
    CalendarTheme(dynamicColor = false) {
        EventEditorScreen(
            draft = previewDraft,
            calendars = previewCalendars,
            dayEvents = previewDayEvents,
            onDraftChange = {},
            onSave = {},
            onClose = {},
            onDelete = {},
            isRecurring = true,
            now = previewDay.atTime(11, 20),
        )
    }
}

@Preview(name = "Gesperrt", showBackground = true, widthDp = 390, heightDp = 1000)
@Composable
private fun EventEditorLockedPreview() {
    CalendarTheme(dynamicColor = false) {
        EventEditorScreen(
            draft = previewDraft,
            calendars = previewCalendars,
            dayEvents = previewDayEvents,
            onDraftChange = {},
            onSave = {},
            onClose = {},
            onDelete = {},
            locked = true,
            now = previewDay.atTime(11, 20),
        )
    }
}
