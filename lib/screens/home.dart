import 'dart:async';
import 'dart:isolate';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:get/get.dart';
import 'package:date_time_picker/date_time_picker.dart';
import 'package:intl/intl.dart';
import 'login.dart';
import 'package:android_alarm_manager_plus/android_alarm_manager_plus.dart';
import 'package:audioplayers/audioplayers.dart';
class Home extends StatefulWidget {
  const Home({super.key});

  @override
  State<Home> createState() => _HomeState();
}

class _HomeState extends State<Home> {
  var Timer_Map = <int,Timer>{} ;
  void sendDateToDB(String? date) async {
    print("sendDateToDB called with: $date");

    if (date == null) {
      print("Date is null, aborting.");
      return;
    }

    try {
      final parsedDate = DateTime.parse(date).toUtc();
      print("Parsed date: ${parsedDate.toIso8601String()}");

      if (parsedDate.isAfter(DateTime.now())) {
        await Supabase.instance.client.from('alarms').insert({
          'alarm_at': parsedDate.toIso8601String(),
          'user_id': Get.find<GlobalController>().id.value,
        });
      } 
    } catch (e) {
      print("Error parsing date: $e");
    }
  }

  @pragma('vm:entry-point')
  void  weapon() async{    
  final DateTime now = DateTime.now();
  final int isolateId = Isolate.current.hashCode;
  print("[$now] Hello, world! isolate=$isolateId function='$murder'");
}
  void murder(DateTime f , int ID) async {
    if(Platform.isAndroid){await AndroidAlarmManager.initialize();
    final int alarmID = 1;
    await AndroidAlarmManager.oneShot(const Duration(minutes: 1), alarmID, weapon);}
    else if(Platform.isLinux){
      if (f.difference(DateTime.now()).isNegative){
        return;
      }
      Timer_Map[ID] = Timer(f.difference(DateTime.now()), () async {
        await Process.run('notify-send', ['${f.toLocal()}', 'Alarm']);
        final player = AudioPlayer();
        await player.play(AssetSource('mixkit-retro-game-emergency-alarm-1000.wav'));
      });
    }
  }


  @override
  Widget build(BuildContext context) {
    final userId = Get.find<GlobalController>().id;
    return Scaffold(
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            const Text(
              'Welcome User',
              style: TextStyle(
                color: Colors.black,
                fontSize: 36.0,
                fontFamily: 'Inter',
                decoration: TextDecoration.underline,
                decorationColor: Colors.black,
              ),
            ),
            const SizedBox(height: 20),

            Expanded(
              child: StreamBuilder<List<Map<String, dynamic>>>(
                stream: Supabase.instance.client
                    .from('alarms')
                    .stream(primaryKey: ['alarm_id'])
                    .eq('user_id', userId),
                builder: (context, snapshot) {
                  if (!snapshot.hasData) {
                    return const Center(child: CircularProgressIndicator());
                  }

                  final alarms = snapshot.data!;
                  final dBformat = DateFormat("yyyy-MM-ddTHH:mm:ss");
                  if (alarms.isEmpty) {
                    return const Center(child: Text('No alarms set.'));
                  }
                  for(final alarm in alarms){
                    if(alarm['active']){
                      final formatted = dBformat.parseUtc(alarm['alarm_at']);
                      murder(formatted, alarm['alarm_id']);                      
                    }
                    else if(!alarm['active']){
                      if(Timer_Map.containsKey(alarm['alarm_id'])){
                        Timer_Map[alarm['alarm_id']]?.cancel();
                        print("Timer ${alarm['alarm_id']} cancelled");
                      }
                    }
                  }

                  return ListView.builder(
                    itemCount: alarms.length,
                    itemBuilder: (context, index) {
                      final alarm = alarms[index];
                      return ListTile(
                        title: Row(
                          children: [
                            Text(
                              DateFormat.yMMMMd(Localizations.localeOf(context).toString()).add_jm().format(dBformat.parseUtc(alarm['alarm_at']).toLocal())
                              ,
                              style: const TextStyle(fontSize: 24.0),
                            ),
                            Checkbox(value: alarm['active'], onChanged: (v) async{
                                await Supabase.instance.client
                                      .from('alarms')
                                      .update({'active': v})
                                      .eq('alarm_id', alarm['alarm_id']);
                            }),
                          IconButton(onPressed: () async{
                            await Supabase.instance.client
                            .from('alarms')
                            .delete().eq('alarm_id', alarm['alarm_id']);
                          setState(() {});

                            if(Timer_Map.containsKey(alarm['alarm_id'])){
                              Timer_Map[alarm['alarm_id']]?.cancel();
                              print("Timer ${alarm['alarm_id']} cancelled");
                            }

                          }, icon:Icon(Icons.delete)
                          )
                          ],
                        ),
                        
                      );
                    },
                  );
                },
              ),
            ),

            const SizedBox(height: 20),

            /// Add New Alarm Dialog
            TextButton(
              onPressed: () {
                TextEditingController controller = TextEditingController(
                  text: DateTime.now().toString(),
                );

                showDialog(
                  context: context,
                  builder: (context) {
                    return AlertDialog(
                      title: const Text('Pick Alarm Time'),
                      content: DateTimePicker(
                        controller: controller,
                        type: DateTimePickerType.dateTimeSeparate,
                        dateMask: 'd MMM, yyyy',
                        firstDate: DateTime(2000),
                        lastDate: DateTime(2100),
                        icon: const Icon(Icons.event),
                        dateLabelText: 'Date',
                        timeLabelText: "Hour",
                      ),
                      actions: [
                        TextButton(
                          onPressed: () => Navigator.pop(context, 'Cancel'),
                          child: const Text('Cancel'),
                        ),
                        TextButton(
                          onPressed: () {
                            print("OK pressed. Sending date: ${controller.text}");
                            sendDateToDB(controller.text);
                            Navigator.pop(context, 'OK');
                          },
                          child: const Text('OK'),
                        ),
                      ],
                    );
                  },
                );
              },
              child: const Text('Show Dialog'),
            ),

            /// Go Back
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text(
                "Go Back",
                style: TextStyle(fontSize: 24.0),
              ),
            ),
            
          ],
        ),
      ),
    );
  }
}
