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
            // ★ Android 12+ はBOOT_COMPLETED直後にstartForegroundService()が不可で
            //   ForegroundServiceStartNotAllowedExceptionでクラッシュするため、
            //   失敗したら通常のstartServiceにフォールバックする
            //   (通常startServiceは起動直後の一時的なバックグラウンド起動許可内で動く)
            try {
                context.startForegroundService(Intent(context, BatteryService::class.java))
            } catch (e: Exception) {
                Log.w("BootReceiver", "FGS開始不可のため通常startServiceで起動", e)
                try {
                    context.startService(Intent(context, BatteryService::class.java))
                } catch (e2: Exception) {
                    Log.e("BootReceiver", "startServiceも失敗", e2)
                }
            }
        }
    }
}
