package de.burkha.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.burkha.calendar.ui.theme.CalendarTheme

/**
 * Platzhalter für Ansichten, die es noch nicht gibt (Monat, Woche).
 * Fliegt raus, sobald die echte Ansicht fertig ist.
 *
 * @param title         z. B. "Monatsansicht"
 * @param viewSwitcher  optional: der Umschalter oben, damit man zurückwechseln kann
 */
@Composable
fun ComingSoonScreen(
    title: String,
    modifier: Modifier = Modifier,
    viewSwitcher: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding(),
    ) {
        if (viewSwitcher != null) {
            // Gleiche Position wie in der Tagesansicht: oben rechts
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 16.dp, top = 16.dp)
                    .width(180.dp),
            ) {
                viewSwitcher()
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Kommt bald.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ComingSoonPreview() {
    CalendarTheme(dynamicColor = false) {
        ComingSoonScreen(
            title = "Monatsansicht",
            viewSwitcher = { ViewSwitcher(selected = CalendarView.MONTH, onSelect = {}) },
        )
    }
}
