import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  await dotenv.load(fileName: ".env");

  // Debug: confirm dotenv loaded
  print("DOTENV LOADED");
  print("API_URL from dotenv = ${dotenv.env['API_URL']}");

  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();

    // start immediately
    SmsService.startListening();
  }

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: Scaffold(
        body: Center(child: Text('SMS Receiver Running')),
      ),
    );
  }
}

class SmsService {
  static const _channel = MethodChannel('com.example.sou/sms');

  static Future<void> startListening() async {
    final apiUrl = dotenv.env['API_URL'] ?? '';

    print("=== FLUTTER DEBUG START ===");
    print("Raw dotenv API_URL = ${dotenv.env['API_URL']}");
    print("Final apiUrl = $apiUrl");
    print("apiUrl length = ${apiUrl.length}");

    if (apiUrl.isEmpty) {
      print("❌ ERROR: API_URL is EMPTY. Check .env file");
      return;
    }

    if (!apiUrl.startsWith("http")) {
      print("❌ ERROR: API_URL missing http/https -> $apiUrl");
      return;
    }

    try {
      final result = await _channel.invokeMethod(
        'startSmsReceiver',
        {'apiUrl': apiUrl},
      );

      print("✅ MethodChannel success: $result");
    } catch (e) {
      print("❌ MethodChannel error: $e");
    }

    print("=== FLUTTER DEBUG END ===");
  }
}


























// import 'package:flutter/material.dart';
// import 'package:flutter/services.dart';
// import 'package:flutter_dotenv/flutter_dotenv.dart';

// Future<void> main() async {
//   WidgetsFlutterBinding.ensureInitialized();
//   await dotenv.load(fileName: ".env");
//   runApp(const MyApp());
// }

// class MyApp extends StatelessWidget {
//   const MyApp({super.key});

//   @override
//   Widget build(BuildContext context) {
//     return const MaterialApp(
//       home: Scaffold(
//         body: Center(child: Text('SMS Receiver Running')),
//       ),
//     );
//   }
// }

// class SmsService {
//   static const _channel = MethodChannel('com.example.sou/sms');

//   static Future<void> startListening() async {
//     print(dotenv.env['API_URL']);
//     final apiUrl = dotenv.env['API_URL'] ?? '';
//     print(dotenv.env['API_URL']);
//     await _channel.invokeMethod('startSmsReceiver', {'apiUrl': apiUrl});
//   }
// }