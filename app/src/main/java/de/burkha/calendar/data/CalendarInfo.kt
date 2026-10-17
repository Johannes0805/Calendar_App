package de.burkha.calendar.data

// datatype to save a calendar from the calendar database of android

data class CalendarInfo(
    val id: Long,
    val name: String,
    val color: Int,
    val accountName: String,
    val isVisible: Boolean,
    var canWrite: Boolean = true,
)
