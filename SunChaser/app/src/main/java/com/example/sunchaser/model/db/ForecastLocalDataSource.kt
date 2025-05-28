package com.example.sunchaser.model.db

import com.example.sunchaser.model.weatherPojo.ForecastEntity

interface ForecastLocalDataSource
{
    suspend fun insertForecasts(forecasts: List<ForecastEntity>)

    suspend fun getAllStoredForecasts(): List<ForecastEntity>

    suspend fun deleteForecast(forecast: ForecastEntity)

    suspend fun getFavoriteLocations(): List<ForecastEntity>

    suspend fun getLocationByCoordinates(lat: Float, lon: Float): ForecastEntity?

   // suspend fun getLatestForecastByCoordinates(lat: Float, lon: Float): ForecastEntity?
}
