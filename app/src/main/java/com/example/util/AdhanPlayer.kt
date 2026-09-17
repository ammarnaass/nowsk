package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.service.AdhanMediaPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Adhan Player Facade:
 * Manages reactive UI state flows and coordinates with the foreground AdhanMediaPlayerService
 * for robust background playback, foreground notifications, and Audio Focus handling.
 */
object AdhanPlayer {
    private const val TAG = "AdhanPlayer"

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPrayerPlaying = MutableStateFlow<String?>(null)
    val currentPrayerPlaying: StateFlow<String?> = _currentPrayerPlaying.asStateFlow()

    private var appContext: Context? = null

    /**
     * Start playing the Adhan audio through the dedicated AdhanMediaPlayerService.
     */
    fun playAdhan(
        context: Context,
        prayerName: String = "الصلاة",
        onCompletion: (() -> Unit)? = null
    ) {
        appContext = context.applicationContext
        try {
            val serviceIntent = Intent(context, AdhanMediaPlayerService::class.java).apply {
                action = AdhanMediaPlayerService.ACTION_START_ADHAN
                putExtra(AdhanMediaPlayerService.EXTRA_PRAYER_NAME, prayerName)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d(TAG, "Sent ACTION_START_ADHAN to AdhanMediaPlayerService for $prayerName")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AdhanMediaPlayerService", e)
        }
    }

    /**
     * Stop Adhan playback and dismiss the service.
     */
    fun stopAdhan(context: Context? = null) {
        val ctx = context ?: appContext
        try {
            if (ctx != null) {
                val serviceIntent = Intent(ctx, AdhanMediaPlayerService::class.java).apply {
                    action = AdhanMediaPlayerService.ACTION_STOP_ADHAN
                }
                ctx.startService(serviceIntent)
            } else {
                AdhanMediaPlayerService.instance?.stopAdhan()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AdhanMediaPlayerService via intent, attempting direct stop", e)
            AdhanMediaPlayerService.instance?.stopAdhan()
        } finally {
            setPlayingState(false, null)
        }
    }

    /**
     * Internal update method called by AdhanMediaPlayerService to update reactive state.
     */
    fun setPlayingState(isPlaying: Boolean, prayerName: String?) {
        _isPlaying.value = isPlaying
        _currentPrayerPlaying.value = if (isPlaying) prayerName else null
    }

    fun isAdhanPlaying(): Boolean {
        return _isPlaying.value
    }
}
