package de.burkha.calendar.ui.editor

/**
 * Was beim Speichern oder Löschen gemeint ist, wenn der Termin zu einer Serie gehört.
 *
 * Bei normalen Terminen meldet der Editor immer ALL. Es gibt dort ja nur diesen einen,
 * "alle" und "nur dieser" ist also dasselbe.
 */
enum class EditScope {
    /** Nur der eine angetippte Termin. Wird in Android als Ausnahme der Serie gespeichert. */
    THIS,

    /** Die ganze Serie. */
    ALL,
}
