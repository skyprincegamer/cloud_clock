import 'package:get/get.dart';

import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'package:crypto/crypto.dart';
import 'dart:convert';
import 'home.dart';

class GlobalController extends GetxController {
  var id = -1.obs;
}
class LoginScreen extends StatefulWidget {
  @override
  _LoginScreenState createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final TextEditingController emailController = TextEditingController();
  final TextEditingController passwordController = TextEditingController();

  void _login() async {
    String email = emailController.text;
    String Entered_Password = passwordController.text;
    late String ogPass;
    late String hashPass;
    late int id;
     try {
    // Await the data directly without .execute()
    final List<dynamic> data = await Supabase.instance.client
        .from('users')
        .select('password , id').filter('username','eq' ,email ).limit(1);
    ogPass = data[0]['password'] ;
    if (data.isEmpty) {
      print('No users found.');
    } else {
      hashPass = sha256.convert(utf8.encode(Entered_Password)).toString();
      if(ogPass ==  hashPass) {
        id = data[0]['id'];
        print("id is $id");
      }
    }
  } catch (e) {
    print('Exception occurred: $e');
  }    
    // Show dialog or snackbar
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: 
      (ogPass == hashPass)?
      Text('Password Valid'): Text('Password Invalid') ),
    );
    if(ogPass == hashPass){
      final globalController = Get.put(GlobalController());
      globalController.id = id;
      Navigator.push(context, MaterialPageRoute(builder: (context) =>  Home()));
    }
   
      }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Padding(
        padding: EdgeInsets.symmetric(horizontal: 24.0),
        child: Center(
          child: SingleChildScrollView(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.lock, size: 100, color: Colors.blue),
                SizedBox(height: 20),
                Text(
                  'Welcome Back',
                  style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                ),
                SizedBox(height: 40),

                // Email TextField
                TextField(
                  controller: emailController,
                  decoration: InputDecoration(
                    labelText: 'Email',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.email),
                  ),
                ),
                SizedBox(height: 20),

                // Password TextField
                TextField(
                  controller: passwordController,
                  obscureText: true,
                  decoration: InputDecoration(
                    labelText: 'Password',
                    border: OutlineInputBorder(),
                    prefixIcon: Icon(Icons.lock),
                  ),
                ),
                SizedBox(height: 30),

                // Login Button
                TextButton(
                  onPressed: _login,
                  style: ElevatedButton.styleFrom(
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                  ),
                  child: Padding(
                    padding: EdgeInsets.symmetric(horizontal: 40, vertical: 12),
                    child: Text('Login', style: TextStyle(fontSize: 18)),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
