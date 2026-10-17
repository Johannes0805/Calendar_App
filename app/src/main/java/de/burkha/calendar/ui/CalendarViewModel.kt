package de.burkha.calendar.ui

import de.burkha.calendar.data.EventDraft
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.burkha.calendar.data.CalendarEvent
import de.burkha.calendar.data.CalendarInfo
import de.burkha.calendar.data.CalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class EditorMode { QUICK, FULL }

class CalendarViewModel(private val repository: CalendarRepository) : ViewModel() {

    var selectedDate by mutableStateOf(LocalDate.now())
        private set

    var currentView by mutableStateOf(CalendarView.DAY)
        private set

    var events by mutableStateOf<List<CalendarEvent>>(emptyList())
        private set

    var editorDraft by mutableStateOf<EventDraft?>(null)
        private set

    var editorMode by mutableStateOf(EditorMode.QUICK)
        private set

    var editorLocked by mutableStateOf(false)
        private set

    var writableCalendars by mutableStateOf<List<CalendarInfo>>(emptyList())
        private set

    // Needed in case an existing Event changes Calendar
    var originalCalendarId: Long? = null

    init {
        loadEvents()
        loadCalendars()
    }

    fun selectDate(date: LocalDate) {
        selectedDate = date
        loadEvents()
    }

    fun selectView(view: CalendarView) {
        currentView = view
        loadEvents()
    }

    fun openNewEvent(){
        originalCalendarId = null
        //Takes the first writeable calendar as standard, to be changed in the future
        editorDraft = EventDraft.newEvent(selectedDate, writableCalendars.firstOrNull()?.id)
        editorMode = EditorMode.QUICK
        editorLocked = false
    }

    fun openEvent(event: CalendarEvent){
        originalCalendarId = event.calendarId
        editorDraft = EventDraft(
            id = event.id,
            calendarId = event.calendarId,
            title = event.title,
            start = event.start,
            end = event.end,
            allDay = event.allDay,
            location = if(event.location != null) event.location!! else "",
            note = if(event.description != null) event.description!! else "")
        editorMode = EditorMode.FULL
        editorLocked = false
    }

    fun showFullEditor(){
        editorMode = EditorMode.FULL
    }

    fun updateDraft(draft: EventDraft){
        editorDraft = draft
    }

    fun saveDraft(){
        val draft = editorDraft ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                when {
                    draft.isNew -> {
                        repository.insertEvent(draft)
                    }

                    draft.calendarId != originalCalendarId -> {
                        repository.deleteEvent(draft.id!!)
                        repository.insertEvent(draft)
                    }

                    else -> repository.updateEvent(draft)
                }
            }
            closeEditor()
            loadEvents()
        }
    }

    fun deleteDraft(){
        val draft = editorDraft ?: return
        val draftId = draft.id ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteEvent(draftId)
            }
            closeEditor()
            loadEvents()
        }
    }

    fun closeEditor(){
        editorDraft = null
        originalCalendarId = null
    }

    private fun loadCalendars(){
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getCalendars().filter{it.canWrite}
            }
            writableCalendars = result
        }
    }

    private fun loadEvents() {

        var from: LocalDate
        var to: LocalDate

        if(currentView == CalendarView.DAY){
            from = selectedDate
            to = selectedDate
        }else if(currentView == CalendarView.WEEK){
            var dayOfWeek= selectedDate.dayOfWeek.value.toLong()
            from = selectedDate.minusDays(dayOfWeek)
            to = selectedDate.plusDays(7 - dayOfWeek)
        }else{
            var dayofMonth = selectedDate.dayOfMonth.toLong()
            from = selectedDate.minusDays(dayofMonth - 1)
            to = selectedDate.plusDays(selectedDate.lengthOfMonth() - dayofMonth)
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getEvents(from, to)
            }
            events = result
        }
    }
}