package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.NusakkirDatabase
import com.example.data.prayer.PrayerSchedule
import com.example.data.prayer.PrayerType
import com.example.receiver.PrayerAlarmReceiver
import java.util.Calendar

object PrayerScheduler {
    private const val TAG = "PrayerScheduler"

    private const val REQ_FAJR = 1001
    private const val REQ_DHUHR = 1002
    private const val REQ_ASR = 1003
    private const val REQ_MAGHRIB = 1004
    private const val REQ_ISHA = 1005

    private const val REQ_PRE_FAJR = 2001
    private const val REQ_PRE_DHUHR = 2002
    private const val REQ_PRE_ASR = 2003
    private const val REQ_PRE_MAGHRIB = 2004
    private const val REQ_PRE_ISHA = 2005

    private const val REQ_ADHKAR_MORNING = 3001
    private const val REQ_ADHKAR_EVENING = 3002

    fun schedulePrayerAlarms(
        context: Context,
        schedule: PrayerSchedule,
        notifyBefore: Boolean = true,
        notifyMorningAdhkar: Boolean = true,
        notifyEveningAdhkar: Boolean = true
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Cancel previous alarms first to avoid duplicates
        cancelAllPrayerAlarms(context)

        val reqCodeMap = mapOf(
            PrayerType.FAJR to Pair(REQ_FAJR, REQ_PRE_FAJR),
            PrayerType.DHUHR to Pair(REQ_DHUHR, REQ_PRE_DHUHR),
            PrayerType.ASR to Pair(REQ_ASR, REQ_PRE_ASR),
            PrayerType.MAGHRIB to Pair(REQ_MAGHRIB, REQ_PRE_MAGHRIB),
            PrayerType.ISHA to Pair(REQ_ISHA, REQ_PRE_ISHA)
        )

        val prayerItems = schedule.prayers.filter { it.type != PrayerType.SUNRISE }

        for (prayer in prayerItems) {
            val reqCodes = reqCodeMap[prayer.type] ?: continue
            val (mainReqCode, preReqCode) = reqCodes

            var triggerTime = prayer.timestampMillis
            if (triggerTime <= System.currentTimeMillis()) {
                // Advance to tomorrow if time has already passed today
                triggerTime += 24 * 60 * 60 * 1000L
            }

            // Main prayer alarm (Adhan & notification)
            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer.type.arabicName)
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_KEY, prayer.type.name)
                putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, prayer.timeFormatted)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                mainReqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            scheduleAlarm(alarmManager, triggerTime, pendingIntent)
            Log.d(TAG, "Scheduled alarm for ${prayer.type.arabicName} at $triggerTime")

            // Pre-prayer alarm (15 minutes prior)
            if (notifyBefore) {
                val preTriggerTime = triggerTime - (15 * 60 * 1000L)
                if (preTriggerTime > System.currentTimeMillis()) {
                    val preIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        action = PrayerAlarmReceiver.ACTION_PRE_PRAYER_ALARM
                        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayer.type.arabicName)
                        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_KEY, prayer.type.name)
                        putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, prayer.timeFormatted)
                    }
                    val prePendingIntent = PendingIntent.getBroadcast(
                        context,
                        preReqCode,
                        preIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    scheduleAlarm(alarmManager, preTriggerTime, prePendingIntent)
                }
            }
        }

        // Schedule Morning Adhkar reminder (07:00 AM)
        if (notifyMorningAdhkar) {
            val morningMillis = getSpecificTimeMillis(7, 0)
            val morningIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_ADHKAR_MORNING
            }
            val morningPi = PendingIntent.getBroadcast(
                context,
                REQ_ADHKAR_MORNING,
                morningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleAlarm(alarmManager, morningMillis, morningPi)
        }

        // Schedule Evening Adhkar reminder (17:00 / 05:00 PM)
        if (notifyEveningAdhkar) {
            val eveningMillis = getSpecificTimeMillis(17, 0)
            val eveningIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = PrayerAlarmReceiver.ACTION_ADHKAR_EVENING
            }
            val eveningPi = PendingIntent.getBroadcast(
                context,
                REQ_ADHKAR_EVENING,
                eveningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleAlarm(alarmManager, eveningMillis, eveningPi)
        }
    }

    private fun scheduleAlarm(alarmManager: AlarmManager, triggerTimeMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Exact alarm permission denied, fallback to standard alarm", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        }
    }

    fun cancelAllPrayerAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val allCodes = listOf(
            REQ_FAJR, REQ_DHUHR, REQ_ASR, REQ_MAGHRIB, REQ_ISHA,
            REQ_PRE_FAJR, REQ_PRE_DHUHR, REQ_PRE_ASR, REQ_PRE_MAGHRIB, REQ_PRE_ISHA,
            REQ_ADHKAR_MORNING, REQ_ADHKAR_EVENING
        )

        for (code in allCodes) {
            val intent = Intent(context, PrayerAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun parseTimeToMillis(timeStr: String): Long {
        try {
            val parts = timeStr.trim().split(":")
            if (parts.size >= 2) {
                val hour = parts[0].toInt()
                val minute = parts[1].split(" ")[0].toInt()

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // If prayer time has already passed today, schedule for tomorrow
                if (cal.timeInMillis <= System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }

                return cal.timeInMillis
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time: $timeStr", e)
        }
        return 0L
    }

    private fun getSpecificTimeMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    suspend fun rescheduleFromDatabase(context: Context) {
        try {
            val database = NusakkirDatabase.getInstance(context)
            val prayerEntity = database.prayerTimesDao().getLatestPrayerTimeSync()
            val settingsDao = database.settingsDao()

            val notifyPrayer = settingsDao.getSettingValue("notify_prayer")?.toBooleanStrictOrNull() ?: true
            val notifyBefore = settingsDao.getSettingValue("notify_before_prayer")?.toBooleanStrictOrNull() ?: true
            val notifyMorning = settingsDao.getSettingValue("notify_morning_adhkar")?.toBooleanStrictOrNull() ?: true
            val notifyEvening = settingsDao.getSettingValue("notify_evening_adhkar")?.toBooleanStrictOrNull() ?: true

            if (prayerEntity != null && notifyPrayer) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                cancelAllPrayerAlarms(context)

                val prayers = listOf(
                    Triple(PrayerType.FAJR, prayerEntity.fajr, REQ_FAJR to REQ_PRE_FAJR),
                    Triple(PrayerType.DHUHR, prayerEntity.dhuhr, REQ_DHUHR to REQ_PRE_DHUHR),
                    Triple(PrayerType.ASR, prayerEntity.asr, REQ_ASR to REQ_PRE_ASR),
                    Triple(PrayerType.MAGHRIB, prayerEntity.maghrib, REQ_MAGHRIB to REQ_PRE_MAGHRIB),
                    Triple(PrayerType.ISHA, prayerEntity.isha, REQ_ISHA to REQ_PRE_ISHA)
                )

                for ((type, timeStr, reqCodes) in prayers) {
                    val triggerTime = parseTimeToMillis(timeStr)
                    if (triggerTime > 0) {
                        val (reqCode, preReqCode) = reqCodes
                        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
                            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, type.arabicName)
                            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_KEY, type.name)
                            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, timeStr)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            reqCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        scheduleAlarm(alarmManager, triggerTime, pendingIntent)

                        if (notifyBefore) {
                            val preTriggerTime = triggerTime - (15 * 60 * 1000L)
                            if (preTriggerTime > System.currentTimeMillis()) {
                                val preIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                                    action = PrayerAlarmReceiver.ACTION_PRE_PRAYER_ALARM
                                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, type.arabicName)
                                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_KEY, type.name)
                                    putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_TIME, timeStr)
                                }
                                val prePendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    preReqCode,
                                    preIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                scheduleAlarm(alarmManager, preTriggerTime, prePendingIntent)
                            }
                        }
                    }
                }

                if (notifyMorning) {
                    val morningMillis = getSpecificTimeMillis(7, 0)
                    val morningIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        action = PrayerAlarmReceiver.ACTION_ADHKAR_MORNING
                    }
                    val morningPi = PendingIntent.getBroadcast(
                        context,
                        REQ_ADHKAR_MORNING,
                        morningIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    scheduleAlarm(alarmManager, morningMillis, morningPi)
                }

                if (notifyEvening) {
                    val eveningMillis = getSpecificTimeMillis(17, 0)
                    val eveningIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        action = PrayerAlarmReceiver.ACTION_ADHKAR_EVENING
                    }
                    val eveningPi = PendingIntent.getBroadcast(
                        context,
                        REQ_ADHKAR_EVENING,
                        eveningIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    scheduleAlarm(alarmManager, eveningMillis, eveningPi)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling from database", e)
        }
    }
}
