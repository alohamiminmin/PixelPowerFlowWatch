package com.example.pixelpowerflowwatch.presentation

import android.content.Context
import com.google.android.gms.wearable.Wearable

object CurrentSender {

    fun sendCurrent(context: Context, current: Float) {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                for (node in nodes) {
                    Wearable.getMessageClient(context)
                        .sendMessage(
                            node.id,
                            "/current",
                            current.toString().toByteArray()
                        )
                }
            }
    }
}
