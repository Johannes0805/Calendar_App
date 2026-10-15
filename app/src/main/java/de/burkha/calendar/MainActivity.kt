package de.burkha.calendar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.burkha.calendar.data.CalendarRepository
import de.burkha.calendar.ui.permission.CalendarPermission
import de.burkha.calendar.ui.theme.CalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = CalendarRepository(applicationContext.contentResolver)

        setContent {
            CalendarTheme{}
                CalendarPermission {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        CalendarTestScreen(repository, Modifier.padding(innerPadding))
                    }
                }
        }
    }
}



@Composable
fun CalendarTestScreen(repository: CalendarRepository, modifier: Modifier = Modifier) {


    // 4. Kalender laden, und zwar neu, sobald sich hasPermission ändert.
    val calendars = remember() {
        repository.getCalendars()
    }

    // 5. Anzeigen.
    Column(modifier = modifier.padding(16.dp)) {
        when {
            calendars.isEmpty() -> Text("Keine Kalender gefunden.")
            else -> calendars.forEach { cal ->
                Text("${cal.name}  (${cal.accountName})")
            }
        }
    }
}

