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
import dev.skyprincegamer.cloudclock.ui.theme.CloudClockTheme
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
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
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
                    AlarmsList(alarms = listAlarms , Modifier.padding(innerPadding) , manager = alarmManager)
                    if(showAdder) AlarmAddDialog(onDismissRequest = {showAdder = false})
                }
            }
        }
    }
}

@Serializable
data class Alarm(
    val alarm_id:Int? = null,
    val created_at : String? = null,
    val alarm_at : String,
    val user_id : String,
    val active : Boolean
)

suspend fun addAlarmToDatabase(t : String) {
    val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    val localDateTime = LocalDateTime.parse(t , format)
    val utcZdt: ZonedDateTime = localDateTime
        .atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneId.of("UTC"))
    val timestamp = utcZdt.format(DateTimeFormatter.ISO_INSTANT)
    val newAlarm = Alarm(alarm_at = timestamp,
        user_id = supabase.auth.currentUserOrNull()!!.id,
        active = true)
    supabase.from("alarms").insert(newAlarm)
}
@Composable
fun AlarmsList(
    alarms: SnapshotStateList<Alarm>,
    modifier: Modifier = Modifier,
    manager: AlarmManager
) {
    val ctxt = LocalContext.current
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO){
            val results = supabase.from("alarms").select().decodeList<Alarm>()
            Log.i("Results of select" , results.toString())
            alarms.addAll(results)
        }
    }
    LaunchedEffect(Unit) {
        val channel = supabase.channel("alarms-channel")
        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "alarms"
        }.onEach { action ->
            when (action) {
                is PostgresAction.Insert -> {
                    val newAlarm = action.decodeRecord<Alarm>()
                    alarms.add(newAlarm)
                    Log.i("Realtime", "Insert: $newAlarm")
                }
                is PostgresAction.Update -> {
                    val updatedAlarm = action.decodeRecord<Alarm>()
                    val index = alarms.indexOfFirst { it.alarm_id == updatedAlarm.alarm_id }
                    if (index != -1) {
                        alarms[index] = updatedAlarm
                    }
                    Log.i("Realtime", "Update: $updatedAlarm")

                }
                is PostgresAction.Delete -> {
                    val deletedId = action.oldRecord["alarm_id"].toString().toInt()
                    alarms.removeAll { it.alarm_id == deletedId }
                    Log.i("Realtime", "Delete: $deletedId")
                    val alarmUp = (PendingIntent.getBroadcast(ctxt , deletedId ,
                        Intent(ctxt, AlarmReceiver::class.java),
                        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                    ) != null)
                    if(alarmUp){
                        manager.cancel(
                            PendingIntent.getBroadcast(
                                ctxt,
                                deletedId,
                                Intent(ctxt, AlarmReceiver::class.java),
                                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                        PendingIntent.getBroadcast(
                            ctxt,
                            deletedId,
                            Intent(ctxt, AlarmReceiver::class.java),
                            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                        ).cancel()
                        Log.i("Alarms Management" , "Alarm id $deletedId unset")
                    }
                }
                else -> {}
            }
        }.launchIn(this)

        channel.subscribe()
    }
    LazyColumn(modifier = modifier) {

        items(alarms) { alarm ->
            val scope = rememberCoroutineScope()
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            val utcTime = LocalDateTime.parse(alarm.alarm_at , format)
            val localTime: ZonedDateTime = utcTime
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.systemDefault())
            val timestamp = localTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault()).withZone(localTime.zone))


            val alarmUp = (PendingIntent.getBroadcast(ctxt , alarm.alarm_id!! ,
                Intent(ctxt, AlarmReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) != null)
            if(!alarm.active && alarmUp){
                manager.cancel(PendingIntent.getBroadcast(ctxt , alarm.alarm_id ,
                    Intent(ctxt, AlarmReceiver::class.java)
                    , PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                ) )
                PendingIntent.getBroadcast(ctxt , alarm.alarm_id ,
                    Intent(ctxt, AlarmReceiver::class.java)
                    , PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                ).cancel()
                Log.i("Alarms Management" , "Alarm id ${alarm.alarm_id} unset")
            }
            else if(alarm.active && !alarmUp){
                val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                val utcTime = LocalDateTime.parse(alarm.alarm_at , format)
                val milis= utcTime.toInstant(ZoneOffset.UTC).toEpochMilli()
                val pendingIntent = PendingIntent.getBroadcast(
                    ctxt,
                    alarm.alarm_id,
                    Intent(ctxt, AlarmReceiver::class.java).apply {
                        putExtra("ALARM_MSG", "my alarm message")
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                if(milis > System.currentTimeMillis()){
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        milis,
                        pendingIntent
                    )
                Log.i("Alarms Management" , "Alarm Set with seconds ${(milis - System.currentTimeMillis())/1000L} and id ${alarm.alarm_id}")
                }
                else{
                    Log.i("Alarms Management" , "Alarm id ${alarm.alarm_id} is expired and cannot be set")
                    scope.launch(Dispatchers.IO){
                        supabase.from("alarms").update(
                            {
                                Alarm::active setTo false
                            }
                        ) {
                            filter {
                                Alarm::alarm_id eq alarm.alarm_id
                            }
                        }
                    }
                }
            }



            ListItem(headlineContent = {Text(timestamp)} ,
                leadingContent = {
                    Checkbox(checked = alarm.active , onCheckedChange = {
                        checkedState -> scope.launch(Dispatchers.IO){
                        try{
                            supabase.from("alarms").update(
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
                            supabase.from("alarms").delete{
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
