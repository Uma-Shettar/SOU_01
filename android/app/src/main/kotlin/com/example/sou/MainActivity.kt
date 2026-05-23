package com.example.sou

import android.database.Cursor
import android.net.Uri
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val CHANNEL = "sms_reader"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->

                if (call.method == "getSMS") {
                    try {
                        val lastTimestamp = call.argument<String>("lastId")?.toLongOrNull()
                        val now = System.currentTimeMillis()
                        val smsList = mutableListOf<Map<String, Any>>()
                        val seenIds = mutableSetOf<String>()

                        val uris = listOf(
                            "content://sms/inbox",
                            "content://sms",
                            "content://mms-sms/conversations",
                        )

                        val selection: String
                        val selectionArgs: Array<String>

                        if (lastTimestamp != null && lastTimestamp > 0L) {
                            selection = "date > ? AND date <= ?"
                            selectionArgs = arrayOf(
                                lastTimestamp.toString(),
                                now.toString()
                            )
                        } else {
                            // Init: last 24 hours
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

                                    // skip if required columns missing (e.g. mms-sms schema differs)
                                    if (idIndex < 0 || dateIndex < 0) {
                                        android.util.Log.w("SMS_DEBUG",
                                            "$uriString → missing columns, skipping")
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

                                android.util.Log.d("SMS_DEBUG",
                                    "$uriString → $uriCount new rows")

                            } catch (e: Exception) {
                                android.util.Log.e("SMS_DEBUG",
                                    "$uriString → error: ${e.message}")
                            }
                        }

                        android.util.Log.d("SMS_DEBUG", "Total returning: ${smsList.size}")
                        for (sms in smsList) {
                            android.util.Log.d("SMS_DEBUG",
                                "  id=${sms["id"]} date=${sms["date"]} sender=${sms["sender"]} type=${sms["type"]}")
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
}