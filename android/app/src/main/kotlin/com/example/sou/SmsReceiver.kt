package com.example.sou

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import io.flutter.plugin.common.EventChannel

class SmsReceiver : BroadcastReceiver() {

    companion object {
        var eventSink: EventChannel.EventSink? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.e("SMS_LIVE", "onReceive triggered! action=${intent.action}")  // ← add this

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            android.util.Log.e("SMS_LIVE", "SMS_RECEIVED action matched!")  // ← add this
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            android.util.Log.e("SMS_LIVE", "Messages count: ${messages.size}")  // ← add this

            for (message in messages) {
                val sender = message.originatingAddress ?: "Unknown"
                val body = message.messageBody ?: ""

                android.util.Log.e("SMS_LIVE", "New SMS: $sender | $body")

                eventSink?.success(
                    mapOf(
                        "sender" to sender,
                        "body" to body,
                        "date" to System.currentTimeMillis().toString()
                    )
                )
            }
        }
    }
}