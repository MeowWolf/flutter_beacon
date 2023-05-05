import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_beacon_example/controller/requirement_state_controller.dart';
import 'package:flutter_beacon_example/services/notification_service.dart';
import 'package:flutter_beacon_example/view/home_page.dart';
import 'package:get/get.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  _requestPermissions();
  NotificationService().initNotification();
  runApp(MainApp());
}

Future<void> _requestPermissions() async {
  if (Platform.isIOS || Platform.isMacOS) {
    await FlutterLocalNotificationsPlugin()
        .resolvePlatformSpecificImplementation<
            IOSFlutterLocalNotificationsPlugin>()
        ?.requestPermissions(
          alert: true,
          badge: true,
          sound: true,
        );
    await FlutterLocalNotificationsPlugin()
        .resolvePlatformSpecificImplementation<
            MacOSFlutterLocalNotificationsPlugin>()
        ?.requestPermissions(
          alert: true,
          badge: true,
          sound: true,
        );
  } else if (Platform.isAndroid) {
    final AndroidFlutterLocalNotificationsPlugin? androidImplementation =
        FlutterLocalNotificationsPlugin().resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>();
  }
}

class MainApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    Get.put(RequirementStateController());

    final themeData = Theme.of(context);
    final primary = Colors.blue;

    return GetMaterialApp(
      theme: ThemeData(
        brightness: Brightness.light,
        primarySwatch: primary,
        appBarTheme: themeData.appBarTheme.copyWith(
          brightness: Brightness.light,
          elevation: 0.5,
          color: Colors.white,
          actionsIconTheme: themeData.primaryIconTheme.copyWith(
            color: primary,
          ),
          iconTheme: themeData.primaryIconTheme.copyWith(
            color: primary,
          ),
          textTheme: themeData.primaryTextTheme.copyWith(
            headline6: themeData.textTheme.headline6?.copyWith(
              color: primary,
            ),
          ),
        ),
      ),
      darkTheme: ThemeData(
        brightness: Brightness.dark,
        primarySwatch: primary,
      ),
      home: HomePage(),
    );
  }
}
