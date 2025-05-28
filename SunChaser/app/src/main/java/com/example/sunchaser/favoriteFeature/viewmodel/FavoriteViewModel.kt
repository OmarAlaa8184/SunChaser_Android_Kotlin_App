package com.example.sunchaser.favoriteFeature.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sunchaser.model.db.SettingsLocalDataSource
import com.example.sunchaser.model.weatherPojo.ForecastEntity
import com.example.sunchaser.model.weatherPojo.ForecastRepository
import com.example.sunchaser.model.weatherPojo.ForecastResponse
import com.example.sunchaser.model.weatherPojo.NetworkUtils
import com.example.sunchaser.model.weatherPojo.Settings
import com.example.sunchaser.model.weatherPojo.toEntityList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class FavoriteViewModelFactory(private val repository: ForecastRepository,private val settingsLocalDataSource: SettingsLocalDataSource) : ViewModelProvider.Factory
{
    override fun <T : ViewModel> create(modelClass: Class<T>): T
    {
        if (modelClass.isAssignableFrom(FavoriteViewModel::class.java))
        {
            return FavoriteViewModel(repository,settingsLocalDataSource) as T
        }
        else
        {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}


class FavoriteViewModel(private val repository: ForecastRepository,private val settingsLocalDataSource: SettingsLocalDataSource) : ViewModel() {

    private val _favorites = MutableLiveData<List<ForecastEntity>>()
    val favorites: LiveData<List<ForecastEntity>> get() = _favorites

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String> get() = _toastMessage

    private val _settings = MutableLiveData<Settings>()
    val settings: LiveData<Settings> get() = _settings

    private val _isOffline = MutableLiveData<Boolean>()
    val isOffline: LiveData<Boolean> get() = _isOffline

    init {
        viewModelScope.launch {
            _settings.value = settingsLocalDataSource.getSettings() ?: Settings()
            loadFavorites()
        }
    }
    fun loadFavorites()
    {
        viewModelScope.launch{
            try
            {
                val favoriteLocations = withContext(Dispatchers.IO)
                {
                    repository.getFavoriteLocations()
                }
                _favorites.postValue(favoriteLocations)
                _isOffline.postValue(false)
            }
            catch (e: Exception)
            {
                //_error.postValue("Failed to load favorites: ${e.message}")

                val cachedFavorites = withContext(Dispatchers.IO) {
                    repository.getFavoriteLocations()
                }
                if (cachedFavorites.isNotEmpty()) {
                    _favorites.postValue(cachedFavorites)
                    _isOffline.postValue(true)
                } else {
                    _error.postValue("Failed to load favorites: ${e.message}")
                }
            }
        }
    }

   /* fun addFavoriteLocation(lat: Double, lng: Double)
    {
        viewModelScope.launch {
            try {
                // Check for duplicate
                val existing = withContext(Dispatchers.IO)
                {
                    repository.getLocationByCoordinates(lat.toFloat(), lng.toFloat())
                }
                if (existing != null)
                {
                    _toastMessage.postValue("Location already in favorites")
                    return@launch
                }
                // Fetch weather data with user-selected units and language
                val settings = settingsLocalDataSource.getSettings() ?: Settings()
                val unit = when (settings.temperatureUnit) {
                    "Kelvin" -> "standard"
                    "Fahrenheit" -> "imperial"
                    else -> "metric"
                }
                val lang = if (settings.language == "Arabic") "ar" else "en"

                // Fetch weather data
                val response: ForecastResponse = withContext(Dispatchers.IO)
                {
                    repository.getFiveDayForecast(lat, lng,unit,lang)
                }
                val forecast = response.toEntityList().firstOrNull()
                if (forecast == null) {
                    _error.postValue("No forecast data available")
                    return@launch
                }

                // Create and insert favorite
                val favorite = ForecastEntity(
                    cityName = response.city.name,
                    country = response.city.country,
                    latitude = lat.toFloat(),
                    longitude = lng.toFloat(),
                    dt = forecast.dt,
                    temp = forecast.temp,
                    feelsLike = forecast.feelsLike,
                    tempMin = forecast.tempMin,
                    tempMax = forecast.tempMax,
                    pressure = forecast.pressure,
                    humidity = forecast.humidity,
                    weatherId = forecast.weatherId,
                    weatherMain = forecast.weatherMain,
                    weatherDescription = forecast.weatherDescription,
                    weatherIcon = forecast.weatherIcon,
                    clouds = forecast.clouds,
                    windSpeed = forecast.windSpeed,
                    windDeg = forecast.windDeg,
                    dtTxt = forecast.dtTxt
                )
                withContext(Dispatchers.IO) {
                    repository.insertForecasts(listOf(favorite))
                }
                //loadFavorites()
                _toastMessage.postValue("Location added to favorites")

            } catch (e: Exception) {
                _error.postValue("Failed to add location: ${e.message}")
            }
        }
    }

    fun deleteFavorite(forecast: ForecastEntity) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO)
                {
                    repository.deleteForecast(forecast)
                }
                loadFavorites()
                _toastMessage.postValue("Location removed from favorites")

            } catch (e: Exception) {
                _error.postValue("Failed to delete location: ${e.message}")
            }
        }
    }*/
   fun addFavoriteLocation(lat: Double, lng: Double) {
       viewModelScope.launch {
           try {
               // Check for duplicate
               val existing = withContext(Dispatchers.IO) {
                   repository.getLocationByCoordinates(lat.toFloat(), lng.toFloat())
               }
               if (existing != null) {
                   _toastMessage.postValue("Location already in favorites")
                   return@launch
               }

               if (NetworkUtils.isNetworkAvailable(settingsLocalDataSource.context)) {
                   // Online: Fetch weather data
                   val settings = settingsLocalDataSource.getSettings() ?: Settings()
                   val unit = when (settings.temperatureUnit) {
                       "Kelvin" -> "standard"
                       "Fahrenheit" -> "imperial"
                       else -> "metric"
                   }
                   val lang = if (settings.language == "Arabic") "ar" else "en"

                   val response: ForecastResponse = withContext(Dispatchers.IO) {
                       repository.getFiveDayForecast(lat, lng, unit, lang)
                   }
                   val forecast = response.toEntityList().firstOrNull()
                   if (forecast == null) {
                       _error.postValue("No forecast data available")
                       return@launch
                   }

                   val favorite = ForecastEntity(
                       cityName = response.city.name,
                       country = response.city.country,
                       latitude = lat.toFloat(),
                       longitude = lng.toFloat(),
                       dt = forecast.dt,
                       temp = forecast.temp,
                       feelsLike = forecast.feelsLike,
                       tempMin = forecast.tempMin,
                       tempMax = forecast.tempMax,
                       pressure = forecast.pressure,
                       humidity = forecast.humidity,
                       weatherId = forecast.weatherId,
                       weatherMain = forecast.weatherMain,
                       weatherDescription = forecast.weatherDescription,
                       weatherIcon = forecast.weatherIcon,
                       clouds = forecast.clouds,
                       windSpeed = forecast.windSpeed,
                       windDeg = forecast.windDeg,
                       dtTxt = forecast.dtTxt
                   )
                   withContext(Dispatchers.IO) {
                       repository.insertForecasts(listOf(favorite))
                   }
                   loadFavorites()
                   _toastMessage.postValue("Location added to favorites")
                   _isOffline.postValue(false)
               } else {
                   _error.postValue("Cannot add new location in offline mode")
                   _isOffline.postValue(true)
               }
           } catch (e: Exception) {
               _error.postValue("Failed to add location: ${e.message}")
               _isOffline.postValue(true)
           }
       }
   }

    fun deleteFavorite(forecast: ForecastEntity) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteForecast(forecast)
                }
                loadFavorites()
                _toastMessage.postValue("Location removed from favorites")
                _isOffline.postValue(false)
            } catch (e: Exception) {
                _error.postValue("Failed to delete location: ${e.message}")
            }
        }
    }
}