package com.example.sunchaser.model.db

import com.example.sunchaser.model.weatherPojo.ForecastEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ForecastLocalDataSourceImpl(val forecastDao: ForecastDao) : ForecastLocalDataSource
{
    private val CACHE_EXPIRY_MS = 60 * 60 * 1000 // 1 hour

    override suspend fun insertForecasts(forecasts: List<ForecastEntity>)
    {
        withContext(Dispatchers.IO)
        {
            forecasts.forEach { forecasts ->
                forecastDao.insert(forecasts)
            }
        }
    }

    override suspend fun getAllStoredForecasts(): List<ForecastEntity>
    {
       val result= withContext(Dispatchers.IO)
       {
           val forecasts=forecastDao.getAllStoredForecasts().filter {
               it.timestamp > System.currentTimeMillis()-CACHE_EXPIRY_MS
           }
           forecasts
       }
        return result
    }

    override suspend fun deleteForecast(forecast: ForecastEntity)
    {
        withContext(Dispatchers.IO)
        {
            forecastDao.delete(forecast)
        }
    }



}