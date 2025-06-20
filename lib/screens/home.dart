import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:get/get.dart';
import 'package:date_time_picker/date_time_picker.dart';
import 'login.dart';

class Home extends StatefulWidget {
  const Home({super.key});

  @override
  State<Home> createState() => _HomeState();
}

class _HomeState extends State<Home> {
  void sendDateToDB(String? date) async {
    print("sendDateToDB called with: $date");

    if (date == null) {
      print("Date is null, aborting.");
      return;
    }

    try {
      final parsedDate = DateTime.parse(date);
      print("Parsed date: $parsedDate");

      if (parsedDate.isAfter(DateTime.now())) {
        await Supabase.instance.client.from('alarms').insert({
          'alarm_at': date,
          'user_id': Get.find<GlobalController>().id,
        });
      } 
    } catch (e) {
      print("Error parsing date: $e");
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
                  if (alarms.isEmpty) {
                    return const Center(child: Text('No alarms set.'));
                  }

                  return ListView.builder(
                    itemCount: alarms.length,
                    itemBuilder: (context, index) {
                      final alarm = alarms[index];
                      return ListTile(
                        title: Text(
                          alarm['alarm_at'],
                          style: const TextStyle(fontSize: 24.0),
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
