package de.burkha.calendar.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.burkha.calendar.data.CalendarEvent
import de.burkha.calendar.data.CalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate

class CalendarViewModel(private val repository: CalendarRepository) : ViewModel() {

    var selectedDate by mutableStateOf(LocalDate.now())
        private set

    var currentView by mutableStateOf(CalendarView.DAY)
        private set

    var events by mutableStateOf<List<CalendarEvent>>(emptyList())
        private set

    init {
        loadEvents()
    }

    fun selectDate(date: LocalDate) {
        selectedDate = date
        loadEvents()
    }

    fun selectView(view: CalendarView) {
        currentView = view
        loadEvents()
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



        // TODO: from und to je nach currentView ausrechnen (when!)
        //   DAY  -> nur selectedDate
        //   WEEK -> Montag bis Sonntag dieser Woche
        //   MONTH -> erstmal wie DAY, kommt später

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.getEvents(from, to)
            }
            events = result
        }
    }
}