package com.example.sunchaser.model.weatherPojo

interface ForecastRepository
{
    suspend fun getFiveDayForecast(lat: Double, lon: Double): ForecastResponse
    suspend fun insertForecasts(forecasts: List<ForecastEntity>)
    suspend fun getAllStoredForecasts(): List<ForecastEntity>
    suspend fun deleteForecast(forecast: ForecastEntity)

}