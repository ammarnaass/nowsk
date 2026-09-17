package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.util.PrayerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "Boot or package update detected. Rescheduling prayer alarms...")
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                PrayerScheduler.rescheduleFromDatabase(appContext)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
