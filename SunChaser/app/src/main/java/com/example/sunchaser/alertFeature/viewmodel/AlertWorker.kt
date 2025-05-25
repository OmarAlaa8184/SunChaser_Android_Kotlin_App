package com.example.sunchaser.alertFeature.viewmodel

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.sunchaser.R
import com.example.sunchaser.model.weatherPojo.Alert

class AlertWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

//    override suspend fun doWork(): Result {
//        val alertId = inputData.getInt("alert_id", -1)
//        val location = inputData.getString("location") ?: return Result.failure()
//        val alertType = inputData.getString("alert_type")?.let { Alert.AlertType.valueOf(it) }
//            ?: return Result.failure()
//        return try {
//            if (alertType == Alert.AlertType.ALARM_SOUND) {
//                startForegroundService(location)
//            } else {
//                showNotification(alertId, location)
//            }
//            Result.success()
//        } catch (e: Exception) {
//            Result.failure()
//        }
//    }

    override suspend fun doWork(): Result {
        val alertId = inputData.getInt("alert_id", -1)
        val location = inputData.getString("location") ?: return Result.failure()
        val alertType = inputData.getString("alert_type")?.let {
            try { Alert.AlertType.valueOf(it) } catch (e: IllegalArgumentException) { null }
        } ?: return Result.failure()

        return try {
            if (alertType == Alert.AlertType.ALARM_SOUND) {
                startForegroundService(location)
            } else {
                showNotification(alertId, location)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
    private fun startForegroundService(location: String) {
        val intent = Intent(applicationContext, AlertService::class.java).apply {
            putExtra("location", location)
        }
        applicationContext.startForegroundService(intent)
    }

    private fun showNotification(alertId: Int, location: String)
    {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        val notification = NotificationCompat.Builder(applicationContext, "weather_alerts")
            .setSmallIcon(R.drawable.ic_alert)
            .setContentTitle("Weather Alert")
            .setContentText("Alert for $location")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(alertId, notification)
    }
}