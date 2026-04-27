package com.example.pixelpowerflowwatch.presentation

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material3.Text

class MainActivity : ComponentActivity() {

    private val chargingReceiver = ChargingReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // BATTERY_CHANGED を動的登録（Manifest では受け取れない）
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(chargingReceiver, filter)

        // Activity が前面にあるので Foreground Service 起動が許可される
        val serviceIntent = Intent(this, BatteryService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            Text("Pixel Power Flow Watch")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(chargingReceiver)
    }
}
