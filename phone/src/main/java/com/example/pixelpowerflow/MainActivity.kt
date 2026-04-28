package com.example.pixelpowerflow

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

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    // 1. 状態管理用の変数（クラス直下に配置）
    private var currentMa by mutableStateOf(0)
    private var batteryLevel by mutableStateOf(0)
    private var isCharging by mutableStateOf(false)
    private var lastSyncTime by mutableStateOf("No Data")
    private var lastSyncMillis by mutableStateOf(0L) // 判定用

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // 2. UI内部での現在時刻管理
            var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

            // 1秒ごとに現在時刻を更新するループ
            LaunchedEffect(Unit) {
                while (true) {
                    currentTimeMillis = System.currentTimeMillis()
                    delay(1000)
                }
            }

            // 同期判定（最後に受信してから10秒以内なら接続中とみなす）
            // ウォッチの送信間隔に合わせて秒数は調整してください
            val isSyncing = (currentTimeMillis - lastSyncMillis) < 10000 && lastSyncMillis != 0L

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 同期ステータス表示
                        Text(
                            text = if (isSyncing) "● SYNCING" else "○ DISCONNECTED",
                            color = if (isSyncing) Color(0xFF4CAF50) else Color.Red,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Watch Data Monitor", fontSize = 18.sp, color = Color.Gray)

                        Spacer(modifier = Modifier.height(20.dp))
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
        Wearable.getDataClient(this).addListener(this)
        Log.d("BatterySync", "Phone: リスナーを登録しました。")
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    // データを受信した時の処理
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/battery_status") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                currentMa = dataMap.getInt("current_ma")
                batteryLevel = dataMap.getInt("level")
                isCharging = dataMap.getBoolean("is_charging")

                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                lastSyncTime = sdf.format(Date())

                // 受信した時刻をミリ秒で記録
                lastSyncMillis = System.currentTimeMillis()
                Log.d("BatterySync", "Phone: データ更新検知 - ${currentMa}mA")
            }
        }
    }
}