package de.burkha.calendar.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.burkha.calendar.ui.theme.CalendarTheme

// Alle Dialoge und Aufklappmenüs der App an einer Stelle, damit sie gleich aussehen:
// stark gerundete Karten in den Farben der Ansichten statt der Standard-Optik von Material.

/** Rundung aller Dialoge. Hier ändern, dann ändert es sich überall. */
val DialogShape = RoundedCornerShape(32.dp)

/** Rundung der Aufklappmenüs (Kalender, Wiederholung, Ende). */
val MenuShape = RoundedCornerShape(20.dp)

/**
 * Grundgerüst für alle Dialoge: eine gerundete Karte in der Mitte.
 *
 * @param title      Überschrift
 * @param onDismiss  daneben getippt oder Zurück-Geste
 * @param text       optionaler Erklärsatz unter der Überschrift
 * @param buttons    Knöpfe unten rechts, z. B. "Abbrechen"
 * @param content    der eigentliche Inhalt, z. B. Auswahl-Kacheln
 */
@Composable
fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    text: String? = null,
    buttons: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    // Dialog statt AlertDialog: Dialog gibt nur das Fenster vor (abgedunkelter Hintergrund,
    // Zurück-Geste), das Aussehen der Karte bestimmen wir selbst.
    Dialog(onDismissRequest = onDismiss) {
        DialogCard(title = title, text = text, buttons = buttons, content = content)
    }
}

/** Die Karte selbst, ohne Fenster. Extra, damit die Vorschau in Android Studio sie zeigen kann. */
@Composable
private fun DialogCard(
    title: String,
    text: String?,
    buttons: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = DialogShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            ) {
                buttons()
            }
        }
    }
}

/** Wie viele Punkte das Symbol links in einer Auswahl-Kachel zeigt. */
enum class ChoiceIcon {
    /** ein Punkt: ein einzelner Termin */
    SINGLE,

    /** drei Punkte: die ganze Serie */
    SERIES,
}

/**
 * Große antippbare Kachel: Symbol links, fetter Titel, kleine Erklärung darunter.
 *
 * @param emphasized   true = farbig hinterlegt, für die naheliegende Wahl
 * @param destructive  true = rot, weil dabei etwas verloren geht (z. B. ganze Serie löschen)
 */
@Composable
fun DialogChoice(
    title: String,
    subtitle: String,
    icon: ChoiceIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    destructive: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val background = when {
        destructive -> colors.errorContainer
        emphasized -> colors.primaryContainer
        else -> colors.surfaceContainerLow
    }
    val foreground = when {
        destructive -> colors.onErrorContainer
        emphasized -> colors.onPrimaryContainer
        else -> colors.onSurface
    }
    // Das Symbol-Kästchen hebt sich immer vom Hintergrund der Kachel ab
    val iconBackground = if (destructive || emphasized) colors.surface else colors.primaryContainer
    val dotColor = if (destructive) colors.error else colors.primary

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = background,
        contentColor = foreground,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ChoiceDots(icon = icon, dotColor = dotColor, background = iconBackground)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    // etwas blasser als der Titel, aber in derselben Farbfamilie
                    color = foreground.copy(alpha = 0.78f),
                )
            }
        }
    }
}

@Composable
private fun ChoiceDots(icon: ChoiceIcon, dotColor: Color, background: Color) {
    val count = if (icon == ChoiceIcon.SINGLE) 1 else 3
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(count) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }
        }
    }
}

/**
 * Einfache Rückfrage mit zwei Knöpfen, z. B. "Termin löschen?" oder "Entwurf verwerfen?".
 *
 * @param destructive  true = der Bestätigen-Knopf ist rot
 */
@Composable
fun AppConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Abbrechen",
    destructive: Boolean = false,
) {
    AppDialog(
        title = title,
        text = text,
        onDismiss = onDismiss,
        buttons = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
            TextButton(
                onClick = onConfirm,
                colors = if (destructive) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors()
                },
            ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
        },
    )
}

/**
 * Aufklappmenü im App-Stil: gerundet, in der Farbe der Blätter.
 * Benutzung wie DropdownMenu, die Einträge sind ganz normale DropdownMenuItems.
 */
@Composable
fun AppMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = MenuShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        content = content,
    )
}

// ---------------------------------------------------------------------------
// Vorschau
// ---------------------------------------------------------------------------

@Composable
private fun ScopePreviewContent(delete: Boolean) {
    DialogCard(
        title = if (delete) "Wiederholenden Termin löschen" else "Wiederholenden Termin ändern",
        text = null,
        buttons = { TextButton(onClick = {}) { Text("Abbrechen") } },
    ) {
        DialogChoice(
            title = "Nur diesen Termin",
            subtitle = if (delete) "Nur dieses eine Mal fällt er weg" else "Die anderen Termine bleiben, wie sie sind",
            icon = ChoiceIcon.SINGLE,
            emphasized = true,
            onClick = {},
        )
        DialogChoice(
            title = if (delete) "Die ganze Serie" else "Alle Termine der Serie",
            subtitle = if (delete) "Alle Termine der Serie werden gelöscht" else "Die Änderung gilt für die ganze Serie",
            icon = ChoiceIcon.SERIES,
            destructive = delete,
            onClick = {},
        )
    }
}

@Preview(name = "Serie ändern", widthDp = 340, showBackground = true, backgroundColor = 0xFF8E9099)
@Composable
private fun ScopeChangePreview() {
    CalendarTheme(dynamicColor = false) { ScopePreviewContent(delete = false) }
}

@Preview(
    name = "Serie löschen, dunkel",
    widthDp = 340,
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ScopeDeleteDarkPreview() {
    CalendarTheme(dynamicColor = false) { ScopePreviewContent(delete = true) }
}

@Preview(name = "Rückfrage", widthDp = 340, showBackground = true, backgroundColor = 0xFF8E9099)
@Composable
private fun ConfirmPreview() {
    CalendarTheme(dynamicColor = false) {
        DialogCard(
            title = "Termin löschen?",
            text = "„Lerngruppe“ wird auch auf deinem Server gelöscht.",
            buttons = {
                TextButton(onClick = {}) { Text("Abbrechen") }
                TextButton(
                    onClick = {},
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Löschen", fontWeight = FontWeight.Bold) }
            },
            content = {},
        )
    }
}
