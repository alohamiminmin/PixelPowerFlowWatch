package com.example.pixelpowerflow

import android.content.Context
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

// Watch1台分のデータ
data class WatchData(
    val watchId: String,
    val currentMa: Int = 0,
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val lastSyncMillis: Long = 0L,
    val lastSyncTime: String = "No Data"
)

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {

    // Watch複数台分（watchId → データ）
    private val watchDataMap = mutableStateMapOf<String, WatchData>()

    // Phone自身の状態
    private var phoneMa by mutableStateOf(0)
    private var phoneLevel by mutableStateOf(0)
    private var isPhoneCharging by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        setContent {
            var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

            LaunchedEffect(Unit) {
                while (true) {
                    currentTimeMillis = System.currentTimeMillis()

                    val microAmps = batteryManager.getIntProperty(
                        BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    phoneMa = abs(microAmps / 1000)
                    phoneLevel = batteryManager.getIntProperty(
                        BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    val status = batteryManager.getIntProperty(
                        BatteryManager.BATTERY_PROPERTY_STATUS)
                    isPhoneCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL)

                    delay(1000)
                }
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // --- Watch複数台 ---
                        items(watchDataMap.values.toList()) { watch ->
                            val isSyncing = (currentTimeMillis - watch.lastSyncMillis) < 30000
                                    && watch.lastSyncMillis != 0L
                            WatchCard(watch, isSyncing)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Watchが1台も繋がっていない場合
                        if (watchDataMap.isEmpty()) {
                            item {
                                Text(
                                    "Watchを探しています...",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                thickness = 1.dp,
                                color = Color.LightGray
                            )
                        }

                        // --- Phone セクション ---
                        item {
                            PhoneCard(phoneMa, phoneLevel, isPhoneCharging)
                        }
                    }
                }
            }
        }
    }

    private fun sendSignalToWatch(path: String) {
        val nodeClient = Wearable.getNodeClient(this)
        val messageClient = Wearable.getMessageClient(this)

        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                Log.w("PhoneMain", "接続中のWatchなし: $path 送信スキップ")
                return@addOnSuccessListener
            }
            for (node in nodes) {
                messageClient.sendMessage(node.id, path, null)
                    .addOnSuccessListener {
                        Log.d("PhoneMain", "$path → ${node.displayName} 送信成功")
                    }
                    .addOnFailureListener { e ->
                        Log.e("PhoneMain", "$path 送信失敗", e)
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        Handler(Looper.getMainLooper()).postDelayed({
            sendSignalToWatch("/start_sync")
        }, 500)
    }

    override fun onPause() {
        super.onPause()
        sendSignalToWatch("/stop_sync")
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path?.startsWith("/battery_status/") == true) {

                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val watchId = dataMap.getString("watch_id") ?: "Unknown"
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

                watchDataMap[watchId] = WatchData(
                    watchId      = watchId,
                    currentMa    = dataMap.getInt("current_ma"),
                    batteryLevel = dataMap.getInt("level"),
                    isCharging   = dataMap.getBoolean("is_charging"),
                    lastSyncMillis = System.currentTimeMillis(),
                    lastSyncTime = sdf.format(Date())
                )

                Log.d("PhoneMain",
                    "[$watchId] ${dataMap.getInt("current_ma")}mA " +
                            "${dataMap.getInt("level")}% " +
                            "charging=${dataMap.getBoolean("is_charging")}")
            }
        }
    }
}

// --- Composable ---

@Composable
fun WatchCard(watch: WatchData, isSyncing: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = watch.watchId,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isSyncing) "● SYNCING" else "○ DISCONNECTED",
                color = if (isSyncing) Color(0xFF4CAF50) else Color.Red,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${watch.batteryLevel}%",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${watch.currentMa}mA",
                fontSize = 24.sp,
                color = if (watch.isCharging) Color(0xFF4285F4) else Color(0xFFEA4335)
            )
            Text(
                text = if (watch.isCharging) "▲ CHARGING" else "▼ DISCHARGING",
                fontSize = 12.sp,
                color = if (watch.isCharging) Color(0xFF4285F4) else Color(0xFFEA4335)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Last Sync: ${watch.lastSyncTime}",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun PhoneCard(phoneMa: Int, phoneLevel: Int, isPhoneCharging: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "PHONE MONITOR",
            fontSize = 12.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$phoneLevel%",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${phoneMa}mA",
            fontSize = 24.sp,
            color = if (isPhoneCharging) Color(0xFF4285F4) else Color(0xFFEA4335)
        )
        Text(
            text = if (isPhoneCharging) "▲ CHARGING" else "▼ DISCHARGING",
            fontSize = 12.sp,
            color = if (isPhoneCharging) Color(0xFF4285F4) else Color(0xFFEA4335)
        )
    }
}