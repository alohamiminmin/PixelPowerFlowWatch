package com.example.pixelpowerflowwatch.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log

class ChargingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        Log.d("ChargingReceiver", "onReceive called")

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING

        Log.d("ChargingReceiver", "charging = $charging")

        // 充電状態だけ更新（サービスは起動しない）
        BatteryServiceHolder.isCharging = charging
    }
}

object BatteryServiceHolder {
    var isCharging = false
}
