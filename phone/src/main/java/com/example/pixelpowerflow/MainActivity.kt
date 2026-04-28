package com.example.pixelpowerflow

import android.os.Bundle
import android.util.Log // これが重要です
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    // 状態管理用の変数
    private var currentMa by mutableStateOf(0)
    private var batteryLevel by mutableStateOf(0)
    private var isCharging by mutableStateOf(false)
    private var lastSyncTime by mutableStateOf("No Data") // これを追加

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Watch Data Monitor", fontSize = 18.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(30.dp))

                        Text(text = "$batteryLevel%", fontSize = 72.sp)

                        val statusColor = if (isCharging) Color(0xFF4285F4) else Color(0xFFEA4335)
                        Text(text = "${currentMa}mA", fontSize = 48.sp, color = statusColor)
                        Text(text = if (isCharging) "CHARGING" else "DISCHARGING", color = statusColor)

                        Spacer(modifier = Modifier.height(40.dp))
                        Text(text = "Last Sync: $lastSyncTime", fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 1. リスナー登録
        Wearable.getDataClient(this).addListener(this)
        Log.d("BatterySync", "Phone: リスナーを登録しました。")

        // 2. 明示的に接続デバイスを確認する（通信路の活性化）
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Log.d("BatterySync", "Phone: 接続されているウォッチが見つかりません。Bluetoothを確認してください。")
                } else {
                    nodes.forEach { node ->
                        Log.d("BatterySync", "Phone: ウォッチを検出しました！ 名前: ${node.displayName}")
                    }
                }
            }
    }
    override fun onPause() {
        super.onPause()
        // リスナーを解除
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d("BatterySync", "Phone: データ受信イベントを検知しました。数: ${dataEvents.count}")

        dataEvents.forEach { event ->
            val uri = event.dataItem.uri
            Log.d("BatterySync", "Phone: 受信パス = ${uri.path}")

            if (uri.path == "/battery_status") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                val newMa = dataMap.getInt("current_ma", -999)
                val newLevel = dataMap.getInt("level", -999)
                val newCharging = dataMap.getBoolean("is_charging", false)

                Log.d("BatterySync", "Phone: 解析成功 -> mA: $newMa, Level: $newLevel, Charging: $newCharging")

                if (newMa != -999) {
                    currentMa = newMa
                    batteryLevel = newLevel
                    isCharging = newCharging

                    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    lastSyncTime = sdf.format(java.util.Date())
                } else {
                    Log.e("BatterySync", "Phone: キー名 'current_ma' が見つかりませんでした。")
                }
            }
        }
    } // onDataChanged の閉じ
} // MainActivity クラスの閉じ