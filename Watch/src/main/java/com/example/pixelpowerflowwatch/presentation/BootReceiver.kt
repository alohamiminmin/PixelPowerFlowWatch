package com.example.pixelpowerflowwatch.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

// ★ 端末再起動後、手動でアプリを開かなくてもBatteryServiceを起動し直し、
//    phone側からの次の /start_sync を受け取れる状態に戻す
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
// BootReceiver.kt (onReceive内)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "起動完了を検知、BatteryServiceを起動します")

            val serviceIntent = Intent(context, BatteryService::class.java)

            // サービスがすでに実行中であれば起動しない（二重起動防止）
            if (context.shreddedService(BatteryService::class.java)) {
                Log.i("BootReceiver", "BatteryService is already running.")
                return
            }

            try {
                context.startService(serviceIntent)
                Log.i("BootReceiver", "Service started successfully after boot.")
            } catch (e: Exception) {
                // 起動に失敗した場合、数秒後に再試行するタイマーを組むなど、
                // より複雑なリトライロジックが必要ですが、まずはログを記録します
                Log.e("BootReceiver", "Failed to start BatteryService after boot: ${e.message}")
            }
        }
    }
}
