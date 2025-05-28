package com.example.sunchaser.model.weatherPojo

import com.example.sunchaser.model.db.ForecastLocalDataSource
import com.example.sunchaser.model.network.ForecastRemoteDataSource


class FakeForecastRemoteDataSource : ForecastRemoteDataSource
{
    override suspend fun getFiveDayForecast(lat: Double, lon: Double, units: String, lang: String): ForecastResponse {
        throw NotImplementedError("Not used in these tests")
    }
}

class FakeForecastLocalDataSource : ForecastLocalDataSource {
    private val forecasts = mutableListOf<ForecastEntity>()

    fun addForecast(forecast: ForecastEntity) {
        forecasts.add(forecast)
    }

    fun getAllForecasts(): List<ForecastEntity> = forecasts.toList()

    override suspend fun insertForecasts(forecasts: List<ForecastEntity>) {
        this.forecasts.addAll(forecasts)
    }

    override suspend fun getAllStoredForecasts(): List<ForecastEntity> {
        val currentTime = System.currentTimeMillis()
        return forecasts.filter {
            it.timestamp > currentTime - 60 * 60 * 1000 // 1 hour cache
        }
    }

    override suspend fun deleteForecast(forecast: ForecastEntity) {
        forecasts.remove(forecast)
    }

    override suspend fun getFavoriteLocations(): List<ForecastEntity> {
        throw NotImplementedError("Not used in these tests")
    }

    override suspend fun getLocationByCoordinates(lat: Float, lon: Float): ForecastEntity? {
        throw NotImplementedError("Not used in these tests")
    }
}