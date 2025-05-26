package com.example.sunchaser.model.weatherPojo

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ForecastResponse(
    val cod: String,
    val list: List<Forecast>,
    val city: City
)

data class Forecast(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>,
    val clouds: Clouds,
    val wind: Wind,
    val dt_txt: String
)

data class Main(
    val temp: Float,
    val feels_like: Float,
    val temp_min: Float,
    val temp_max: Float,
    val pressure: Int,
    val humidity: Int
)

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class Clouds(val all: Int)

data class Wind(val speed: Float, val deg: Int)

data class City(val name: String, val coord: Coord, val country: String)

data class Coord(val lat: Float, val lon: Float)


@Entity(tableName = "forecasts_table")
data class ForecastEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val cityName: String,
    val country: String,
    val latitude: Float,
    val longitude: Float,
    val dt: Long,
    val temp: Float,
    val feelsLike: Float,
    val tempMin: Float,
    val tempMax: Float,
    val pressure: Int,
    val humidity: Int,
    val weatherId: Int,
    val weatherMain: String,
    val weatherDescription: String,
    val weatherIcon: String,
    val clouds: Int,
    val windSpeed: Float,
    val windDeg: Int,
    val dtTxt: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "weather_alerts")
data class Alert(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val startTime: Long, // Unix timestamp
    val endTime: Long,   // Unix timestamp
    val alertType: AlertType,
    val isActive: Boolean = true
)
{
    enum class AlertType {
        NOTIFICATION,
        ALARM_SOUND
    }
}

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey
    val id: Int = 0,
    val locationSource: String = "GPS", // "GPS" or "MAP"
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "Current Location",
    val temperatureUnit: String = "Celsius", // Kelvin, Celsius, Fahrenheit
    val windSpeedUnit: String = "meters/sec", // meters/sec, miles/hour
    val language: String = "English" // English, Arabic
)

data class DailyForecast(
    val date: String,
    val avgTemp: Float,
    val weather: Weather
)

data class StatisticItem(val title: String, val value: String)


// Convert ForecastResponse to ForecastEntity
fun ForecastResponse.toEntityList(): List<ForecastEntity> {
    return list.map { forecast ->
        ForecastEntity(
            cityName = city.name,
            country = city.country,
            latitude = city.coord.lat,
            longitude = city.coord.lon,
            dt = forecast.dt,
            temp = forecast.main.temp,
            feelsLike = forecast.main.feels_like,
            tempMin = forecast.main.temp_min,
            tempMax = forecast.main.temp_max,
            pressure = forecast.main.pressure,
            humidity = forecast.main.humidity,
            weatherId = forecast.weather.first().id,
            weatherMain = forecast.weather.first().main,
            weatherDescription = forecast.weather.first().description,
            weatherIcon = forecast.weather.first().icon,
            clouds = forecast.clouds.all,
            windSpeed = forecast.wind.speed,
            windDeg = forecast.wind.deg,
            dtTxt = forecast.dt_txt
        )
    }
}

// Convert ForecastEntity to ForecastResponse
fun List<ForecastEntity>.toForecastResponse(): ForecastResponse
{
    val cityName = firstOrNull()?.cityName ?: ""
    val country = firstOrNull()?.country ?: ""
    val lat = firstOrNull()?.latitude ?: 0f
    val lon = firstOrNull()?.longitude ?: 0f

    return ForecastResponse(
        cod = "200",
        list = map { entity ->
            Forecast(
                dt = entity.dt,
                main = Main(
                    temp = entity.temp,
                    feels_like = entity.feelsLike,
                    temp_min = entity.tempMin,
                    temp_max = entity.tempMax,
                    pressure = entity.pressure,
                    humidity = entity.humidity
                ),
                weather = listOf(
                    Weather(
                        id = entity.weatherId,
                        main = entity.weatherMain,
                        description = entity.weatherDescription,
                        icon = entity.weatherIcon
                    )
                ),
                clouds = Clouds(all = entity.clouds),
                wind = Wind(speed = entity.windSpeed, deg = entity.windDeg),
                dt_txt = entity.dtTxt
            )
        },
        city = City(name = cityName, coord = Coord(lat, lon), country = country)
    )
}

// Group ForecastEntity by day and convert to DailyForecast-like data
 fun List<ForecastEntity>.toDailyForecasts(): List<DailyForecast>
 {
    val dailyGroups = this.groupBy { entity ->
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(entity.dt * 1000))
    }

    return dailyGroups.map { (date, entities) ->
        val avgTemp = entities.map { it.temp }.average().toFloat()
        val mostFrequentWeather = entities.groupBy { it.weatherIcon }
            .maxByOrNull { it.value.size }?.value?.first()
            ?: entities.first()
        DailyForecast(
            date = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)!!),
            avgTemp = avgTemp,
            weather = Weather(
                id = mostFrequentWeather.weatherId,
                main = mostFrequentWeather.weatherMain,
                description = mostFrequentWeather.weatherDescription,
                icon = mostFrequentWeather.weatherIcon
            )
        )
    }
        .take(5)
}

fun Long.toHourlyFormat(): String {
    return SimpleDateFormat("h a", Locale.getDefault()).format(Date(this * 1000))
}

/*object SettingsConstants {
    const val PREFS_NAME = "SettingsPrefs"
    const val KEY_LOCATION_SOURCE = "locationSource"
    const val KEY_TEMP_UNIT = "tempUnit"
    const val KEY_WIND_UNIT = "windUnit"
    const val KEY_LANGUAGE = "language"

    const val LOCATION_GPS = "gps"
    const val LOCATION_MAP = "map"
    const val TEMP_KELVIN = "Kelvin"
    const val TEMP_CELSIUS = "Celsius"
    const val TEMP_FAHRENHEIT = "Fahrenheit"
    const val WIND_MPS = "meters/sec"
    const val WIND_MPH = "miles/hour"
    const val LANGUAGE_ENGLISH = "English"
    const val LANGUAGE_ARABIC = "Arabic"

    val DEFAULT_LOCATION_SOURCE = LOCATION_GPS
    val DEFAULT_TEMP_UNIT = TEMP_CELSIUS
    val DEFAULT_WIND_UNIT = WIND_MPS
    val DEFAULT_LANGUAGE = LANGUAGE_ENGLISH
}*/