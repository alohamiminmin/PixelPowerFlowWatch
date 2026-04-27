package com.example.pixelpowerflowwatch.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.pixelpowerflowwatch.R
import com.google.android.gms.wearable.Wearable

class BatteryService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val interval = 2000L // 2秒ごとに送信

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        startMonitoring()
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, "battery_channel")
            .setContentTitle("Battery Monitoring")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    private fun startMonitoring() {
        handler.post(object : Runnable {
            override fun run() {
                val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
                val currentMicroA = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                val currentMilliA = currentMicroA / 1000f

                sendCurrent(currentMilliA)

                handler.postDelayed(this, interval)
            }
        })
    }

    private fun sendCurrent(current: Float) {
        val nodeClient = Wearable.getNodeClient(this)
        val messageClient = Wearable.getMessageClient(this)

        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                messageClient.sendMessage(
                    node.id,
                    "/current",
                    current.toString().toByteArray()
                )
            }
        }
    }

    override fun onBind(intent: Intent?) = null
}
