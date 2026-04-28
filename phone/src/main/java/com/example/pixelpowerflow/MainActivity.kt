package com.example.pixelpowerflow

import android.content.Context
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    // Watch側の状態
    private var currentMa by mutableStateOf(0)
    private var batteryLevel by mutableStateOf(0)
    private var isCharging by mutableStateOf(false)
    private var lastSyncTime by mutableStateOf("No Data")
    private var lastSyncMillis by mutableStateOf(0L)

    // Phone自身の状態
    private var phoneMa by mutableStateOf(0)
    private var phoneLevel by mutableStateOf(0)
    private var isPhoneCharging by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // BatteryManagerの取得
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        setContent {
            var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

            // 1秒ごとの更新ループ
            LaunchedEffect(Unit) {
                while (true) {
                    currentTimeMillis = System.currentTimeMillis()

                    // Phone自身の情報を取得
                    val microAmps = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    phoneMa = abs(microAmps / 1000)
                    phoneLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
                    isPhoneCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL)

                    delay(1000)
                }
            }

            // 同期判定（30秒以上空いたら切断とみなす）
            val isSyncing = (currentTimeMillis - lastSyncMillis) < 30000 && lastSyncMillis != 0L

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- Watch セクション ---
                        Text("WATCH MONITOR", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isSyncing) "● SYNCING" else "○ DISCONNECTED",
                            color = if (isSyncing) Color(0xFF4CAF50) else Color.Red,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "$batteryLevel%", fontSize = 56.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${currentMa}mA",
                            fontSize = 28.sp,
                            color = if (isCharging) Color(0xFF4285F4) else Color(0xFFEA4335)
                        )
                        Text(text = if (isCharging) "CHARGING" else "DISCHARGING", fontSize = 12.sp)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 32.dp), thickness = 1.dp, color = Color.LightGray)

                        // --- Phone セクション ---
                        Text("PHONE MONITOR", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "$phoneLevel%", fontSize = 56.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${phoneMa}mA",
                            fontSize = 28.sp,
                            color = if (isPhoneCharging) Color(0xFF4285F4) else Color(0xFFEA4335)
                        )
                        Text(text = if (isPhoneCharging) "CHARGING" else "DISCHARGING", fontSize = 12.sp)

                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "Last Sync: $lastSyncTime", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }

// Phone側の MainActivity.kt 内

    private fun sendSignalToWatch(path: String) {
        val nodeClient = Wearable.getNodeClient(this)
        val messageClient = Wearable.getMessageClient(this)

        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                messageClient.sendMessage(node.id, path, null)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        // ウォッチに「送信開始」の合図を送る
        sendSignalToWatch("/start_sync")
    }

    override fun onPause() {
        super.onPause()
        // ウォッチに「送信停止」の合図を送る
        sendSignalToWatch("/stop_sync")
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/battery_status") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                currentMa = dataMap.getInt("current_ma")
                batteryLevel = dataMap.getInt("level")
                isCharging = dataMap.getBoolean("is_charging")

                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                lastSyncTime = sdf.format(Date())
                lastSyncMillis = System.currentTimeMillis()
            }
        }
    }
}