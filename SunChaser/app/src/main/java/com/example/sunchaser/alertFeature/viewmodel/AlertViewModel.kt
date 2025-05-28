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
import com.example.productsusingviewbinding.RetrofitClient
import com.example.sunchaser.model.db.AlertLocalDataSource
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.example.sunchaser.alertFeature.viewmodel.AlertWorker
import com.example.sunchaser.favoriteFeature.viewmodel.FavoriteViewModel
import com.example.sunchaser.model.db.ForecastDatabase
import com.example.sunchaser.model.db.ForecastLocalDataSourceImpl
import com.example.sunchaser.model.db.SettingsLocalDataSourceImpl
import com.example.sunchaser.model.network.ForecastRemoteDataSourceImpl
import com.example.sunchaser.model.weatherPojo.Alert
import com.example.sunchaser.model.weatherPojo.ForecastRepository
import com.example.sunchaser.model.weatherPojo.ForecastRepositoryImpl
import com.example.sunchaser.model.weatherPojo.Settings
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

    private val _temperatures = MutableLiveData<Map<Int, Float>>()
    val temperatures: LiveData<Map<Int, Float>> get() = _temperatures


    private val forecastRepository = ForecastRepositoryImpl.getInstance(
        ForecastRemoteDataSourceImpl(RetrofitClient.retrofitService),
        ForecastLocalDataSourceImpl(ForecastDatabase.getInstance(context).forecastDao())
    )
    init {
        viewModelScope.launch {
            _alerts.value = alertLocalDataSource.getActiveAlerts()
            fetchTemperaturesForAlerts()
        }
    }

    fun addAlert(
        locationName: String,
        lat: Double,
        lon: Double,
        startTime: Long,
        endTime: Long,
        alertType: Alert.AlertType,
        temperature: Float
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
            val settings = SettingsLocalDataSourceImpl(context).getSettings() ?: Settings()
            val unitLabel = when (settings.temperatureUnit) {
                "Kelvin" -> "K"
                "F" -> "°F"
                else -> "°C"
            }
            alertLocalDataSource.addAlert(alert,temperature,unitLabel)
            _alerts.value = alertLocalDataSource.getActiveAlerts()

            val currentTemps = _temperatures.value?.toMutableMap() ?: mutableMapOf()
            currentTemps[alert.id] = temperature
            _temperatures.postValue(currentTemps)
            fetchTemperaturesForAlerts()

        }
    }


    private fun fetchTemperaturesForAlerts()
    {
        viewModelScope.launch {
            val alerts = alertLocalDataSource.getActiveAlerts()
            val settings = SettingsLocalDataSourceImpl(context).getSettings() ?: Settings()
            val unit = when (settings.temperatureUnit)
            {
                "Kelvin" -> "standard"
                "Fahrenheit" -> "imperial"
                else -> "metric"
            }
            val lang = if (settings.language == "Arabic") "ar" else "en"
            val tempMap = mutableMapOf<Int, Float>()
            alerts.forEach { alert ->
                try {
                    val response = forecastRepository.getFiveDayForecast(
                        alert.latitude, alert.longitude, unit, lang
                    )
                    // Get the first forecast entry (closest to current time)
                    val currentTemp = response.list.firstOrNull()?.main?.temp ?: 0f
                    tempMap[alert.id] = currentTemp
                }
                catch (e: Exception)
                {
                    Log.e("AlertViewModel", "Error fetching temp for alert ${alert.id}", e)
                    tempMap[alert.id] = 0f // Fallback temperature
                }
            }
            _temperatures.postValue(tempMap)
        }
    }


   /* fun toggleAlert(id: Int, isActive: Boolean) {
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
    }*/

    fun toggleAlert(id: Int, isActive: Boolean) {
       viewModelScope.launch(Dispatchers.IO) {
           try {
               val targetAlert = alertLocalDataSource.getAlertById(id)
               targetAlert?.let { alert ->
                   if (alert.isActive != isActive) {
                       val updatedAlert = alert.copy(isActive = isActive)
                       val settings = SettingsLocalDataSourceImpl(context).getSettings() ?: Settings()
                       val unitLabel = when (settings.temperatureUnit) {
                           "Kelvin" -> "K"
                           "F" -> "°F"
                           else -> "°C"
                       }
                       val temperature = _temperatures.value?.get(alert.id) ?: 0f
                       alertLocalDataSource.updateAlert(updatedAlert, temperature, unitLabel)
                       withContext(Dispatchers.Main) {
                           _alerts.value = alertLocalDataSource.getAllAlerts()
                           fetchTemperaturesForAlerts()
                       }
                       if (isActive) {
                           scheduleAlert(updatedAlert)
                       } else {
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
            fetchTemperaturesForAlerts()
        }
    }


    private fun cancelAlert(id: Int) {
        // Cancel any pending WorkManager requests for this alert
        WorkManager.getInstance(context).cancelUniqueWork("alert_$id")
    }


    private suspend fun scheduleAlert(alert: Alert) {
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

        // Get temperature for this alert
        val temperature = _temperatures.value?.get(alert.id) ?: 0f
        // Get temperature unit from settings
        val settings = SettingsLocalDataSourceImpl(context).getSettings() ?: Settings()
        val unitLabel = when (settings.temperatureUnit) {
            "Kelvin" -> "K"
            "Fahrenheit" -> "°F"
            else -> "°C"
        }
        // Prepare data for Worker
        val data = workDataOf(
            "alertId" to alert.id.toString(),
            "location" to alert.locationName,
            "type" to alert.alertType.name,
            "endTime" to alert.endTime.toString(),
            "temperature" to temperature,
            "unitLabel" to unitLabel
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
