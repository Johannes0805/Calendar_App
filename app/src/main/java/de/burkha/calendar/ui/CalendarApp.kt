package de.burkha.calendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import de.burkha.calendar.data.CalendarRepository
import de.burkha.calendar.ui.day.DayScreen
import de.burkha.calendar.ui.editor.EditScope
import de.burkha.calendar.ui.editor.EventDetailSheet
import de.burkha.calendar.ui.editor.EventEditorScreen
import de.burkha.calendar.ui.editor.QuickAddSheet
import de.burkha.calendar.ui.month.MonthScreen
import de.burkha.calendar.ui.week.WeekScreen
import java.time.LocalDateTime
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun CalendarApp(repository: CalendarRepository){
    val viewModel = viewModel { CalendarViewModel(repository) }
    var now by remember {mutableStateOf(LocalDateTime.now())}
    // refresh data when app is brought back to foreground
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {viewModel.reload()}
    // reload the now value after app is back in foreground
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {now = LocalDateTime.now()}

    LaunchedEffect(Unit) {
        while(true) {
            now = LocalDateTime.now()
            delay((60L - now.second).seconds)
        }
    }

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
            onSave = {x -> viewModel.saveDraft(x)},
            onClose = {viewModel.closeEditor()},
            onDelete = {x -> viewModel.deleteDraft(x)},
            isRecurring = viewModel.editorIsRecurring,
            isException = viewModel.editorIsException,
            now = now
        )
    }else {
        when (viewModel.currentView) {
            CalendarView.DAY -> DayScreen(
                viewModel.selectedDate,
                viewModel.events,
                { x -> viewModel.selectDate(x) },
                {x -> viewModel.openEvent(x)},
                {viewModel.openNewEvent()},
                viewSwitcher = switcher,
                now = now
            )

            CalendarView.WEEK -> WeekScreen(
                viewModel.selectedDate,
                viewModel.events,
                { x -> viewModel.selectDate(x) },
                {x -> viewModel.openEvent(x)},
                {viewModel.openNewEvent()},
                viewSwitcher = switcher,
                now = now
            )

            CalendarView.MONTH -> MonthScreen(
                viewModel.selectedDate,
                viewModel.events,
                { x -> viewModel.selectDate(x) },
                {x -> viewModel.openEvent(x)},
                {viewModel.openNewEvent()},
                viewSwitcher = switcher,
                now = now
            )
        }
        if (draft != null && viewModel.editorMode == EditorMode.QUICK) {
            QuickAddSheet(
                draft,
                viewModel.writableCalendars,
                onDraftChange = {x -> viewModel.updateDraft(x) },
                onSave = {viewModel.saveDraft(EditScope.ALL)},
                onMoreDetails = { viewModel.showFullEditor() },
                onDismiss = { viewModel.closeEditor() }
            )
        }
        if (draft != null && viewModel.editorMode == EditorMode.DETAIL){
            EventDetailSheet(
                draft = draft,
                calendars = viewModel.allCalendars,
                onEdit = {viewModel.editEvent()},
                onDelete = {x -> viewModel.deleteDraft(x)},
                onDismiss = {viewModel.closeEditor()},
                onDuplicate = {viewModel.duplicateDraft()},
                isRecurring = viewModel.editorIsRecurring,
                isException = viewModel.editorIsException,
                now = now
            )
        }
    }
}