import 'dart:async';

import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:get/get.dart';
import 'package:date_time_picker/date_time_picker.dart';
import 'package:intl/intl.dart';
import 'login.dart';
import 'package:url_launcher/url_launcher.dart';

class Home extends StatefulWidget {
  const Home({super.key});

  @override
  State<Home> createState() => _HomeState();
}

class _HomeState extends State<Home> {
  var Timer_Map = Map<int,Timer>() ;
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
          'user_id': Get.find<GlobalController>().id,
        });
      } 
    } catch (e) {
      print("Error parsing date: $e");
    }
  }
  void runCodeAt(DateTime target , int alarm_id,  Function clbk){
    final diff = target.difference(DateTime.now());
    if(diff.isNegative){
      print("Aint possible");
      return;
    }
    print('Timer set for $diff and id $alarm_id');
    Timer_Map[alarm_id] = Timer(diff, ()
    {
      clbk();
      });
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
                  final dformat = DateFormat("yyyy-MM-ddTHH:mm:ss");
                  if (alarms.isEmpty) {
                    return const Center(child: Text('No alarms set.'));
                  }
                  for(final alarm in alarms){
                    if(alarm['active']){
                      final formatted = dformat.parseUtc(alarm['alarm_at']);
                      runCodeAt(formatted,alarm['alarm_id'], () async{
                        final url = 'https://www.youtube.com/watch?v=PflEJM-aUug';
                        if (await canLaunchUrl(Uri.parse(url))) {
                          await launchUrl(Uri.parse(url));
                        } else {
                           print('Could not launch $url');
                        }
                      });                      
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
                              dformat.parseUtc(alarm['alarm_at']).toLocal().toIso8601String()
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
