package de.burkha.calendar.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Eckenrundungen der ganzen App. Größere Werte = runder.
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // Termine in der Wochenansicht (schmale Spalten)
    small = RoundedCornerShape(10.dp),       // Uhrzeit-Buttons im Editor
    medium = RoundedCornerShape(16.dp),      // Termin-Kacheln, Plus-Button
    large = RoundedCornerShape(18.dp),       // Tage im Wochenstreifen der Tagesansicht
    extraLarge = RoundedCornerShape(28.dp),  // Terminbereich unter dem Monatsraster
)
