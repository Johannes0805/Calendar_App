package de.burkha.calendar.ui.permission

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.burkha.calendar.ui.theme.CalendarTheme

/**
 * Wird angezeigt, wenn der Kalenderzugriff fehlt.
 *
 * Reine UI: Der Screen entscheidet nichts selbst, er meldet nur Klicks nach oben.
 *
 * @param onRequestAgain     Klick auf "Zugriff erlauben" -> Berechtigungsdialog erneut starten.
 * @param onOpenSettings     Klick auf "Einstellungen öffnen" -> App-Einstellungen von Android öffnen.
 * @param permanentlyDenied  true = Android zeigt den Dialog nicht mehr an (zweimal abgelehnt).
 *                           Dann gibt es nur noch den Weg über die Einstellungen.
 */
@Composable
fun PermissionDeniedScreen(
    onRequestAgain: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    permanentlyDenied: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CalendarBadge()

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Kalenderzugriff benötigt",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (permanentlyDenied) {
                "Du hast den Zugriff abgelehnt. Du kannst ihn in den App-Einstellungen " +
                    "unter „Berechtigungen → Kalender“ wieder erlauben."
            } else {
                "Die App zeigt die Termine aus dem Kalender deines Handys an, " +
                    "zum Beispiel die über DAVx⁵ synchronisierten. Ohne Zugriff bleibt sie leer."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        if (permanentlyDenied) {
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Einstellungen öffnen")
            }
        } else {
            Button(onClick = onRequestAgain, modifier = Modifier.fillMaxWidth()) {
                Text("Zugriff erlauben")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Einstellungen öffnen")
            }
        }
    }
}

/** Runder Kreis mit einem gezeichneten Kalender-Symbol. Kommt ohne Icon-Bibliothek aus. */
@Composable
private fun CalendarBadge() {
    val iconColor = MaterialTheme.colorScheme.onPrimaryContainer

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(96.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(44.dp)) {
                val w = size.width
                val h = size.height
                val stroke = 3.dp.toPx()

                // Kalenderblatt
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(stroke / 2, h * 0.14f),
                    size = Size(w - stroke, h * 0.86f - stroke / 2),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = stroke),
                )
                // Linie unter der Kopfzeile
                drawLine(iconColor, Offset(0f, h * 0.40f), Offset(w, h * 0.40f), stroke)
                // Zwei Ringe oben
                drawLine(iconColor, Offset(w * 0.3f, 0f), Offset(w * 0.3f, h * 0.26f), stroke, StrokeCap.Round)
                drawLine(iconColor, Offset(w * 0.7f, 0f), Offset(w * 0.7f, h * 0.26f), stroke, StrokeCap.Round)
            }
        }
    }
}

// ---------- Vorschau in Android Studio (Split-Ansicht) ----------

@Preview(showBackground = true, name = "Erstmals abgelehnt")
@Composable
private fun PermissionDeniedPreview() {
    CalendarTheme(dynamicColor = false) {
        PermissionDeniedScreen(onRequestAgain = {}, onOpenSettings = {})
    }
}

@Preview(showBackground = true, name = "Dauerhaft abgelehnt")
@Composable
private fun PermissionPermanentlyDeniedPreview() {
    CalendarTheme(dynamicColor = false) {
        PermissionDeniedScreen(onRequestAgain = {}, onOpenSettings = {}, permanentlyDenied = true)
    }
}
