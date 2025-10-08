import 'dart:io';
import 'package:flutter/services.dart';

class AlarmManager {
  List<int> alarms = List.empty(growable: true);

  static const _channel = MethodChannel('alarm_channel');

  void setAlarm(DateTime time, String message) async {
    alarms.add( _channel.invokeMethod('setAlarm', {
      'timestamp': time.millisecondsSinceEpoch,
      'message': message,
    }) as int);
  }
  void cancel(int ID) async {
    await _channel.invokeMethod('cancel' , {
      'alarm_id' : ID
    });
    alarms.remove(ID);
  }
  void cancelAll() async {
    await _channel.invokeMethod('cancelAll');
    alarms.clear();
  }

  static bool get isSupported =>
      Platform.isAndroid || Platform.isWindows || Platform.isLinux;
}
