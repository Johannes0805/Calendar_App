package de.burkha.calendar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Farbe für alles, was "jetzt" markiert: Punkt und Linie im Zeitstrahl, Strich im Wochenbalken.
 * Ein etwas weicheres, moderneres Rot als das Fehler-Rot von Material.
 * Hier an EINER Stelle ändern, dann ändert es sich überall.
 */
@Composable
fun nowColor(): Color {
    // Dunkles Theme erkennt man an einem dunklen Hintergrund
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (isDark) Color(0xFFFF6369) else Color(0xFFE5484D)
}

/** Plus-Symbol für den Button "Neuer Termin". Gezeichnet, damit keine Icon-Bibliothek nötig ist. */
@Composable
fun PlusIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val c = size.width / 2
        val l = 7.dp.toPx()
        val w = 2.dp.toPx()
        drawLine(color, Offset(c, c - l), Offset(c, c + l), w, StrokeCap.Round)
        drawLine(color, Offset(c - l, c), Offset(c + l, c), w, StrokeCap.Round)
    }
}

/** Kleiner runder Pfeil-Button (zurück / weiter). */
@Composable
fun ChevronButton(pointsLeft: Boolean, label: String, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 2.dp.toPx()
            val tipX = if (pointsLeft) w * 0.3f else w * 0.7f
            val backX = if (pointsLeft) w * 0.7f else w * 0.3f
            drawLine(color, Offset(backX, h * 0.1f), Offset(tipX, h * 0.5f), stroke, StrokeCap.Round)
            drawLine(color, Offset(tipX, h * 0.5f), Offset(backX, h * 0.9f), stroke, StrokeCap.Round)
        }
    }
}

/** Kachel mit großer Zahl und kleinem Text darunter, z. B. "23 / Termine". */
@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
