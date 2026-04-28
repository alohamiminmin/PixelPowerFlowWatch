package com.example.pixelpowerflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    private var currentMa by mutableIntStateOf(0)
    private var batteryLevel by mutableIntStateOf(0)
    private var isCharging by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Watch Battery Monitor", fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = "Level: $batteryLevel%", fontSize = 40.sp)
                        Text(text = "Current: ${currentMa}mA", fontSize = 40.sp, color = if (isCharging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        Text(text = if (isCharging) "Charging" else "Discharging", fontSize = 20.sp)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.uri.path == "/battery_status") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                currentMa = dataMap.getInt("current_ma")
                batteryLevel = dataMap.getInt("level")
                isCharging = dataMap.getBoolean("is_charging")
            }
        }
    }
}