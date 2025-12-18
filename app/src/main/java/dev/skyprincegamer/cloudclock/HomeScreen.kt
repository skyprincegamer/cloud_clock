package dev.skyprincegamer.cloudclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.skyprincegamer.cloudclock.db.AlarmDatabase
import dev.skyprincegamer.cloudclock.db.RoomManager
import dev.skyprincegamer.cloudclock.models.Alarm
import dev.skyprincegamer.cloudclock.sup.SupabaseManager
import dev.skyprincegamer.cloudclock.ui.theme.CloudClockTheme
import dev.skyprincegamer.cloudclock.util.AlarmReceiver
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Locale


class HomeScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CloudClockTheme {
                val ctxt = LocalContext.current

                var showAdder by remember { mutableStateOf(false) }
                val listAlarms = remember { mutableStateListOf<Alarm>() }
                val alarmManager = ctxt.getSystemService(ALARM_SERVICE) as AlarmManager
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Log.e(
                            "Alarms Management",
                            "Cannot schedule exact alarms - permission not granted"
                        )
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        ctxt.startActivity(intent)
                    }
                    else{
                        Log.i(
                            "Alarms Management",
                            "manager.canScheduleExactAlarms() = ${alarmManager.canScheduleExactAlarms()}"
                        )
                    }
                }
                Scaffold(modifier = Modifier.fillMaxSize() ,
                    floatingActionButton = { AlarmAddButton(onClick = {showAdder = true})
                }) { innerPadding ->
                    AlarmsList(Modifier.padding(innerPadding) , RoomManager.getDB(ctxt))
                    if(showAdder) AlarmAddDialog(onDismissRequest = {showAdder = false})
                }
            }
        }
    }
}



suspend fun addAlarmToDatabase(t : String) {
    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val localDateTime = LocalDateTime.parse(t , format)
    val utcZdt: ZonedDateTime = localDateTime
        .atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneId.of("UTC"))
    val timestamp = utcZdt.format(DateTimeFormatter.ISO_INSTANT)
    val newAlarm = Alarm(
        alarm_at = timestamp,
        user_id = SupabaseManager.getClient().auth.currentUserOrNull()!!.id,
        active = true
    )
    SupabaseManager.getClient().from("alarms").insert(newAlarm)
}
@Composable
fun AlarmsList(
    modifier: Modifier = Modifier,
    db : AlarmDatabase
) {

    val ctxt = LocalContext.current
    val alarms by db.alarmDAO().getAll().collectAsStateWithLifecycle(initialValue = emptyList())

    LazyColumn(modifier = modifier) {

        items(alarms,
            key = {a -> a.alarm_id!!})
        { alarm ->
            val scope = rememberCoroutineScope()
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            val utcTime = LocalDateTime.parse(alarm.alarm_at , format)
            val localTime: ZonedDateTime = utcTime
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.systemDefault())
            val timestamp = localTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault()).withZone(localTime.zone))

            ListItem(headlineContent = {Text(timestamp)} ,
                leadingContent = {
                    Checkbox(checked = alarm.active , onCheckedChange = {
                        checkedState -> scope.launch(Dispatchers.IO){
                        try{
                            SupabaseManager.getClient().from("alarms").update(
                                {
                                    Alarm::active setTo checkedState
                                }
                            ) {
                                filter {
                                    Alarm::alarm_id eq alarm.alarm_id
                                }
                            }
                        }
                        catch (_ : Exception){
                            Log.i("Realtime" , "Cannot set an alarm in the past as active")
                            withContext(Dispatchers.Main){
                                Toast.makeText(ctxt , "Cannot set an alarm in past as active" , Toast.LENGTH_LONG).show()
                            }
                        }
                }
                })},
                trailingContent = {
                    IconButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            SupabaseManager.getClient().from("alarms").delete{
                                filter {
                                    Alarm::alarm_id eq alarm.alarm_id
                                }
                            }
                        }

                    }) {
                        Icon(Icons.Default.Delete,
                            contentDescription = "Alarm Delete Button")
                    }
                }

            )

        }
    }
}

@Composable
fun AlarmAddButton( onClick : () -> Unit){
    LargeFloatingActionButton(
        onClick = { onClick() }
    ){
        Icon(
            Icons.Default.AlarmAdd,
            contentDescription = "Alarm Add Button",
            Modifier.size(72.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmAddDialog(onDismissRequest: () -> Unit) {
    var myTime by remember { mutableStateOf("   ") }
    var showDateModal by remember { mutableStateOf(false) }
    var showTimeModal by remember { mutableStateOf(false) }
    val ctxt = LocalContext.current
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(Modifier.fillMaxWidth()) {
            Row {
                val scope = rememberCoroutineScope()
                Text(myTime)
                TextButton(onClick = {showDateModal = true}) { Text("Pick\nDate") }
                TextButton(onClick = {showTimeModal = true} , enabled = myTime.isNotBlank()) { Text("Pick\nTime") }
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO){
                        try{
                            addAlarmToDatabase(myTime)
                            onDismissRequest()
                        }
                        catch (_ : Exception){
                            withContext(Dispatchers.Main){
                                Toast.makeText(ctxt , "Cannot add an alarm that's in the past" ,Toast.LENGTH_LONG).show()
                            }
                        }
                    } },
                    enabled = myTime.isNotBlank() && myTime.contains(':'))
                { Text("Add") }
            }
        }
    }
    if(showDateModal){
        DateModal(onDateSelect = {date ->
            if(date != null)
                myTime = date
        },
            onDismiss = {showDateModal = false})
    }
    if(showTimeModal){
        DialWithDialogExample(onConfirm = {
            state -> myTime += " " + String.format("%02d" , state.hour) + ":" + String.format("%02d",state.minute)
            showTimeModal = false
        },
            onDismiss = {showTimeModal = false})
    }
}

@Composable
fun DateModal(onDateSelect : (String?) -> Unit , onDismiss : () -> Unit){
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selectedDate = datePickerState.selectedDateMillis?.let { millis ->
                    val instant = Instant.ofEpochMilli(millis)
                    val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                    localDate.format(DateTimeFormatter.ISO_LOCAL_DATE) // Format: yyyy-MM-dd
                }
                onDateSelect(selectedDate)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialWithDialogExample(
    onConfirm: (TimePickerState) -> Unit,
    onDismiss: () -> Unit,
) {
    val currentTime = Calendar.getInstance()

    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = currentTime.get(Calendar.MINUTE),
        is24Hour = true,
    )

    TimePickerDialog(
        onDismiss = { onDismiss() },
        onConfirm = { onConfirm(timePickerState) }
    ) {
        TimePicker(
            state = timePickerState,
        )
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("Dismiss")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text("OK")
            }
        },
        text = { content() }
    )
}
