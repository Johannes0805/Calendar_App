package de.burkha.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.burkha.calendar.data.CalendarRepository
import de.burkha.calendar.ui.CalendarApp
import de.burkha.calendar.ui.CalendarViewModel
import de.burkha.calendar.ui.day.DayScreen
import de.burkha.calendar.ui.permission.CalendarPermission
import de.burkha.calendar.ui.theme.CalendarTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = CalendarRepository(applicationContext.contentResolver)


        setContent {
            CalendarTheme(dynamicColor = false) {
                CalendarPermission {
                        CalendarApp(repository)
                    }
                }
        }
    }
}
