package de.burkha.calendar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Schriftart der ganzen App. FontFamily.Default = Systemschrift des Handys.
// Für eine eigene Schrift (z. B. Figtree): .ttf-Dateien nach res/font legen und hier
// FontFamily(Font(R.font.figtree_regular), Font(R.font.figtree_semibold, FontWeight.SemiBold))
// eintragen.
private val AppFont = FontFamily.Default

// Material-3-Standardgrößen als Basis.
private val Base = Typography()

// Wo wird was benutzt:
// titleLarge  -> "Oktober 2026" in der Kopfzeile
// titleMedium -> Überschrift über der Terminliste ("Mittwoch, 14. Oktober")
// bodyLarge   -> Titel eines Termins
// bodyMedium  -> Uhrzeit / Ort unter dem Termin
// labelLarge  -> Buttons, Segment-Umschalter
// labelMedium -> Wochentage (Mo, Di, ...)
// labelSmall  -> Uhrzeiten an der Zeitachse
val AppTypography = Typography(
    titleLarge = Base.titleLarge.copy(fontFamily = AppFont, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontFamily = AppFont, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontFamily = AppFont, fontWeight = FontWeight.Medium),
    bodyMedium = Base.bodyMedium.copy(fontFamily = AppFont),
    labelLarge = Base.labelLarge.copy(fontFamily = AppFont, fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontFamily = AppFont, fontWeight = FontWeight.SemiBold),
    labelSmall = Base.labelSmall.copy(fontFamily = AppFont),
)
