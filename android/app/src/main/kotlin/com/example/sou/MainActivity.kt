package com.example.sou

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
                    val lastId = call.argument<String>("lastId")
                    val smsList = getSMS(lastId)
                    result.success(smsList)
                } else {
                    result.notImplemented()
                }
            }
    }

    private fun getSMS(lastId: String?): List<Map<String, String>> {
        val smsList = mutableListOf<Map<String, String>>()
        try {
            val uri = Uri.parse("content://sms/inbox")

            val cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("_id", "address", "body", "date", "sub_id"),
                null,  // no sub_id filter
                null,
                "date DESC"
            )

            if (cursor != null) {
                val idIndex = cursor.getColumnIndex("_id")
                val addressIndex = cursor.getColumnIndex("address")
                val bodyIndex = cursor.getColumnIndex("body")
                val dateIndex = cursor.getColumnIndex("date")

                var count = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIndex)
                    val sender = cursor.getString(addressIndex) ?: "Unknown"
                    val body = cursor.getString(bodyIndex) ?: ""
                    val date = cursor.getLong(dateIndex)

                    smsList.add(mapOf(
                        "id" to id,
                        "sender" to sender,
                        "body" to body,
                        "date" to date.toString()
                    ))
                }
                cursor.close()
            }

        } catch (e: Exception) {
            android.util.Log.e("SMS_READER", "ERROR: ${e.message}")
        }
        return smsList
    }
}