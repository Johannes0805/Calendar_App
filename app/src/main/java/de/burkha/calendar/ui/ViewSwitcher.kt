package de.burkha.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.burkha.calendar.ui.theme.CalendarTheme

/** Die drei Ansichten der App. Das ViewModel merkt sich, welche gerade aktiv ist. */
enum class CalendarView(val label: String) {
    MONTH("Monat"),
    WEEK("Woche"),
    DAY("Tag"),
}

/**
 * Segment-Leiste "Monat | Woche | Tag".
 *
 * Reine UI: zeigt an, was ausgewählt ist, und meldet Klicks per onSelect.
 *
 * @param selected  die aktuell aktive Ansicht
 * @param onSelect  wird mit der angetippten Ansicht aufgerufen
 */
@Composable
fun ViewSwitcher(
    selected: CalendarView,
    onSelect: (CalendarView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val outline = MaterialTheme.colorScheme.outline

    // Breite bestimmt der Aufrufer über modifier, z. B. Modifier.width(180.dp).
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(shape)
            .border(1.dp, outline, shape)
            .selectableGroup(),
    ) {
        CalendarView.entries.forEachIndexed { index, view ->
            if (index > 0) {
                // Trennlinie zwischen zwei Segmenten
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(outline),
                )
            }

            val isSelected = view == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(view) },
                        role = Role.Tab,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = view.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ViewSwitcherPreview() {
    CalendarTheme(dynamicColor = false) {
        ViewSwitcher(
            selected = CalendarView.DAY,
            onSelect = {},
            modifier = Modifier
                .padding(16.dp)
                .width(180.dp),
        )
    }
}
