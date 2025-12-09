package dev.skyprincegamer.cloudclock

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.skyprincegamer.cloudclock.ui.theme.CloudClockTheme
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.runBlocking


val supabase = createSupabaseClient(
    supabaseUrl = "https://dvoquibudfcvelmgyisr.supabase.co",
    supabaseKey ="sb_publishable_VpvAKa1D7yNjYlHsVuyzVw_eIZcEpf0"
) {
    install(Auth){
        alwaysAutoRefresh = true
        autoSaveToStorage = true
        autoLoadFromStorage = true
    }
    install(Postgrest)
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CloudClockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    var loginEmail by remember { mutableStateOf("") }
                    var loginPassword by remember {mutableStateOf("")}
                    val context = LocalContext.current

                    LaunchedEffect(Unit) {
                        val session = supabase.auth.currentSessionOrNull()
                        if(session != null)
                            context.startActivity(Intent(context , HomeScreen::class.java))

                    }

                    Column(modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text("Welcome Back" , fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { loginEmail = it },
                            label = { Text("Email") }
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = {
                            runBlocking{
                                try{
                                    supabase.auth.signInWith(Email) {
                                        email = loginEmail
                                        password = loginPassword
                                    }
                                    Toast.makeText(context, "SIGN IN SUCCESSFUL", Toast.LENGTH_LONG)
                                        .show()
                                    context.startActivity(Intent(context , HomeScreen::class.java))
                                }
                                catch (e : Exception){
                                    Log.i("LOGIN ERROR" , e.toString())
                                    Toast.makeText(context, "Invalid Credentials", Toast.LENGTH_LONG)
                                        .show()
                                }
                            }
                        }) { Text("CLICK ME")}
                    }
                }
            }
        }
    }
}
