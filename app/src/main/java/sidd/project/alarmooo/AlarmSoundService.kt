package sidd.project.alarmooo

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresApi

class AlarmSoundService : Service() {
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alarmoo_channel",
                "Alarmoo Alarm",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(
        intent: Intent?, flags: Int,
        startId: Int
    ): Int {
        if (intent?.action == "STOP_ALARM") {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        createNotificationChannel()
        val openAppIntent = Intent(this, MainActivity::class.java)
        openAppIntent.putExtra("show_ringing_screen",true)
        val pendingOpenAppIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(
            this,
            "alarmoo_channel"
        )
            .setContentTitle("Alarmoo is ringing")
            .setContentText("Your alarm is active")
            .setContentIntent(pendingOpenAppIntent)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
        val alarmUri = RingtoneManager.getDefaultUri(
            RingtoneManager.TYPE_ALARM
        )
        ringtone = RingtoneManager.getRingtone(
            this,
            alarmUri
        )
        ringtone?.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        ringtone?.play()

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        ringtone?.stop()
        ringtone = null
        super.onDestroy()
    }

    private var ringtone: Ringtone? = null
}