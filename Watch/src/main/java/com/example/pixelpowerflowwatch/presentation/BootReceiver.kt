package com.example.pixelpowerflowwatch.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

// ★ 端末再起動後、手動でアプリを開かなくてもBatteryServiceを起動し直し、
//    phone側からの次の /start_sync を受け取れる状態に戻す
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "起動完了を検知、BatteryServiceを起動します")
            context.startForegroundService(Intent(context, BatteryService::class.java))
        }
    }
}
