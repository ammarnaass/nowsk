package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.R
import com.example.util.AdhanPlayer
import com.example.util.NusakkirNotificationHelper

/**
 * Foreground MediaPlayer Service that handles the playback of the Adhan audio file
 * with appropriate Audio Focus management, lifecycle handling, and notification controls.
 */
class AdhanMediaPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null

    private var isPausedDueToTransientLoss = false
    private var isDucked = false
    private var currentPrayerName: String = "الصلاة"

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "AudioFocus: GAIN")
                if (isPausedDueToTransientLoss) {
                    try {
                        mediaPlayer?.start()
                        isPausedDueToTransientLoss = false
                        AdhanPlayer.setPlayingState(true, currentPrayerName)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resuming MediaPlayer after focus gain", e)
                    }
                } else if (isDucked) {
                    try {
                        mediaPlayer?.setVolume(1.0f, 1.0f)
                        isDucked = false
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restoring volume after focus gain", e)
                    }
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "AudioFocus: PERMANENT LOSS -> stopping Adhan")
                stopAdhan()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "AudioFocus: TRANSIENT LOSS -> pausing Adhan")
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                        isPausedDueToTransientLoss = true
                        AdhanPlayer.setPlayingState(false, currentPrayerName)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error pausing MediaPlayer on transient focus loss", e)
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "AudioFocus: LOSS_TRANSIENT_CAN_DUCK -> lowering volume")
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.setVolume(0.2f, 0.2f)
                        isDucked = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error ducking MediaPlayer volume", e)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        instance = this
        Log.d(TAG, "AdhanMediaPlayerService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ADHAN
        val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "الصلاة"
        Log.d(TAG, "onStartCommand: action=$action, prayerName=$prayerName")

        when (action) {
            ACTION_START_ADHAN -> {
                currentPrayerName = prayerName
                startForegroundAdhan(prayerName)
                playAdhanAudio()
            }

            ACTION_STOP_ADHAN -> {
                stopAdhan()
            }

            else -> {
                stopAdhan()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundAdhan(prayerName: String) {
        try {
            val notification = NusakkirNotificationHelper.buildAdhanServiceNotification(
                context = this,
                prayerName = prayerName
            )
            startForeground(NusakkirNotificationHelper.NOTIFICATION_ID_ADHAN_SERVICE, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service notification", e)
        }
    }

    private fun playAdhanAudio() {
        // 1. Request Audio Focus
        val focusGranted = requestAudioFocus()
        if (!focusGranted) {
            Log.w(TAG, "Audio focus was NOT granted for Adhan playback")
            // Still proceed to attempt alarm playback, as prayer alarm is critical
        }

        // 2. Initialize and start MediaPlayer
        try {
            releaseMediaPlayer()

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)

                val afd = resources.openRawResourceFd(R.raw.adhan)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()

                setOnPreparedListener { mp ->
                    mp.start()
                    AdhanPlayer.setPlayingState(true, currentPrayerName)
                    Log.d(TAG, "MediaPlayer prepared and started Adhan for $currentPrayerName")
                }

                setOnCompletionListener {
                    Log.d(TAG, "Adhan audio playback completed naturally")
                    stopAdhan()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stopAdhan()
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing MediaPlayer for Adhan", e)
            stopAdhan()
        }
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()

            val res = am.requestAudioFocus(focusRequest!!)
            res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val res = am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN
            )
            res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error abandoning audio focus", e)
        } finally {
            focusRequest = null
        }
    }

    fun stopAdhan() {
        Log.d(TAG, "Stopping Adhan MediaPlayer Service")
        releaseMediaPlayer()
        abandonAudioFocus()
        AdhanPlayer.setPlayingState(false, null)

        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping foreground", e)
        }

        stopSelf()
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.reset()
                mp.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
            isPausedDueToTransientLoss = false
            isDucked = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
        abandonAudioFocus()
        AdhanPlayer.setPlayingState(false, null)
        instance = null
        Log.d(TAG, "AdhanMediaPlayerService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AdhanService"

        const val ACTION_START_ADHAN = "com.example.service.action.START_ADHAN"
        const val ACTION_STOP_ADHAN = "com.example.service.action.STOP_ADHAN"
        const val EXTRA_PRAYER_NAME = "com.example.service.extra.PRAYER_NAME"

        var instance: AdhanMediaPlayerService? = null
            private set
    }
}
