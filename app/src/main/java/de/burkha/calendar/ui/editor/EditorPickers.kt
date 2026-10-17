package de.burkha.calendar.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.burkha.calendar.data.CalendarInfo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

// Kleine Bausteine, die beide Editoren benutzen: Datum wählen, Uhrzeit wählen, Kalender wählen.
// Die Dialoge von Material 3 gelten noch als "experimentell", deshalb das @OptIn.
// Das heißt nur: Google darf die API noch ändern. Funktionieren tun sie.

/**
 * Datumsauswahl als Dialog.
 *
 * Achtung, Falle: Der DatePicker rechnet in Millisekunden in UTC (Mitternacht UTC),
 * nicht in deiner Zeitzone. Deshalb wird hier überall mit ZoneOffset.UTC umgerechnet.
 * Kennst du schon von den ganztägigen Terminen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialogFor(
    date: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    ) {
        DatePicker(state = state)
    }
}

/** Uhrzeitauswahl als Dialog (24-Stunden-Uhr). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogFor(
    time: LocalTime,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onPick(LocalTime.of(state.hour, state.minute))
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        text = { TimePicker(state = state) },
    )
}

/** Aufklappmenü mit allen Kalendern. Hängt sich an das Element, in dem es steht. */
@Composable
fun CalendarMenu(
    expanded: Boolean,
    calendars: List<CalendarInfo>,
    onPick: (CalendarInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        calendars.forEach { calendar ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CalendarDot(color = Color(calendar.color))
                        Text(calendar.name)
                    }
                },
                onClick = {
                    onPick(calendar)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
fun CalendarDot(color: Color, size: Dp = 10.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}
