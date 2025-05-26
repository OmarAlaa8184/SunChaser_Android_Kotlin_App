package com.example.sunchaser.alertFeature.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.sunchaser.model.db.AlertLocalDataSource
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.example.sunchaser.alertFeature.viewmodel.AlertWorker
import com.example.sunchaser.favoriteFeature.viewmodel.FavoriteViewModel
import com.example.sunchaser.model.weatherPojo.Alert
import com.example.sunchaser.model.weatherPojo.ForecastRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlertViewModelFactory(val alertLocalDataSource: AlertLocalDataSource,val context: Context) : ViewModelProvider.Factory
{
    override fun <T : ViewModel> create(modelClass: Class<T>): T
    {
        if (modelClass.isAssignableFrom(AlertViewModel::class.java))
        {
            return AlertViewModel(alertLocalDataSource,context) as T
        }
        else
        {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

class AlertViewModel(private val alertLocalDataSource: AlertLocalDataSource, private val context: Context) : ViewModel() {


    private val _alerts = MutableLiveData<List<Alert>>()
    val alerts: LiveData<List<Alert>> get() = _alerts

    init {
        viewModelScope.launch {
            _alerts.value = alertLocalDataSource.getActiveAlerts()
        }
    }

    fun addAlert(
        locationName: String,
        lat: Double,
        lon: Double,
        startTime: Long,
        endTime: Long,
        alertType: Alert.AlertType
    ) {
        viewModelScope.launch {
            val alert = Alert(
                locationName = locationName,
                latitude = lat,
                longitude = lon,
                startTime = startTime,
                endTime = endTime,
                alertType = alertType
            )
            alertLocalDataSource.addAlert(alert)
            _alerts.value = alertLocalDataSource.getActiveAlerts()
        }
    }

    //    fun toggleAlert(id: Int, isActive: Boolean)
//    {
//        viewModelScope.launch{
//            val allAlerts = alertLocalDataSource.getActiveAlerts()
//            val targetAlert = allAlerts.find { it.id == id }
//            targetAlert?.let {
//                val updatedAlert = it.copy(isActive = isActive)
//                alertLocalDataSource.updateAlert(updatedAlert)
//                _alerts.value = alertLocalDataSource.getAlertById(id)
//            }
//        }
//    }
    fun toggleAlert(id: Int, isActive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Get the specific alert directly instead of all alerts
                val targetAlert = alertLocalDataSource.getAlertById(id)

                targetAlert?.let { alert ->
                    // 2. Only update if the state is actually changing
                    if (alert.isActive != isActive) {
                        val updatedAlert = alert.copy(isActive = isActive)

                        // 3. Perform update and refresh in a transaction
                        alertLocalDataSource.updateAlert(updatedAlert)

                        // 4. Update LiveData on main thread
                        withContext(Dispatchers.Main)
                        {
                            _alerts.value = alertLocalDataSource.getAllAlerts()
                        }

                        // 5. Handle scheduling/unscheduling based on toggle
                        if (isActive)
                        {
                            scheduleAlert(updatedAlert)
                        }
                        else
                        {
                            cancelAlert(updatedAlert.id)
                        }
                    }
                } ?: run {
                    Log.w("AlertViewModel", "Alert with id $id not found")
                }
            } catch (e: Exception) {
                Log.e("AlertViewModel", "Error toggling alert $id", e)

            }
        }
    }

    fun deleteAlert(alert: Alert) {
        viewModelScope.launch {
            alertLocalDataSource.deleteAlert(alert)
            _alerts.value = alertLocalDataSource.getActiveAlerts()
        }
    }

    private fun cancelAlert(id: Int) {
        // Cancel any pending WorkManager requests for this alert
        WorkManager.getInstance(context).cancelUniqueWork("alert_$id")
    }
    private fun scheduleAlert(alert: Alert) {
        // Validate alert first
        if (!alert.isActive) {
            Log.d("AlertViewModel", "Alert ${alert.id} is inactive - not scheduling")
            return
        }

        val now = System.currentTimeMillis()
        val delayMillis = alert.startTime - now

        // Don't schedule if alert time has passed
        if (delayMillis <= 0) {
            Log.w("AlertViewModel", "Alert ${alert.id} start time is in the past")
            return
        }

        // Prepare data for Worker
        val data = workDataOf(
            "alertId" to alert.id.toString(),
            "type" to alert.alertType.name,
            "endTime" to alert.endTime.toString()
        )

        // Create unique work request
        val workRequest = OneTimeWorkRequestBuilder<AlertWorker>()
            .setInputData(data)
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .addTag("weather_alert_${alert.id}")
            .build()

        // Enqueue with unique name to prevent duplicates
        WorkManager.getInstance(context).enqueueUniqueWork(
            "alert_${alert.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d("AlertViewModel", "Scheduled alert ${alert.id} to trigger in ${delayMillis/1000} seconds")
    }
}
