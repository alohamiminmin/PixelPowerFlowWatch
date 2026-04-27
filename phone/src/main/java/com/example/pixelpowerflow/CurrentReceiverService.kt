package com.example.pixelpowerflow

import com.google.android.gms.wearable.WearableListenerService
import com.google.android.gms.wearable.MessageEvent
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase


class CurrentReceiverService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/current") {
            val current = String(messageEvent.data).toFloat()
            sendToFirestore(current)
        }
    }

    private fun sendToFirestore(current: Float) {
        val data = hashMapOf(
            "current" to current,
            "timestamp" to System.currentTimeMillis()
        )

        Firebase.firestore
            .collection("pixel_power_flow")
            .add(data)
    }
}
