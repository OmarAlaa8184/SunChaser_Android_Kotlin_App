package com.example.sunchaser.model.db

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.sunchaser.alertFeature.viewmodel.AlertWorker
import com.example.sunchaser.model.weatherPojo.Alert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AlertLocalDataSourceImpl(val alertDao: AlertDao,private val context: Context) :AlertLocalDataSource
{

    override suspend fun addAlert(alert: Alert, temperature: Float, unitLabel: String)
    {
        withContext(Dispatchers.IO)
        {
            alertDao.insert(alert)
            if(alert.isActive)
            {
                scheduleAlertWithWorkManager(alert,temperature,unitLabel)
            }
        }
    }

    override suspend fun updateAlert(alert: Alert, temperature: Float, unitLabel: String)
    {
        withContext(Dispatchers.IO)
        {
            alertDao.update(alert)
            if(alert.isActive)
            {
                scheduleAlertWithWorkManager(alert,temperature,unitLabel)
            }
            else
            {
                cancelAlert(alert)
            }
        }

    }

    override suspend fun deleteAlert(alert: Alert)
    {
        withContext(Dispatchers.IO)
        {
            alertDao.delete(alert)
            cancelAlert(alert)
        }
    }

    override suspend fun getActiveAlerts(): List<Alert>
    {

        val result=withContext(Dispatchers.IO)
        {
            alertDao.getActiveAlerts()
        }
        return result
    }

    override suspend fun getAllAlerts(): List<Alert>
    {
        val result = withContext(Dispatchers.IO)
        {
            alertDao.getAll()
        }
        return result

    }

    override suspend fun getAlertById(alertId: Int): Alert?
    {

        val result= withContext(Dispatchers.IO)
        {
            alertDao.getAlertById(alertId)
        }
        return result

    }

    private fun scheduleAlertWithWorkManager(alert: Alert,temperature: Float, unitLabel: String)
    {
        val now = System.currentTimeMillis()
        if (alert.startTime <= now) return // Skip past alerts


        val data = workDataOf(
            "alert_id" to alert.id,
            "location" to alert.locationName,
            "alert_type" to alert.alertType.name,
            "temperature" to temperature,
            "unitLabel" to unitLabel

        )
        val delay = alert.startTime - now

        // Add constraints to allow execution in Doze mode
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true) // Optional: adjust based on needs
            .build()

        val request = OneTimeWorkRequestBuilder<AlertWorker>()
            .setInputData(data)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag("alert_${alert.id}")
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "alert_${alert.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun cancelAlert(alert: Alert) {
        WorkManager.getInstance(context).cancelAllWorkByTag("alert_${alert.id}")
    }

}