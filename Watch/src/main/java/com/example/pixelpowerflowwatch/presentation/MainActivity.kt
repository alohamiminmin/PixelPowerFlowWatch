package com.example.pixelpowerflowwatch.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Dialog
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import android.util.Log

// レシーバー
class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_POWER_CONNECTED) {
            val i = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "power_flow_ch"
            val channel = NotificationChannel(channelId, "Power Flow", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)

            val pendingIntent = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setContentTitle("Charging Started")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1, notification)
            try { context.startActivity(i) } catch (e: Exception) {}
        }
    }
}

class MainActivity : ComponentActivity() {
    private val chargerReceiver = PowerConnectionReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        registerReceiver(chargerReceiver, IntentFilter(Intent.ACTION_POWER_CONNECTED))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { ChargingMonitorApp(this) }
    }

    fun updateBrightness(brightness: Float) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness.coerceIn(0.01f, 1.0f)
        window.attributes = layoutParams
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(chargerReceiver) } catch (e: Exception) {}
    }
}

@Composable
fun ChargingMonitorApp(activity: MainActivity) {
    val context = activity as Context
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val dataClient = remember { Wearable.getDataClient(context) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    var showSettings by remember { mutableStateOf(false) }
    var brightnessValue by remember { mutableFloatStateOf(0.05f) }
    var currentMaDisplay by remember { mutableIntStateOf(0) }
    var isCharging by remember { mutableStateOf(false) }
    var batteryLevel by remember { mutableIntStateOf(0) }
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MM/dd(E)", Locale.ENGLISH)
        while (true) {
            val now = Date()
            val microAmps = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

            android.util.Log.d("BatteryCheck", "取得した生の値(microAmps): $microAmps")

            currentMaDisplay = abs(microAmps / 1000)

            android.util.Log.d("BatteryCheck", "表示用の値(mA): $currentMaDisplay")

            val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)

// Watch側の送信処理部分
            val putDataReq: PutDataRequest = PutDataMapRequest.create("/battery_status").run {
                dataMap.putInt("current_ma", currentMaDisplay)
                dataMap.putInt("level", batteryLevel)
                dataMap.putBoolean("is_charging", isCharging)

                // 毎回違う値を入れて強制同期させる
                dataMap.putLong("timestamp", System.currentTimeMillis())

                // 1. まず PutDataRequest 型に変換
                asPutDataRequest()
            }.setUrgent() // 2. その後で最優先設定をかける（ここがポイント）

// 送信実行
            dataClient.putDataItem(putDataReq)
                .addOnSuccessListener {
                    Log.d("BatterySync", "Watch: 送信成功！")
                }
                .addOnFailureListener { e ->
                    Log.e("BatterySync", "Watch: 送信失敗...", e)
                }
            dataClient.putDataItem(putDataReq)
                .addOnFailureListener { e -> Log.e("BatteryLog", "送信失敗", e) }
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. 日付 (曜日)
            // 日付取得時に "yyyy/MM/dd (E)" などのフォーマットを使用している前提です
            Text(
                text = currentDate,
                color = Color.Gray,
                fontSize = (screenWidth * 0.07).sp,
                style = MaterialTheme.typography.caption1
            )

            // 2. 時間
            Text(
                text = currentTime,
                color = Color.Yellow,
                fontSize = (screenWidth * 0.16).sp,
                style = MaterialTheme.typography.display1
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. バッテリー残量 / 充放電流値
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$batteryLevel%",
                    color = Color.White,
                    fontSize = (screenWidth * 0.1).sp,
                    style = MaterialTheme.typography.body1
                )
                Text(
                    text = " / ",
                    color = Color.Gray,
                    fontSize = (screenWidth * 0.08).sp
                )
                Text(
                    text = "${currentMaDisplay}mA",
                    color = if (isCharging) Color.Green else Color.Red,
                    fontSize = (screenWidth * 0.1).sp,
                    style = MaterialTheme.typography.body1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4. CHARGE / DISCHARGE 表記
            Text(
                text = if (isCharging) "CHARGE" else "DISCHARGE",
                color = if (isCharging) Color.Green else Color.LightGray,
                fontSize = (screenWidth * 0.08).sp,
                style = MaterialTheme.typography.button
            )
        }
    }

    if (showSettings) {
        Dialog(showDialog = showSettings, onDismissRequest = { showSettings = false }) {
            ScalingLazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                item { Text("Settings", fontSize = 14.sp) }
                item {
                    CompactButton(onClick = { brightnessValue = (brightnessValue + 0.1f).coerceAtMost(1f); activity.updateBrightness(brightnessValue) }) { Text("Brightness +") }
                }
                item { CompactButton(onClick = { showSettings = false }) { Text("OK") } }
            }
        }
    }
}