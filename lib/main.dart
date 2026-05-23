import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'dart:ui';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  PlatformDispatcher.instance.onError = (error, stack) {
    debugPrint("GLOBAL ERROR: $error");
    return true;
  };
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});
  @override
  Widget build(BuildContext context) => const MaterialApp(home: SmsPage());
}

class SmsPage extends StatefulWidget {
  const SmsPage({super.key});
  @override
  State<SmsPage> createState() => _SmsPageState();
}

class _SmsPageState extends State<SmsPage> {
  static const platform = MethodChannel('sms_reader');

  List<Map<String, dynamic>> smsList = [];
  Timer? _timer;
  String _lastDate = "0";
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    final smsStatus = await Permission.sms.request();
    final phoneStatus = await Permission.phone.request();

    if (!smsStatus.isGranted || !phoneStatus.isGranted) {
      await openAppSettings();
      return;
    }

    await _initLastDate();

    _timer = Timer.periodic(const Duration(seconds: 2), (_) => _checkNewSms());
  }

  Future<void> _initLastDate() async {
    try {
      // Pass null so Kotlin uses the 24h init branch
      final List result = await platform.invokeMethod('getSMS', {"lastId": null});
      if (result.isEmpty) {
        // No SMS in last 24h — anchor to now so we only get future messages
        _lastDate = DateTime.now().millisecondsSinceEpoch.toString();
        return;
      }

      final sms = result.map((e) => Map<String, dynamic>.from(e)).toList();
      sms.sort((a, b) => int.parse(b['date']).compareTo(int.parse(a['date'])));

      _lastDate = sms.first['date'].toString();
      debugPrint("Initialized _lastDate to: $_lastDate");

      // Show initial SMS in UI
      if (mounted) setState(() => smsList = sms);
    } catch (e) {
      debugPrint("INIT ERROR: $e");
      _lastDate = DateTime.now().millisecondsSinceEpoch.toString();
    }
  }

  Future<void> _checkNewSms() async {
    if (_loading) return;
    _loading = true;

    try {
      debugPrint("Polling since: $_lastDate");
      final List result = await platform.invokeMethod('getSMS', {"lastId": _lastDate});

      if (result.isNotEmpty) {
        final sms = result.map((e) => Map<String, dynamic>.from(e)).toList();
        sms.sort((a, b) => int.parse(b['date']).compareTo(int.parse(a['date'])));

        // Advance the cursor to the latest seen date
        final latestDate = int.parse(sms.first['date'].toString());
        final currentLast = int.parse(_lastDate);
        if (latestDate > currentLast) {
          _lastDate = latestDate.toString();
        }

        if (mounted) {
          setState(() {
            for (final sm in sms) {
              final alreadyExists = smsList.any((s) => s['id'] == sm['id']);
              if (!alreadyExists) smsList.insert(0, sm);
            }
          });
        }

        debugPrint("Added ${sms.length} new messages, cursor now: $_lastDate");
      }
    } catch (e) {
      debugPrint("POLL ERROR: $e");
    } finally {
      // finally guarantees _loading resets even if an exception occurs
      _loading = false;
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  String _format(String ts) {
    try {
      final d = DateTime.fromMillisecondsSinceEpoch(int.parse(ts));
      return "${d.day}/${d.month}/${d.year} ${d.hour}:${d.minute.toString().padLeft(2, '0')}";
    } catch (_) {
      return "";
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("SMS Reader"),
        actions: [
          IconButton(
            icon: const Icon(Icons.delete),
            onPressed: () => setState(() => smsList.clear()),
          ),
        ],
      ),
      body: smsList.isEmpty
          ? const Center(child: Text("Waiting for SMS..."))
          : ListView.builder(
              itemCount: smsList.length,
              itemBuilder: (context, index) {
                final sms = smsList[index];
                return ListTile(
                  title: Text(sms['sender'] ?? ''),
                  subtitle: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(sms['body'] ?? ''),
                      Text(_format(sms['date'] ?? '0'),
                          style: const TextStyle(fontSize: 12)),
                    ],
                  ),
                );
              },
            ),
    );
  }
}