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
import de.burkha.calendar.ui.editor.EditScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Duration

enum class EditorMode { QUICK, FULL }

class CalendarViewModel(private val repository: CalendarRepository) : ViewModel() {

    var selectedDate by mutableStateOf(LocalDate.now())
        private set

    var currentView by mutableStateOf(CalendarView.MONTH)
        private set

    var events by mutableStateOf<List<CalendarEvent>>(emptyList())
        private set

    var editorDraft by mutableStateOf<EventDraft?>(null)
        private set

    var editorMode by mutableStateOf(EditorMode.QUICK)
        private set

    var editorIsRecurring by mutableStateOf(false)
        private set

    var writableCalendars by mutableStateOf<List<CalendarInfo>>(emptyList())
        private set

    /**
     * Needed in case an existing event changes Calendar
     */
    var originalCalendarId: Long? = null

    /**
     * If the event is not recurring -> null
     * If the event is part of a series -> start
     * If the event is an exception -> original start of the event
     * */
    var originalDateRecurring: LocalDateTime? = null
        private set
    var seriesId: Long? = null
        private set

    var editorIsException: Boolean = false
        private set

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
        editorIsRecurring = false
        editorIsException = false
        originalDateRecurring = null
        seriesId = null
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
            location = event.location ?: "",
            note = event.description ?: "")
        editorMode = EditorMode.FULL
        editorIsRecurring = event.isRecurring
        originalDateRecurring = event.originalStart
        seriesId = event.seriesId
        editorIsException = editorIsRecurring && (seriesId != event.id)
    }

    fun showFullEditor(){
        editorMode = EditorMode.FULL
    }

    fun updateDraft(draft: EventDraft){
        editorDraft = draft
    }

    fun saveDraft(scope: EditScope){
        val draft = editorDraft ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val isNormal = seriesId == draft.id
                when {
                    draft.isNew -> {
                        repository.insertEvent(draft)
                    }
                    // User changed a single event of a recurring row
                    scope == EditScope.THIS -> {
                        if (isNormal){
                            repository.insertException(
                                seriesId ?: return@withContext,
                                originalDateRecurring ?: return@withContext,
                                draft.allDay,
                                draft
                            )
                        }else{
                            repository.updateException(draft)
                        }
                    }
                    // User changed all events of a recurring row
                    scope == EditScope.ALL && (seriesId != null) -> {
                        val shift = Duration.between(originalDateRecurring, draft.start)
                        repository.updateSeries(
                            seriesId ?: return@withContext,
                            shift,
                            draft
                        )
                    }
                    // User changed the Calendar of the event
                    draft.calendarId != originalCalendarId -> {
                        // Gets deleted from old calendar
                        repository.deleteEvent(draft.id!!)
                        // Inserted into new calendar
                        repository.insertEvent(draft)
                    }
                    else -> repository.updateEvent(draft)
                }
            }
            closeEditor()
            loadEvents()
        }
    }

    fun deleteDraft(scope: EditScope){
        val draft = editorDraft ?: return
        val isNormal = seriesId == draft.id

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (scope == EditScope.ALL) {
                    repository.deleteEvent(seriesId ?: draft.id ?: return@withContext)
                }else{
                    if (isNormal) {
                        repository.cancelInstance(
                            seriesId ?: return@withContext,
                            originalDateRecurring ?: return@withContext,
                            draft.allDay
                        )
                    }else{
                        repository.cancelException(draft)
                    }
                }
            }
            closeEditor()
            loadEvents()
        }
    }

    fun closeEditor(){
        // Reset
        editorDraft = null
        originalCalendarId = null
        seriesId = null
        originalDateRecurring = null
        editorIsException = false
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
            val dayOfWeek= selectedDate.dayOfWeek.value.toLong()
            from = selectedDate.minusDays(dayOfWeek)
            to = selectedDate.plusDays(7 - dayOfWeek)
        }else{
            val dayOfMonth = selectedDate.dayOfMonth.toLong()
            from = selectedDate.minusDays(dayOfMonth - 1)
            to = selectedDate.plusDays(selectedDate.lengthOfMonth() - dayOfMonth)
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getEvents(from, to)
            }
            events = result
        }
    }
}