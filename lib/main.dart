import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: SmsPage(),
    );
  }
}

class SmsPage extends StatefulWidget {
  @override
  State<SmsPage> createState() => _SmsPageState();
}

class _SmsPageState extends State<SmsPage> {
  static const platform = MethodChannel('sms_reader');

  List smsList = [];
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _initSms();
  }

  Future<void> _initSms() async {
    final status = await Permission.sms.request();
    debugPrint("SMS Permission status: $status");  // ← add this

    if (status.isGranted) {
      debugPrint("Permission granted, fetching SMS...");  // ← add this
      await getSms();
      _timer = Timer.periodic(const Duration(seconds: 30), (_) => getSms());
    } else {
      debugPrint("SMS permission denied: $status");  // ← add this
    }
  }

  Future<void> getSms() async {
    try {
      debugPrint("Calling getSMS on platform channel...");  // ← add this
      final List result = await platform.invokeMethod('getSMS');
      debugPrint("SMS fetched: ${result.length} messages");  // ← add this
      debugPrint("Raw result: $result");                      // ← add this
      setState(() {
        smsList = result;
      });
    } catch (e) {
      debugPrint("Error fetching SMS: $e");  // ← add this
    }
  }
  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("SMS Reader"),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: getSms,
          ),
        ],
      ),
      body: smsList.isEmpty
          ? const Center(child: Text("No SMS in the last 2 minutes"))
          : ListView.builder(
              itemCount: smsList.length,
              itemBuilder: (context, index) {
                final sms = smsList[index];
                return ListTile(
                  leading: const Icon(Icons.sms),
                  title: Text(sms['sender'] ?? ''),
                  subtitle: Text(sms['body'] ?? ''),
                );
              },
            ),
    );
  }
}