package de.burkha.calendar.ui.theme

import androidx.compose.ui.unit.dp

// Alle Abstände und Größen an einer Stelle.
// Ändere hier einen Wert, und er ändert sich in jedem Screen.
object Dimens {
    // Allgemein
    val ScreenPadding = 16.dp       // Abstand zum Bildschirmrand
    val ItemSpacing = 10.dp         // Abstand zwischen Termin-Kacheln
    val TopBarHeight = 64.dp
    val SegmentHeight = 40.dp       // Umschalter Monat / Woche / Tag
    val Fab = 56.dp                 // Plus-Button

    // Monatsansicht
    val DayCellHeight = 54.dp
    val DayCircle = 34.dp           // Kreis um die Tageszahl
    val EventDot = 6.dp             // Farbpunkte unter dem Tag

    // Zeitachse (Woche + Tag)
    val TimeColumnWidth = 56.dp     // Spalte mit den Uhrzeiten links
    val HourHeightWeek = 48.dp      // Höhe einer Stunde in der Wochenansicht
    val HourHeightDay = 64.dp       // Höhe einer Stunde in der Tagesansicht
    val NowLine = 2.dp              // Dicke der roten "Jetzt"-Linie
}
