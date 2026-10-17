package de.burkha.calendar.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import de.burkha.calendar.data.CalendarRepository
import de.burkha.calendar.ui.day.DayScreen
import de.burkha.calendar.ui.editor.EventEditorScreen
import de.burkha.calendar.ui.editor.QuickAddSheet
import de.burkha.calendar.ui.month.MonthScreen
import de.burkha.calendar.ui.week.WeekScreen

@Composable
fun CalendarApp(repository: CalendarRepository){
    val viewModel = viewModel { CalendarViewModel(repository) }
    val draft = viewModel.editorDraft

    val switcher: @Composable () -> Unit = {
        ViewSwitcher(selected = viewModel.currentView, onSelect = {x: CalendarView -> viewModel.selectView(x)})
    }
    if(draft != null && viewModel.editorMode == EditorMode.FULL){
        EventEditorScreen(
            draft = draft,
            calendars = viewModel.writableCalendars,
            dayEvents = viewModel.events,
            onDraftChange = {x -> viewModel.updateDraft(x)},
            onSave = {viewModel.saveDraft()},
            onClose = {viewModel.closeEditor()},
            onDelete = {viewModel.deleteDraft()},
            locked = viewModel.editorLocked
        )
    }else {
        when (viewModel.currentView) {
            CalendarView.DAY -> DayScreen(
                viewModel.selectedDate,
                viewModel.events,
                { x -> viewModel.selectDate(x) },
                {x -> viewModel.openEvent(x)},
                {viewModel.openNewEvent()},
                viewSwitcher = switcher
            )

            CalendarView.WEEK -> WeekScreen(
                viewModel.selectedDate,
                viewModel.events,
                { x -> viewModel.selectDate(x) },
                {x -> viewModel.openEvent(x)},
                {viewModel.openNewEvent()},
                viewSwitcher = switcher
            )

            CalendarView.MONTH -> MonthScreen(
                viewModel.selectedDate,
                viewModel.events,
                { x -> viewModel.selectDate(x) },
                {x -> viewModel.openEvent(x)},
                {viewModel.openNewEvent()},
                viewSwitcher = switcher
            )
        }
        if (draft != null) {
            QuickAddSheet(
                draft,
                viewModel.writableCalendars,
                { x -> viewModel.updateDraft(x) },
                { viewModel.saveDraft() },
                { viewModel.showFullEditor() },
                { viewModel.closeEditor() }
            )
        }
    }
}