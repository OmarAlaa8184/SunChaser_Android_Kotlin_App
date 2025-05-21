package com.example.sunchaser.homeFeature.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sunchaser.model.network.Location
import com.example.sunchaser.model.weatherPojo.ForecastRepository
import com.example.sunchaser.model.weatherPojo.ForecastResponse
import com.example.sunchaser.model.weatherPojo.toEntityList
import com.example.sunchaser.model.weatherPojo.toForecastResponse
import kotlinx.coroutines.launch


class HomeViewModelFactory(private val repository: ForecastRepository,private val location: Location): ViewModelProvider.Factory
{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(HomeViewModel::class.java))
        {
            HomeViewModel(repository,location ) as T
        }
        else
        {
            throw IllegalArgumentException("ViewModel Class not found")
        }
    }
}

class HomeViewModel (private val forecastRepository: ForecastRepository, private val location: Location) : ViewModel()
{
    private val _forecast = MutableLiveData<ForecastResponse>()
    val forecast: LiveData<ForecastResponse> = _forecast

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun fetchForecast(lat: Double, lon: Double) {
        viewModelScope.launch {
            try
            {
                val response = forecastRepository.getFiveDayForecast(lat, lon)
                forecastRepository.insertForecasts(response.toEntityList())
                _forecast.postValue(response)
            }
            catch (e: Exception)
            {
                val cachedForecasts = forecastRepository.getAllStoredForecasts()
                if (cachedForecasts.isNotEmpty())
                {
                    _forecast.postValue(cachedForecasts.toForecastResponse())
                }
                else
                {
                    _error.postValue(e.message ?: "Unknown error")
                }
            }
        }
    }

    fun fetchForecastByLocation()
    {
        location.getLastLocation(
            callback = { lat, lon ->
                fetchForecast(lat, lon)
            },
            onError = { error ->
                _error.postValue(error)
                fetchForecast(30.0444, 31.2357) // Fallback: Cairo
            }
        )
    }
}