package dev.skyprincegamer.cloudclock

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.skyprincegamer.cloudclock.ui.theme.CloudClockTheme
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class HomeScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CloudClockTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AlarmsList(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Serializable
data class Alarm(
    val alarm_id:Int,
    val created_at : String,
    val alarm_at : String,
    val user_id : String,
    val active : Boolean
)

@Composable
fun AlarmsList(modifier: Modifier = Modifier) {
    val alarms = remember { mutableStateListOf<Alarm>() }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO){
            val results = supabase.from("alarms").select().decodeList<Alarm>()
            Log.i("Results of select" , results.toString())
            alarms.addAll(results)
        }
    }
    LazyColumn(modifier = modifier) {
        items(alarms) { alarm ->
            ListItem(headlineContent = {Text(alarm.alarm_at)} ,
                supportingContent = {Text(alarm.active.toString())})
        }
    }
}