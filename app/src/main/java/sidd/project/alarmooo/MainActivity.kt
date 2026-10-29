package sidd.project.alarmooo

import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.compose.runtime.DisposableEffect
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Space
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import sidd.project.alarmooo.ui.theme.AlarmoooTheme
import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            println("Notification permission granted : $isGranted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
        enableEdgeToEdge()
        setContent {
            AlarmoooTheme {
                Alarmoo(
                    startRinging = intent.getBooleanExtra(
                        "show_ringing_screen",
                        false
                    )
                )
            }
        }
    }
}

@Composable
fun RingingScreen(
    alarmTime: String,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5FF))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    )

    {
        Image(
            painter = painterResource(id = R.drawable.cow_mascot),
            contentDescription = "Alarmoo cow mascot",
            modifier = Modifier.size(160.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))


        Text(
            text = "Alarmoo",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6750A4)
        )
        Spacer(modifier = Modifier.padding(16.dp))
        Text(
            text = alarmTime,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF24212B)
        )
        Spacer(modifier = Modifier.padding(24.dp))
        Button(
            onClick = onStop,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6750A4)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 1.dp
            )
        ) {
            Text(
                "STOP ALARM",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun Alarmoo(
    modifier: Modifier = Modifier,
    startRinging: Boolean = false
) {
    val context = LocalContext.current
    var showAlarmPicker by remember {
        mutableStateOf(false)
    }
    var savedAlarms by remember {
        mutableStateOf(listOf<String>())
    }
    var isRinging by remember {
        mutableStateOf(startRinging)
    }
    DisposableEffect(context) {
        val alarmReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("ALarmoo", "UI received alarm broadcast")
                isRinging = true
            }
        }

        val filter = IntentFilter("sidd.project.alarmooo.ALARM_TRIGGERED")

        ContextCompat.registerReceiver(
            context,
            alarmReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(alarmReceiver)
        }
    }

    var selectedHour by remember {
        mutableStateOf(12)
    }
    var selectingMinutes by remember {
        mutableStateOf(false)
    }
    var selectedMinute by remember {
        mutableStateOf(0)
    }
    var selectedPeriod by remember {
        mutableStateOf("AM")
    }
    var selectedTime = "$selectedHour:${selectedMinute.toString().padStart(2, '0')} $selectedPeriod"
    if (isRinging) {
        RingingScreen(
            alarmTime = selectedTime,
            onStop = {
                isRinging = false
                val stopIntent = Intent(
                    context,
                    AlarmSoundService::class.java
                ).apply {
                    action = "STOP_ALARM"
                }
                context.startService(stopIntent)
            }
        )
    } else {
        if (showAlarmPicker) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Alarmoo",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.weight(1f))


                }
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    val centre = maxWidth / 2
                    val radius = maxWidth / 2
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val dx = offset.x - size.width / 2
                                    val dy = offset.y - size.height / 2
                                    val angle = Math.toDegrees(atan2(dy, dx).toDouble())
                                    println("Angle : $angle")
                                    var adjustedAngle = angle + 90
                                    if (adjustedAngle < 0) {
                                        adjustedAngle += 360
                                    }
                                    if (!selectingMinutes) {
                                        val hour = ((adjustedAngle + 15) / 30).toInt()
                                        val selectedHourValue = if (hour == 0) 12 else hour
                                        selectedHour = selectedHourValue
                                        selectingMinutes = true
                                    } else {
                                        val minute = ((adjustedAngle / 30).roundToInt() * 5) % 60
                                        selectedMinute = minute
                                    }


                                }

                            }) {
                        val centreX = size.width / 2
                        val centreY = size.height / 2
                        val radius = size.minDimension / 2

                        drawCircle(
                            color = Color(0xFFF8F5FF),
                            radius = size.minDimension / 2
                        )
                        drawCircle(
                            color = Color(0xFFD8CDEE),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 4.dp.toPx())
                        )
                        val selectedAngle = if (selectingMinutes) {
                            Math.toRadians(
                                (selectedMinute * 6 - 90).toDouble()
                            )
                        } else {
                            Math.toRadians(
                                (selectedHour * 30 - 90).toDouble()
                            )
                        }
                        val handX = centreX + radius * 0.65f * cos(selectedAngle).toFloat()
                        val handY = centreY + radius * 0.65f * sin(selectedAngle).toFloat()
                        if (!selectingMinutes) {
                            for (hour in 1..12) {
                                val angle = Math.toRadians(
                                    (hour * 30 - 90).toDouble()
                                )
                                val x = centreX + radius * 0.75f * cos(angle).toFloat()
                                val y = centreY + radius * 0.75f * sin(angle.toFloat())
                                //hour drawing code
                                if (hour == selectedHour) {
                                    drawCircle(
                                        color = Color(0xFF6750A4),
                                        radius = 30f,
                                        center = Offset(x, y)
                                    )
                                }
                                drawIntoCanvas { canvas ->
                                    val paint = android.graphics.Paint().apply {
                                        color = if (hour == selectedHour) {
                                            android.graphics.Color.WHITE
                                        } else {
                                            android.graphics.Color.BLACK
                                        }
                                        textSize = 44f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                                    canvas.nativeCanvas.drawText(
                                        hour.toString(),
                                        x,
                                        y - (paint.ascent() + paint.descent()) / 2,
                                        paint
                                    )

                                }

                            }
                        } else {
                            for (minute in 0..55 step 5) {
                                val angle = Math.toRadians((minute * 6 - 90).toDouble())
                                val x = centreX + radius * 0.75f * cos(angle).toFloat()
                                val y = centreY + radius * 0.75f * sin(angle.toFloat())

                                if (selectingMinutes && minute == selectedMinute) {
                                    drawCircle(
                                        color = Color(0xFF6750A4),
                                        radius = 30f,
                                        center = Offset(x, y)
                                    )
                                }

                                drawIntoCanvas { canvas ->
                                    val paint = android.graphics.Paint().apply {
                                        color = if (selectingMinutes && minute == selectedMinute) {
                                            android.graphics.Color.WHITE
                                        } else {
                                            android.graphics.Color.BLACK
                                        }
                                        textSize = 44f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                                    canvas.nativeCanvas.drawText(
                                        minute.toString().padStart(2, '0'),
                                        x,
                                        y - (paint.ascent() + paint.descent()) / 2,
                                        paint
                                    )
                                }
                            }
                            drawLine(
                                color = Color(0xFF6750A4),
                                start = Offset(centreX, centreY),
                                end = Offset(handX, handY),
                                strokeWidth = 4f,
                                cap = StrokeCap.Round
                            )
                            drawCircle(
                                color = Color(0xFF6750A4),
                                radius = 12f,
                                center = Offset(centreX, centreY)
                            )
                        }

                    }


                }

                Text(
                    text = selectedTime,
                    color = Color(0xFF24212B),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp)
                )
                Spacer(modifier = Modifier.padding(24.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp)
                ) {
                    listOf("AM", "PM").forEach { period ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selectedPeriod == period)
                                        Color(0xFF6750A4)
                                    else
                                        Color(0xFFEDE7F6)
                                )
                                .clickable {
                                    selectedPeriod = period
                                }
                                .padding(horizontal = 24.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = period,
                                color = if (selectedPeriod == period)
                                    Color.White
                                else
                                    Color(0xFF49454F),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        val alarmManager =
                            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val intent = Intent(context, AlarmReceiver::class.java)
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        val calender = Calendar.getInstance().apply {
                            set(
                                Calendar.HOUR_OF_DAY,
                                selectedHour % 12 + if (selectedPeriod == "PM") 12 else 0
                            )
                            set(Calendar.MINUTE, selectedMinute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)



                            if (timeInMillis <= System.currentTimeMillis()) {
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }
                        println("Alarm Set For: ${calender.time}")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    calender.timeInMillis,
                                    pendingIntent
                                )

                            }
                        }
                        savedAlarms = savedAlarms + selectedTime
                        showAlarmPicker = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6750A4),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 1.dp
                    )
                ) {
                    Text(
                        text = "Set Alarm",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.padding(8.dp))
                Button(
                    onClick = {
                        val stopIntent = Intent(
                            context,
                            AlarmSoundService::class.java
                        ).apply {
                            action = "STOP_ALARM"
                        }
                        context.startService(stopIntent)
                    }, modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6750A4),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 1.dp
                    )
                ) {
                    Text(
                        text = "Stop Alarm", fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }


            }
        } else {
            AlarmHomeScreen(savedAlarms = savedAlarms,
                onAddAlarm = {
                showAlarmPicker = true
            })
        }
    }
}

@Composable
fun AlarmHomeScreen(
    savedAlarms: List<String>,
    onAddAlarm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5FF))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Alarmoo 🐮",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6750A4)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "set Alarms, Good moorning!🐮",
            fontSize = 16.sp,
            color = Color(0xFF49454F)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = painterResource(id = R.drawable.cow_mascot),
            contentDescription = "Cute cow mascot",
            modifier = Modifier.size(110.dp)
        )
       if (savedAlarms.isEmpty()){
           Text(
               text = "No Alarms yet",
               fontSize = 22.sp,
               fontWeight = FontWeight.SemiBold,
               color = Color(0xFF24212B)
           )
       } else{
           savedAlarms.forEach{ alarmTime ->
               Text(
                   text = alarmTime,
                   fontSize = 28.sp,
                   fontWeight = FontWeight.Bold,
                   color = Color(0xFF24212B),
                   modifier = Modifier.padding(vertical = 8.dp)
               )
           }
       }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add an alarm to start your day.",
            fontSize = 15.sp,
            color = Color(0xFF49454F)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddAlarm,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6750A4),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "+ Add Alarm",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}





