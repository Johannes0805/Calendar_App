package de.burkha.calendar.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Gemeinsame Kopfzeile für Tag, Woche und Monat: oben klein das Jahr mit Pfeilen,
 * darunter groß der Monat, rechts daneben der ViewSwitcher.
 *
 * Weil alle drei Ansichten genau diese Funktion benutzen, sind Höhe und Position
 * überall gleich. Beim Wechseln der Ansicht springt im Kopf nichts.
 *
 * @param date           der gewählte Tag. Angezeigt werden nur sein Jahr und sein Monat.
 * @param onPrevious     linker Pfeil. Was "zurück" heißt (Tag, Woche, Monat), entscheidet die Ansicht.
 * @param previousLabel  Text für Bildschirmleser, z. B. "Vorheriger Tag"
 * @param action         Slot rechts, z. B. für den ViewSwitcher
 */
@Composable
fun CalendarHeader(
    date: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    previousLabel: String,
    nextLabel: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Abstand nach oben: hier kleiner oder größer machen
            .padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = date.year.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                ChevronButton(pointsLeft = true, label = previousLabel, onClick = onPrevious)
                ChevronButton(pointsLeft = false, label = nextLabel, onClick = onNext)
            }
            Text(
                text = date.month.getDisplayName(TextStyle.FULL, Locale.GERMAN),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (action != null) {
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .padding(bottom = 2.dp),
            ) {
                action()
            }
        }
    }
}
