package de.burkha.calendar.data

/**
 *
 */


data class CalendarInfo(
    val id: Long,
    val name: String,
    val color: Int,
    val accountName: String,
    val isVisible: Boolean,
    var canWrite: Boolean = true,
)
