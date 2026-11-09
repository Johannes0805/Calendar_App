package de.burkha.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.burkha.calendar.data.CalendarRepository
import de.burkha.calendar.ui.CalendarApp
import de.burkha.calendar.ui.permission.CalendarPermission
import de.burkha.calendar.ui.theme.CalendarTheme

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
