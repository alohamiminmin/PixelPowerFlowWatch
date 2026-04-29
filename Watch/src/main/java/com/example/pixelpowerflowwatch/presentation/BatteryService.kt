package com.example.pixelpowerflowwatch.presentation

import android.app.*
import android.content.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.*
import com.example.pixelpowerflowwatch.R
import kotlin.math.abs

class BatteryService : Service(), MessageClient.OnMessageReceivedListener {
    private val handler = Handler(Looper.getMainLooper())
    private var isPhoneActive = false
    private var wakeLock: PowerManager.WakeLock? = null

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (isPhoneActive) {
                val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                // ★ abs()追加（元コードは符号付きのまま送信していた）
                val ma = abs(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000)
                val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                // monitorRunnable内のputDataReq部分を修正
                val watchId = android.os.Build.MODEL
                    .replace(" ", "_")  // スペースをアンダースコアに

                val putDataReq = PutDataMapRequest.create("/battery_status/$watchId").run {
                    dataMap.putInt("current_ma", ma)
                    dataMap.putInt("level", level)
                    dataMap.putBoolean("is_charging", isCharging)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                    dataMap.putString("watch_id", watchId)  // ★ 識別用に名前も入れる
                    asPutDataRequest()
                }.setUrgent()

                Wearable.getDataClient(applicationContext).putDataItem(putDataReq)
                    .addOnSuccessListener { Log.d("BatteryService", "送信成功: ${ma}mA") }
                    .addOnFailureListener { e -> Log.e("BatteryService", "送信失敗", e) }

                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Wearable.getMessageClient(this).addListener(this)
        createNotificationChannel()
        Log.d("BatteryService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "battery_channel")
            .setContentTitle("Battery Sync Mode")
            .setContentText("Waiting for phone...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        startForeground(1, notification,
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        return START_STICKY
    }

    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            "/start_sync" -> {
                if (!isPhoneActive) {
                    isPhoneActive = true
                    acquireWakeLock()
                    handler.post(monitorRunnable)
                    Log.d("BatteryService", "Sync Started")
                }
            }
            "/stop_sync" -> {
                isPhoneActive = false
                handler.removeCallbacks(monitorRunnable)
                releaseWakeLock()
                Log.d("BatteryService", "Sync Stopped")
            }
        }
    }

    private fun acquireWakeLock() {
        releaseWakeLock() // 二重取得防止
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PixelPowerFlow::BatteryLock"
        ).apply {
            // ★ 時間制限なし（Phone側が /stop_sync を送るまで保持）
            acquire()
        }
        Log.d("BatteryService", "WakeLock acquired")
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            Log.d("BatteryService", "WakeLock released")
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "battery_channel", "Battery Sync", NotificationManager.IMPORTANCE_MIN
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        isPhoneActive = false
        handler.removeCallbacks(monitorRunnable)
        Wearable.getMessageClient(this).removeListener(this)
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}