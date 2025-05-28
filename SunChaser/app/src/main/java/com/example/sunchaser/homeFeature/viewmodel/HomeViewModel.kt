package com.example.sunchaser.homeFeature.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sunchaser.model.db.SettingsLocalDataSource
import com.example.sunchaser.model.network.Location
import com.example.sunchaser.model.weatherPojo.ForecastRepository
import com.example.sunchaser.model.weatherPojo.ForecastResponse
import com.example.sunchaser.model.weatherPojo.NetworkUtils
import com.example.sunchaser.model.weatherPojo.toForecastResponse
import kotlinx.coroutines.launch
import com.example.sunchaser.model.weatherPojo.Settings
import com.example.sunchaser.model.weatherPojo.SettingsManager


class HomeViewModelFactory(private val repository: ForecastRepository,private val location: Location,private val settingsLocalDataSource: SettingsLocalDataSource): ViewModelProvider.Factory
{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(HomeViewModel::class.java))
        {
            HomeViewModel(repository,location,settingsLocalDataSource) as T
        }
        else
        {
            throw IllegalArgumentException("ViewModel Class not found")
        }
    }
}

class HomeViewModel(
    private val forecastRepository: ForecastRepository,
    private val location: Location,
    private val settingsLocalDataSource: SettingsLocalDataSource
) : ViewModel()

{
    private val _forecast = MutableLiveData<ForecastResponse>()
    val forecast: LiveData<ForecastResponse> = _forecast

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _isOffline = MutableLiveData<Boolean>()
    val isOffline: LiveData<Boolean> = _isOffline

    init {

        viewModelScope.launch {
            // Observe settings changes
            SettingsManager.settingsFlow.collect { settings ->
                // Refetch forecast if units or location source changed
                if (settings.locationSource == "GPS")
                {
                    fetchForecastByLocation()
                }
                else
                {
                    fetchForecast(settings.latitude, settings.longitude)
                }
            }
        }

        // Initial fetch
        fetchForecastByLocation()
    }

    fun fetchForecast(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                if (NetworkUtils.isNetworkAvailable(location.context)) {
                    // Online: Fetch from API
                    val settings = settingsLocalDataSource.getSettings() ?: Settings()
                    val unit = when (settings.temperatureUnit) {
                        "Kelvin" -> "standard"
                        "Fahrenheit" -> "imperial"
                        else -> "metric"
                    }
                    val lang = if (settings.language == "Arabic") "ar" else "en"
                    val response = forecastRepository.getFiveDayForecast(lat, lon, unit, lang)
                    _forecast.postValue(response)
                    _isOffline.postValue(false)
                }
                else
                {
                    // Offline: Fetch cached data
                    val cachedForecasts = forecastRepository.getAllStoredForecasts()

                    if (cachedForecasts.isNotEmpty()) {
                        _forecast.postValue(cachedForecasts.toForecastResponse())
                        _isOffline.postValue(true)
                    } else {
                        _error.postValue("No internet connection and no cached data available")
                    }
                }
            } catch (e: Exception) {
                val cachedForecasts = forecastRepository.getAllStoredForecasts()
                if (cachedForecasts.isNotEmpty()) {
                    _forecast.postValue(cachedForecasts.toForecastResponse())
                    _isOffline.postValue(true)
                } else {
                    _error.postValue("No internet connection and no cached data: ${e.message}")
                }
            }
        }
    }

    fun fetchForecastByLocation() {
        viewModelScope.launch {
            val settings = settingsLocalDataSource.getSettings() ?: Settings()
            if (settings.locationSource == "GPS") {
                location.getLastLocation(
                    callback = { lat, lon ->
                        fetchForecast(lat, lon)
                    },
                    onError = { error ->
                        _error.postValue(error)
                        fetchForecast(30.0444, 31.2357) // Fallback: Cairo
                    }
                )
            } else {
                fetchForecast(settings.latitude, settings.longitude)
            }
        }
    }
}