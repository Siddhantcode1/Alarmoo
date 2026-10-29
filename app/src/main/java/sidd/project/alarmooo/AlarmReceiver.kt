package sidd.project.alarmooo

import android.util.Log
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(

        context: Context,
        intent: Intent
    ) {
        Log.d("Alarmoo", "Alarm triggered")
        val screenIntent = Intent("sidd.project.alarmooo.ALARM_TRIGGERED")
            .setPackage(context.packageName)
        context.sendBroadcast(screenIntent)

        val serviceIntent = Intent(context, AlarmSoundService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}


