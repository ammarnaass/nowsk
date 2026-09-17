package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.PrayerAlarmReceiver

object NusakkirNotificationHelper {

    const val CHANNEL_PRAYER = "channel_prayer_alerts"
    const val CHANNEL_PRAYER_ALERTS = "channel_prayer_alerts"
    const val CHANNEL_ADHAN_SERVICE = "channel_adhan_playback_service"
    const val CHANNEL_ADHKAR = "channel_adhkar_reminders"

    const val NOTIFICATION_ID_PRE_PRAYER = 101
    const val NOTIFICATION_ID_PRAYER = 102
    const val NOTIFICATION_ID_ADHAN_SERVICE = 103
    const val NOTIFICATION_ID_ADHKAR = 201

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.adhan}")

            // Dedicated Channel 1: High-importance Prayer & Adhan Alerts
            val prayerAlertsChannel = NotificationChannel(
                CHANNEL_PRAYER_ALERTS,
                "تنبيهات أوقات الصلاة والأذان",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات دخول أوقات الصلاة المكتوبة مع صوت الأذان والتنبيه المسبق"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 1000)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                enableLights(true)
            }

            // Dedicated Channel 2: Foreground Adhan MediaPlayer Service (low sound/silent so it doesn't double-beep while Adhan plays)
            val adhanServiceChannel = NotificationChannel(
                CHANNEL_ADHAN_SERVICE,
                "خدمة تشغيل الأذان في الخلفية",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار تشغيل صوت الأذان مع أزرار التحكم والإيقاف"
                setShowBadge(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            // Channel 3: Daily Adhkar Reminders
            val adhkarChannel = NotificationChannel(
                CHANNEL_ADHKAR,
                "تنبيهات الأذكار والورد",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تذكير بأذكار الصباح والمساء وورد القرآن الكريم"
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(prayerAlertsChannel)
            notificationManager.createNotificationChannel(adhanServiceChannel)
            notificationManager.createNotificationChannel(adhkarChannel)
        }
    }

    fun showPrayerNotification(
        context: Context,
        prayerName: String,
        prayerTime: String? = null,
        isPreAlarm: Boolean = false,
        isTest: Boolean = false
    ) {
        createNotificationChannels(context)

        // Main Tap Intent -> Open app to Prayer Times screen
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("initial_route", "prayer_times")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            10,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Stop Adhan Action Intent
        val stopAdhanIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_STOP_ADHAN
        }
        val stopAdhanPendingIntent = PendingIntent.getBroadcast(
            context,
            20,
            stopAdhanIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Adhkar Action Intent
        val adhkarIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("initial_route", "adhkar")
        }
        val adhkarPendingIntent = PendingIntent.getActivity(
            context,
            30,
            adhkarIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val timeSnippet = if (!prayerTime.isNullOrBlank()) " ($prayerTime)" else ""

        val title = when {
            isTest -> "🔔 أذان صلاة $prayerName$timeSnippet"
            isPreAlarm -> "⏳ اقتراب وقت صلاة $prayerName$timeSnippet"
            else -> "🕌 حان الآن وقت صلاة $prayerName$timeSnippet"
        }

        val message = when {
            isTest -> "الله أكبر، الله أكبر.. تجربة تنبيه الأذان لصلاة $prayerName الساعة $prayerTime. يرفع الأذان الآن."
            isPreAlarm -> "متبقي 15 دقيقة على صلاة $prayerName$timeSnippet. استعد للوضوء والصلاة."
            else -> "الله أكبر، الله أكبر.. حان الآن موعد أذان صلاة $prayerName$timeSnippet. حيّ على الصلاة، حيّ على الفلاح."
        }

        val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.adhan}")

        val builder = NotificationCompat.Builder(context, CHANNEL_PRAYER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setSummaryText("مواقيت الصلاة والأذان")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 1000))
            .setAutoCancel(true)

        if (!isPreAlarm) {
            // Add Stop Adhan action button
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "إيقاف الأذان",
                stopAdhanPendingIntent
            )
            // Add Post-Prayer Adhkar action button
            builder.addAction(
                android.R.drawable.ic_menu_agenda,
                "أذكار بعد الصلاة",
                adhkarPendingIntent
            )
        }

        try {
            val notificationId = if (isPreAlarm) NOTIFICATION_ID_PRE_PRAYER else NOTIFICATION_ID_PRAYER
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Notification permission might not be granted
        }
    }

    fun showAdhkarReminder(context: Context, isMorning: Boolean) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("initial_route", "adhkar")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            40,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isMorning) "أذكار الصباح ☀️" else "أذكار المساء 🌙"
        val message = if (isMorning) {
            "أصبحنا وأصبح الملك لله.. حان وقت أذكار الصباح، حصّن يومك بذكر الله."
        } else {
            "أمسينا وأمسى الملك لله.. حان وقت أذكار المساء، سكينة وحفظ لنفسك وأهلك."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ADHKAR)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ADHKAR, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    fun dismissNotification(context: Context, notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (_: Exception) {
        }
    }

    /**
     * Builds the foreground service notification for AdhanMediaPlayerService
     */
    fun buildAdhanServiceNotification(
        context: Context,
        prayerName: String
    ): android.app.Notification {
        createNotificationChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("initial_route", "prayer_times")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            50,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Stop Adhan Action Intent
        val stopAdhanIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_STOP_ADHAN
        }
        val stopAdhanPendingIntent = PendingIntent.getBroadcast(
            context,
            51,
            stopAdhanIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_ADHAN_SERVICE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🕌 يرفع الآن أذان $prayerName")
            .setContentText("الله أكبر، الله أكبر.. اضغط هنا لفتح التطبيق أو إيقاف لكتم الصوت")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "إيقاف الأذان",
                stopAdhanPendingIntent
            )
            .build()
    }
}
