package com.example.sunchaser.model.db

import android.content.Context
import com.example.sunchaser.model.weatherPojo.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsLocalDataSourceImpl(override val context: Context) : SettingsLocalDataSource {
    private val preferences = context.getSharedPreferences("WeatherAppPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ID = "settings_id"
        private const val KEY_LOCATION_SOURCE = "location_source"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_LOCATION_NAME = "location_name"
        private const val KEY_TEMPERATURE_UNIT = "temperature_unit"
        private const val KEY_WIND_SPEED_UNIT = "wind_speed_unit"
        private const val KEY_LANGUAGE = "language"
    }

    override suspend fun getSettings(): Settings? {
        return withContext(Dispatchers.IO) {
            if (!preferences.contains(KEY_ID)) {
                null
            } else {
                Settings(
                    id = preferences.getInt(KEY_ID, 0),
                    locationSource = preferences.getString(KEY_LOCATION_SOURCE, "GPS") ?: "GPS",
                    latitude = preferences.getFloat(KEY_LATITUDE, 0f).toDouble(),
                    longitude = preferences.getFloat(KEY_LONGITUDE, 0f).toDouble(),
                    locationName = preferences.getString(KEY_LOCATION_NAME, "Current Location") ?: "Current Location",
                    temperatureUnit = preferences.getString(KEY_TEMPERATURE_UNIT, "Celsius") ?: "Celsius",
                    windSpeedUnit = preferences.getString(KEY_WIND_SPEED_UNIT, "meters/sec") ?: "meters/sec",
                    language = preferences.getString(KEY_LANGUAGE, "English") ?: "English"
                )
            }
        }
    }

    override suspend fun saveSettings(settings: Settings) {
        withContext(Dispatchers.IO) {
            preferences.edit().apply {
                putInt(KEY_ID, settings.id)
                putString(KEY_LOCATION_SOURCE, settings.locationSource)
                putFloat(KEY_LATITUDE, settings.latitude.toFloat())
                putFloat(KEY_LONGITUDE, settings.longitude.toFloat())
                putString(KEY_LOCATION_NAME, settings.locationName)
                putString(KEY_TEMPERATURE_UNIT, settings.temperatureUnit)
                putString(KEY_WIND_SPEED_UNIT, settings.windSpeedUnit)
                putString(KEY_LANGUAGE, settings.language)
                apply()
            }
        }
    }

    override suspend fun updateSettings(settings: Settings) {
        saveSettings(settings)
    }
}