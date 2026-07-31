package com.dream.wowiptv.presentation.common

import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun rememberDeviceStatus(): String {
    val context = LocalContext.current
    val batteryManager = remember { context.getSystemService(BatteryManager::class.java) }
    var timeText by remember { mutableStateOf("") }
    var battery by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        while (true) {
            timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            battery = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            delay(30000)
        }
    }

    return if (battery >= 0) "$timeText  $battery%" else timeText
}
