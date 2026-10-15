package de.burkha.calendar.ui.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

private val CALENDAR_PERMISSIONS = arrayOf(
    Manifest.permission.READ_CALENDAR,
    Manifest.permission.WRITE_CALENDAR,
)

@Composable
fun CalendarPermission(content: @Composable () -> Unit) {
    // Get context of current
    val localContext = LocalContext.current

    var amountReqPer by remember {mutableStateOf(0)}

    //list of all permissions missing -> for settings(future) and to request specific ones
    var missingPermission: List<String> by remember {
        mutableStateOf(CALENDAR_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(
                localContext, it
            ) != PackageManager.PERMISSION_GRANTED
        })
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> missingPermission = result.filter { !it.value }.keys.toList() }



    LaunchedEffect(amountReqPer) {
        if (missingPermission.isNotEmpty()) {
            //request Permission
            val misPer: Array<String> = missingPermission.toTypedArray()

            launcher.launch(misPer)
        }
    }

    if(missingPermission.isEmpty()){
        content()
    }else{
        PermissionDeniedScreen(
            onRequestAgain = {amountReqPer += 1},
            onOpenSettings = {val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", localContext.packageName, null),
            )
                localContext.startActivity(intent)},
            permanentlyDenied = amountReqPer >= 1
        )
    }

}
