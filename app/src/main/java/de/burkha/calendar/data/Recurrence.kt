package de.burkha.calendar.data

/**
 * Wie oft sich ein Termin wiederholt.
 *
 * Bewusst einfach gehalten: "wöchentlich" heißt immer "am selben Wochentag wie der erste Termin",
 * "monatlich" immer "am selben Tag im Monat". Das deckt die meisten Fälle ab
 * (Vorlesung jeden Montag, Miete am 1., Geburtstag jedes Jahr).
 *
 * Wie daraus eine RRULE für Android wird, entscheidet das Repository.
 */
enum class Recurrence {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}
