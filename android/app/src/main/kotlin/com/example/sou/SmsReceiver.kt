package com.example.sou

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SmsReceiver : BroadcastReceiver() {
    companion object {
        var apiUrl: String = ""  // set from MainActivity via MethodChannel
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Combine multi-part messages
        val sender = messages[0].originatingAddress ?: "Unknown"
        val body = messages.joinToString("") { it.messageBody }
        val date = System.currentTimeMillis()

        android.util.Log.d("SmsReceiver", "New SMS from $sender: $body")

        // POST immediately in background thread
        Thread {
            postSmsToApi(sender, body, date)
        }.start()
    }

    // private fun postSmsToApi(sender: String, body: String, date: Long) {
    //     try {
    //         val payload = JSONObject()
    //         payload.put("sender",      sender)
    //         payload.put("body",        body)
    //         payload.put("date",        date)
    //         payload.put("device_time", System.currentTimeMillis())

    //         val url = URL(apiUrl)
    //         android.util.Log.d("SmsReceiver", "apiUrl = '$apiUrl'")
    //         val conn = url.openConnection() as HttpURLConnection
    //         conn.requestMethod = "POST"
    //         conn.setRequestProperty("Content-Type", "application/json")
    //         conn.setRequestProperty("Accept", "application/json")
    //         conn.doOutput = true
    //         conn.connectTimeout = 10_000
    //         conn.readTimeout = 10_000

    //         OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
    //             writer.write(payload.toString())
    //             writer.flush()
    //         }

    //         android.util.Log.d("SmsReceiver", "Posted → ${conn.responseCode}")
    //         conn.disconnect()

    //     } catch (e: Exception) {
    //         android.util.Log.e("SmsReceiver", "POST failed: ${e.message}")
    //     }
    // }

    private fun postSmsToApi(sender: String, body: String, date: Long) {
        try {
            android.util.Log.d("SmsReceiver", "apiUrl = $apiUrl")
            android.util.Log.d("SmsReceiver", "apiUrl length = ${apiUrl.length}")

            val url = URL(apiUrl)

            android.util.Log.d("SmsReceiver", "Parsed URL = $url")

            val payload = JSONObject()
            payload.put("sender", sender)
            payload.put("body", body)
            payload.put("date", date)
            payload.put("device_time", System.currentTimeMillis())

            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            android.util.Log.d("SmsReceiver", "Posted → ${conn.responseCode}")
            conn.disconnect()

        } catch (e: Exception) {
            android.util.Log.e(
                "SmsReceiver",
                "POST failed. apiUrl='$apiUrl', error=${e.message}",
                e
            )
        }
    }


}