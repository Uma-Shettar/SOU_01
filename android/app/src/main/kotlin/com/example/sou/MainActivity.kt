package com.example.sou

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.sou/sms"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                if (call.method == "startSmsReceiver") {
                    val apiUrl = call.argument<String>("apiUrl") ?: ""
                    SmsReceiver.apiUrl = apiUrl
                    result.success(null)
                } else {
                    result.notImplemented()
                }
            }
    }
}