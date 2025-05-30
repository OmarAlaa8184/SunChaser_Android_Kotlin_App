package com.example.sunchaser.model.weatherPojo

interface ForecastRepository
{
    suspend fun getFiveDayForecast(lat: Double, lon: Double, units: String, lang: String): ForecastResponse
    suspend fun insertForecasts(forecasts: List<ForecastEntity>)
    suspend fun getAllStoredForecasts(): List<ForecastEntity>
    suspend fun deleteForecast(forecast: ForecastEntity)
    suspend fun getFavoriteLocations(): List<ForecastEntity>
    suspend fun getLocationByCoordinates(lat: Float, lon: Float): ForecastEntity?
}