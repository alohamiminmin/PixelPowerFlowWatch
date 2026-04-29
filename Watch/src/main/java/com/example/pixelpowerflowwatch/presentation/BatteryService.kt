package com.example.pixelpowerflowwatch.presentation

import android.app.*
import android.content.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.*
import com.example.pixelpowerflowwatch.R

class BatteryService : Service(), MessageClient.OnMessageReceivedListener {
    private val handler = Handler(Looper.getMainLooper())
    private var isPhoneActive = false
    private var wakeLock: PowerManager.WakeLock? = null

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (isPhoneActive) {
                val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val ma = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000
                val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                val putDataReq = PutDataMapRequest.create("/battery_status").run {
                    dataMap.putInt("current_ma", ma)
                    dataMap.putInt("level", level)
                    dataMap.putBoolean("is_charging", isCharging)
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                    asPutDataRequest()
                }
                Wearable.getDataClient(applicationContext).putDataItem(putDataReq.setUrgent())
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Wearable.getMessageClient(this).addListener(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "battery_channel")
            .setContentTitle("Battery Sync Mode")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

        startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        return START_STICKY
    }

    // --- ここがエラー(image_4bc9d4)の修正ポイント ---
    override fun onMessageReceived(event: MessageEvent) {
        when (event.path) {
            "/start_sync" -> {
                if (!isPhoneActive) {
                    acquireWakeLock() // 暗転対策：CPUを起こす
                    isPhoneActive = true
                    handler.post(monitorRunnable)
                    Log.d("BatteryService", "Sync Started")
                }
            }
            "/stop_sync" -> {
                isPhoneActive = false
                handler.removeCallbacks(monitorRunnable)
                releaseWakeLock() // CPUを眠らせる
                Log.d("BatteryService", "Sync Stopped")
            }
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BatteryMonitor::Lock").apply {
            acquire(10 * 60 * 1000L) // 10分間有効
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel("battery_channel", "Battery Sync", NotificationManager.IMPORTANCE_MIN)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        Wearable.getMessageClient(this).removeListener(this)
        handler.removeCallbacks(monitorRunnable)
        releaseWakeLock()
        super.onDestroy()
    }

    // --- この関数がないとエラーになります ---
    override fun onBind(intent: Intent?): IBinder? = null
}