package de.burkha.calendar.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import de.burkha.calendar.data.CalendarRepository
import de.burkha.calendar.ui.day.DayScreen
import de.burkha.calendar.ui.month.MonthScreen
import de.burkha.calendar.ui.week.WeekScreen

@Composable
fun CalendarApp(repository: CalendarRepository){
    val viewModel = viewModel { CalendarViewModel(repository) }

    val switcher: @Composable () -> Unit = {
        ViewSwitcher(selected = viewModel.currentView, onSelect = {x: CalendarView -> viewModel.selectView(x)})
    }

    when(viewModel.currentView){
        CalendarView.DAY -> DayScreen(
            viewModel.selectedDate,
            viewModel.events,
            {x -> viewModel.selectDate(x)},
            {},
            {},
            viewSwitcher = switcher
        )
        CalendarView.WEEK -> WeekScreen(
            viewModel.selectedDate,
            viewModel.events,
            {x -> viewModel.selectDate(x)},
            {},
            {},
            viewSwitcher = switcher
        )
        CalendarView.MONTH -> MonthScreen(
            viewModel.selectedDate,
            viewModel.events,
            {x -> viewModel.selectDate(x)},
            {},
            {},
            viewSwitcher = switcher
        )
    }
}