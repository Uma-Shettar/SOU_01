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
    return MaterialApp(home: SmsPage());
  }
}

class SmsPage extends StatefulWidget {
  @override
  State<SmsPage> createState() => _SmsPageState();
}

class _SmsPageState extends State<SmsPage> {
  static const platform = MethodChannel('sms_reader');

  List<Map<String, dynamic>> smsList = [];
  Timer? _timer;
  String? _lastId; // track last seen SMS id

  @override
  void initState() {
    super.initState();
    _initSms();
  }

  Future<void> _initSms() async {
    final status = await Permission.sms.request();
    debugPrint("SMS permission: $status");

    if (status.isGranted) {
      // Get initial last ID without showing history
      await _initLastId();
      // Start polling every 3 seconds
      _timer = Timer.periodic(const Duration(seconds: 3), (_) {
        debugPrint("Polling... lastId: $_lastId");  
        _checkNewSms();
      });
    }
  }
  String _lastTimestamp = "0";

  Future<void> _initLastId() async {
    // Subtract 5 minutes to catch SMS that arrived just before app started
    _lastTimestamp = (DateTime.now().millisecondsSinceEpoch - (5 * 60 * 1000)).toString();
    debugPrint("Watching SMS from timestamp: $_lastTimestamp");
  }


  Future<void> _checkNewSms() async {
    try {
      final List result = await platform.invokeMethod('getSMS', {"lastId": null});

      for (final sms in result) {
        debugPrint("SMS id:${sms['id']} date:${sms['date']} sender:${sms['sender']}");
      }

      final newSms = result.where((sms) {
        final date = int.tryParse(sms['date'] ?? '0') ?? 0;
        return date > int.parse(_lastTimestamp);
      }).toList();

      debugPrint("Polling... total: ${result.length}, new: ${newSms.length}");

      if (newSms.isNotEmpty) {
        setState(() {
          for (final sms in newSms) {
            smsList.insert(0, Map<String, dynamic>.from(sms));
          }
        });
        _lastTimestamp = newSms.first['date'];
      }
    } catch (e) {
      debugPrint("Error: $e");
    }
  }
  String _formatDate(String dateStr) {
    try {
      final date = DateTime.fromMillisecondsSinceEpoch(int.parse(dateStr));
      return "${date.day}/${date.month}/${date.year} "
          "${date.hour}:${date.minute.toString().padLeft(2, '0')}";
    } catch (_) {
      return '';
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
        title: const Text("Live SMS"),
        actions: [
          // Live indicator
          Padding(
            padding: const EdgeInsets.all(12.0),
            child: Row(
              children: [
                const Icon(Icons.circle, size: 10, color: Colors.green),
                const SizedBox(width: 4),
                const Text("Live", style: TextStyle(fontSize: 12)),
              ],
            ),
          ),
          IconButton(
            icon: const Icon(Icons.delete_sweep),
            onPressed: () => setState(() => smsList.clear()),
          ),
        ],
      ),
      body: smsList.isEmpty
          ? const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.sms, size: 64, color: Colors.grey),
                  SizedBox(height: 16),
                  Text(
                    "Waiting for new SMS...",
                    style: TextStyle(color: Colors.grey, fontSize: 16),
                  ),
                  SizedBox(height: 8),
                  Text(
                    "Checking every 3 seconds",
                    style: TextStyle(color: Colors.grey, fontSize: 12),
                  ),
                ],
              ),
            )
          : ListView.separated(
              itemCount: smsList.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (context, index) {
                final sms = smsList[index];
                return ListTile(
                  leading: CircleAvatar(
                    backgroundColor: Colors.blue.shade100,
                    child: Text(
                      (sms['sender'] ?? '?')
                          .toString()
                          .substring(0, 1)
                          .toUpperCase(),
                      style: const TextStyle(color: Colors.blue),
                    ),
                  ),
                  title: Text(
                    sms['sender'] ?? '',
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  subtitle: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(sms['body'] ?? ''),
                      Text(
                        _formatDate(sms['date'] ?? ''),
                        style: const TextStyle(
                            fontSize: 11, color: Colors.grey),
                      ),
                    ],
                  ),
                  isThreeLine: true,
                );
              },
            ),
    );
  }
}