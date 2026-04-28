package com.example.pixelpowerflowwatch.presentation

import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.wear.compose.material3.Text
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() {

    private val chargingReceiver = ChargingReceiver()

    private lateinit var dataClient: DataClient

     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         // 通信クライアントを初期化
         dataClient = Wearable.getDataClient(this)

         // BATTERY_CHANGED を動的登録（Manifest では受け取れない）
         val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
         registerReceiver(chargingReceiver, filter)

         // Activity が前面にあるので Foreground Service 起動が許可される
         val serviceIntent = Intent(this, BatteryService::class.java)
         startForegroundService(serviceIntent)

         setContent {
             Text("Pixel Power Flow Watch")
         }
     }
    private fun sendBatteryDataToPhone(ma: Int, level: Int, charging: Boolean) {
        val putDataReq: PutDataRequest = PutDataMapRequest.create("/battery_status").run {
            dataMap.putInt("current_ma", ma)
            dataMap.putInt("level", level)
            dataMap.putBoolean("is_charging", charging)
            dataMap.putLong("timestamp", System.currentTimeMillis())
            asPutDataRequest()
        }
             // 非同期で送信。スマートフォンのアプリがこれを受け取ります
             dataClient.putDataItem(putDataReq)
         }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(chargingReceiver)
    }
}
