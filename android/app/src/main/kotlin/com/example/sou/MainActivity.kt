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
                    val smsList = getSMS()
                    result.success(smsList)
                } else {
                    result.notImplemented()
                }
            }
    }

    private fun getSMS(): List<Map<String, String>> {
        val smsList = mutableListOf<Map<String, String>>()

        try {
            val uri = Uri.parse("content://sms/inbox")

            val cursor = contentResolver.query(
                uri,
                null,
                null,
                null,
                "date DESC"
            )

            if (cursor != null) {
                val addressIndex = cursor.getColumnIndex("address")
                val bodyIndex = cursor.getColumnIndex("body")
                val dateIndex = cursor.getColumnIndex("date")

                if (addressIndex == -1 || bodyIndex == -1 || dateIndex == -1) {
                    cursor.close()
                    return smsList
                }

                val currentTime = System.currentTimeMillis()

                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addressIndex)
                    val body = cursor.getString(bodyIndex)
                    val time = cursor.getLong(dateIndex)

                    //if (currentTime - time <= 2 * 60 * 1000) {
                        android.util.Log.e("SMS_READER", "INSTANT: $sender | $body")
                        smsList.add(
                            mapOf(
                                "sender" to sender,
                                "body" to body
                            )
                        )
                    //}
                }

                cursor.close()
            }

        } catch (e: Exception) {
            android.util.Log.e("SMS_READER", "ERROR: ${e.message}")
        }

        return smsList
    }
}