package com.example.sunchaser.model.db

import com.example.sunchaser.model.weatherPojo.ForecastEntity

interface ForecastLocalDataSource
{
    suspend fun insertForecasts(forecasts: List<ForecastEntity>)

    suspend fun getAllStoredForecasts(): List<ForecastEntity>

    suspend fun deleteForecast(forecast: ForecastEntity)

}
