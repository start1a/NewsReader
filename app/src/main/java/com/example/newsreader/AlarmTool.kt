package com.example.newsreader

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock

class AlarmTool {

    companion object {

        private var alarmMgr: AlarmManager? = null
        private lateinit var alarmIntent: PendingIntent
        const val ACTION_UPDATE_CACHE_DATA = "UPDATE_CACHE_DATA"

        fun setAlarm(context: Context) {
            alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmIntent = Intent(context, CacheDataUpdate::class.java).let { intent ->
                intent.action = ACTION_UPDATE_CACHE_DATA
                PendingIntent.getBroadcast(context, 0, intent, 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmMgr?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + AlarmManager.INTERVAL_HALF_HOUR, alarmIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmMgr?.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + AlarmManager.INTERVAL_HALF_HOUR, alarmIntent)
            }else {
                alarmMgr?.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + AlarmManager.INTERVAL_HALF_HOUR, alarmIntent);
            }
        }
    }
}