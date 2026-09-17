package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.NusakkirDatabase
import com.example.util.AdhanPlayer
import com.example.util.NusakkirNotificationHelper
import com.example.util.PrayerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "PrayerAlarmReceiver received action: $action")

        when (action) {
            ACTION_PRAYER_ALARM -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
                val prayerKey = intent.getStringExtra(EXTRA_PRAYER_KEY) ?: "prayer"
                val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME)
                handlePrayerAlarm(context, prayerName, prayerKey, prayerTime)
            }

            ACTION_PRE_PRAYER_ALARM -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
                val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME)
                NusakkirNotificationHelper.showPrayerNotification(
                    context = context,
                    prayerName = prayerName,
                    prayerTime = prayerTime,
                    isPreAlarm = true
                )
            }

            ACTION_STOP_ADHAN -> {
                AdhanPlayer.stopAdhan(context)
                NusakkirNotificationHelper.dismissNotification(context, NusakkirNotificationHelper.NOTIFICATION_ID_PRAYER)
            }

            ACTION_TEST_ADHAN -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "الظهر"
                val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME)
                NusakkirNotificationHelper.showPrayerNotification(
                    context = context,
                    prayerName = prayerName,
                    prayerTime = prayerTime,
                    isPreAlarm = false,
                    isTest = true
                )
                AdhanPlayer.playAdhan(context, prayerName)
            }

            ACTION_ADHKAR_MORNING -> {
                NusakkirNotificationHelper.showAdhkarReminder(context, isMorning = true)
            }

            ACTION_ADHKAR_EVENING -> {
                NusakkirNotificationHelper.showAdhkarReminder(context, isMorning = false)
            }
        }
    }

    private fun handlePrayerAlarm(context: Context, prayerName: String, prayerKey: String, prayerTime: String? = null) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = NusakkirDatabase.getInstance(appContext)
                val settingsDao = database.settingsDao()

                // Check user preferences
                val isNotifyEnabled = settingsDao.getSettingValue("notify_prayer")?.toBooleanStrictOrNull() ?: true
                val isAdhanEnabled = settingsDao.getSettingValue("play_adhan_sound")?.toBooleanStrictOrNull() ?: true

                if (isNotifyEnabled) {
                    NusakkirNotificationHelper.showPrayerNotification(
                        context = appContext,
                        prayerName = prayerName,
                        prayerTime = prayerTime,
                        isPreAlarm = false
                    )

                    if (isAdhanEnabled) {
                        AdhanPlayer.playAdhan(appContext, prayerName)
                    }
                }

                // Reschedule next prayer alarms
                PrayerScheduler.rescheduleFromDatabase(appContext)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling prayer alarm", e)
                // Fallback: show notification and play adhan
                NusakkirNotificationHelper.showPrayerNotification(appContext, prayerName, prayerTime, false)
                AdhanPlayer.playAdhan(appContext, prayerName)
            }
        }
    }

    companion object {
        private const val TAG = "PrayerAlarmReceiver"

        const val ACTION_PRAYER_ALARM = "com.example.action.PRAYER_ALARM"
        const val ACTION_PRE_PRAYER_ALARM = "com.example.action.PRE_PRAYER_ALARM"
        const val ACTION_STOP_ADHAN = "com.example.action.STOP_ADHAN"
        const val ACTION_TEST_ADHAN = "com.example.action.TEST_ADHAN"
        const val ACTION_ADHKAR_MORNING = "com.example.action.ADHKAR_MORNING"
        const val ACTION_ADHKAR_EVENING = "com.example.action.ADHKAR_EVENING"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
    }
}
