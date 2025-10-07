import 'package:get/get.dart';

import 'package:flutter/material.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'home.dart';

class GlobalController extends GetxController {
  RxString id = "".obs;
}
class LoginScreen extends StatefulWidget {
  @override
  _LoginScreenState createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final TextEditingController emailController = TextEditingController();
  final TextEditingController passwordController = TextEditingController();

  void _login() async {
      final emailT = emailController.text;
      final pass = passwordController.text;
      try{
      final res = await Supabase.instance.client.auth.signInWithPassword(
        email: emailT,
        password: pass
      );
      final gb = Get.put(GlobalController());
      gb.id.value = res.user!.id;
      print("User id is ${gb.id.value}");
      Navigator.push(context, MaterialPageRoute(builder: (context) => const Home()) );
      }
      catch(e){
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Invalid Login Credentials")));
        print(e);
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
                    prefixIcon: Icon(Icons.supervised_user_circle_outlined),
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
