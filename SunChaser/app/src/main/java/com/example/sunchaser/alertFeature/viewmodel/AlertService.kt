package com.example.sunchaser.alertFeature.viewmodel

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.sunchaser.R


class AlertService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private val notificationId = 101

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound)?.apply {
                isLooping = true
            }
        } catch (e: Exception) {
            // Log error if resource is missing
            Log.e("AlertService", "Failed to initialize MediaPlayer", e)
        }
    }

   /* @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val location = intent?.getStringExtra("location") ?: "Unknown location"
        val notification = createNotification(location)
        startForeground(notificationId, notification)

        if (mediaPlayer != null) {
            mediaPlayer?.start()
        }
        else
        {
            // Fallback: show notification if sound fails
            showFallbackNotification(location)
        }

        // Stop service after a reasonable duration (e.g., 5 minutes) if not stopped by receiver
        Handler(Looper.getMainLooper()).postDelayed({
            stopSelf()
        }, 5 * 60 * 1000L)

        return START_NOT_STICKY
    }*/

    @SuppressLint("ForegroundServiceType")
   override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
       val location = intent?.getStringExtra("location") ?: "Unknown location"
       val temperature = intent?.getFloatExtra("temperature", 0f) ?: 0f
       val unitLabel = intent?.getStringExtra("unitLabel") ?: "°C"
       val endTime = intent?.getLongExtra("endTime", 0L) ?: (System.currentTimeMillis() + 5 * 60 * 1000L)
       val notification = createNotification(location, temperature, unitLabel)
       startForeground(notificationId, notification)
       Log.d("AlertService", "Foreground service started for $location")

       if (mediaPlayer != null) {
           try {
               mediaPlayer?.start()
               Log.d("AlertService", "MediaPlayer started")
           }
           catch (e: Exception)
           {
               Log.e("AlertService", "Failed to start MediaPlayer", e)
               showFallbackNotification(location, temperature, unitLabel)
           }
       }
       else
       {
           showFallbackNotification(location, temperature, unitLabel)
       }

       // Stop service when endTime is reached
       val now = System.currentTimeMillis()
       val delay = if (endTime > now) endTime - now else 5 * 60 * 1000L
       Handler(Looper.getMainLooper()).postDelayed({
           stopSelf()
           Log.d("AlertService", "Service stopped after duration")
       }, delay)

       return START_NOT_STICKY
   }
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "alarm_channel",
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Weather alarm notifications"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /*private fun createNotification(location: String): Notification {
        val stopIntent = Intent(this, AlertReceiver::class.java).apply {
            action = "STOP_ALARM"
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "alarm_channel")
            .setContentTitle("Weather Alarm Active")
            .setContentText("Alert for $location")
            .setSmallIcon(R.drawable.ic_alert)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun showFallbackNotification(location: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "weather_alerts")
            .setContentTitle("Weather Alert")
            .setContentText("Alert for $location (Sound unavailable)")
            .setSmallIcon(R.drawable.ic_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId + 1, notification)
    }*/

    private fun createNotification(location: String, temperature: Float, unitLabel: String): Notification
    {
        val stopIntent = Intent(this, AlertReceiver::class.java).apply {
            action = "STOP_ALARM"
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "alarm_channel")
            .setContentTitle("Weather Alarm Active")
            .setContentText("Alert for $location, ${temperature.toInt()}$unitLabel")
            .setSmallIcon(R.drawable.ic_alert)
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun showFallbackNotification(location: String, temperature: Float, unitLabel: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "weather_alerts")
            .setContentTitle("Weather Alert")
            .setContentText("Alert for $location, ${temperature.toInt()}$unitLabel (Sound unavailable)")
            .setSmallIcon(R.drawable.ic_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId + 1, notification)
        Log.d("AlertService", "Fallback notification shown for $location")
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}