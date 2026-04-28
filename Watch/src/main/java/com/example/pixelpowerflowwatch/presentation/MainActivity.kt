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
import androidx.compose.ui.text.style.TextAlign

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

    // --- フォントサイズ計算 ---
    val largeFontSize = (screenWidth * 0.13).sp
    val smallFontSize = (screenWidth * 0.07).sp

    var showSettings by remember { mutableStateOf(false) }
    var brightnessValue by remember { mutableFloatStateOf(0.05f) }

    var currentMaDisplay by remember { mutableIntStateOf(0) }
    var isCharging by remember { mutableStateOf(false) }
    var batteryLevel by remember { mutableIntStateOf(0) }
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    val customBlue = Color(0xFF3460FB)

    LaunchedEffect(Unit) {
        val dateFormat = SimpleDateFormat("MM/dd(E)", Locale.ENGLISH)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        while (true) {
            val now = Date()
            val microAmps = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            currentMaDisplay = abs(microAmps / 1000)

            val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL)

            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)

            // --- スマホ側へのデータ送信 (成功済みロジック) ---
            val putDataReq: PutDataRequest = PutDataMapRequest.create("/battery_status").run {
                dataMap.putInt("current_ma", currentMaDisplay)
                dataMap.putInt("level", batteryLevel)
                dataMap.putBoolean("is_charging", isCharging)
                dataMap.putLong("timestamp", System.currentTimeMillis())
                asPutDataRequest()
            }.setUrgent()

            dataClient.putDataItem(putDataReq)
                .addOnSuccessListener { Log.d("BatterySync", "Watch: 送信成功！") }
                .addOnFailureListener { e -> Log.e("BatterySync", "Watch: 送信失敗", e) }

            delay(1000)
        }
    }

    val maColor = if (isCharging) customBlue else Color.Red
    val batteryColor = when {
        batteryLevel < 15 -> Color.Red
        batteryLevel < 30 -> Color.Yellow
        batteryLevel < 60 -> Color.White
        batteryLevel < 75 -> Color.Cyan
        else -> Color.Green
    }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { showSettings = true },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                // 1. 日付(曜日)
                Text(
                    text = currentDate,
                    fontSize = smallFontSize,
                    color = Color.White,
                    textAlign = TextAlign.Center // シンプルに指定
                )

                // 2. 時刻
                Text(
                    text = currentTime,
                    fontSize = largeFontSize,
                    color = Color.Yellow,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 3. バッテリー残量 ＋ 充放電電流値
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "$batteryLevel%", fontSize = largeFontSize, color = batteryColor)
                    Text(
                        text = if (isCharging) " ▲ " else " ▼ ",
                        fontSize = smallFontSize,
                        color = maColor
                    )
                    Text(text = "${currentMaDisplay}mA", fontSize = largeFontSize, color = maColor)
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 4. 状態テキスト
                Text(
                    text = if (isCharging) "CHARGING" else "DISCHARGING",
                    fontSize = smallFontSize,
                    color = maColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // 設定ダイアログ
    if (showSettings) {
        Dialog(showDialog = showSettings, onDismissRequest = { showSettings = false }) {
            val listState = rememberScalingLazyListState()
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().background(Color.Black)
            ) {
                item { Text("Settings", fontSize = 14.sp, color = Color.White) }
                item {
                    CompactButton(
                        onClick = {
                            brightnessValue = (brightnessValue + 0.2f).let { if (it > 1f) 0.05f else it }
                            activity.updateBrightness(brightnessValue)
                        }
                    ) { Text("Brightness Loop") }
                }
                item { CompactButton(onClick = { showSettings = false }) { Text("OK") } }
            }
        }
    }
}