package dev.skyprincegamer.cloudclock.alarm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import dev.skyprincegamer.cloudclock.alarm.ui.theme.CloudClockTheme


class AlarmScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContent {
            CloudClockTheme {
                val ctxt = LocalContext.current
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    AlarmScreen(
                        onDismiss = { dismissAlarm(ctxt) },
                        alarmTime = intent.getStringExtra("ALARM_AT"),
                        modifier = Modifier.padding(innerPadding)
                    )
                }

            }
        }
    }
    fun dismissAlarm(ctxt : Context) {
        stopService(Intent(ctxt, AlarmRingService::class.java))
        finish()
    }
}


@Composable
fun AlarmScreen(onDismiss: () -> Unit, alarmTime: String?, modifier: Modifier){
    if(alarmTime == null)
        return

    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally){
        Text(text = "YOUR ALARM IS RINGING!!" , fontSize = 24.sp)
        Text(alarmTime)
        TextButton(onClick = { onDismiss() }) { Text("Dismiss") }
    }
}


