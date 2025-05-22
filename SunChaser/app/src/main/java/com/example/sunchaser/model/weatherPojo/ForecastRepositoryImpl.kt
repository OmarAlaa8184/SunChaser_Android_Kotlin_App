package com.example.sunchaser.model.weatherPojo

import com.example.sunchaser.model.db.ForecastLocalDataSource
import com.example.sunchaser.model.network.ForecastRemoteDataSource

class ForecastRepositoryImpl private constructor(private var forecastRemoteDataSource: ForecastRemoteDataSource, private var forecastLocalDataSource: ForecastLocalDataSource):ForecastRepository
{
    companion object
    {
        @Volatile
        private var instance:ForecastRepositoryImpl?=null
        fun getInstance(forecastRemoteDataSource: ForecastRemoteDataSource, forecastLocalDataSource: ForecastLocalDataSource):ForecastRepositoryImpl
        {
            return instance?: synchronized(this)
            {
                instance ?: ForecastRepositoryImpl(forecastRemoteDataSource, forecastLocalDataSource).also {
                    instance = it
                }
            }
        }

    }

    override suspend fun getFiveDayForecast(lat: Double, lon: Double): ForecastResponse
    {
        return try
        {
            val response = forecastRemoteDataSource.getFiveDayForecast(lat, lon)
           // forecastLocalDataSource.insertForecasts(response.toEntityList())
            response
        }
        catch (e: Exception)
        {
            val cachedForecasts = forecastLocalDataSource.getAllStoredForecasts()
            if (cachedForecasts.isNotEmpty())
            {
                cachedForecasts.toForecastResponse()
            }
            else
            {
                throw e
            }
        }
    }

    override suspend fun insertForecasts(forecasts: List<ForecastEntity>)
    {

        forecastLocalDataSource.insertForecasts(forecasts)

    }

    override suspend fun getAllStoredForecasts(): List<ForecastEntity>
    {
       return forecastLocalDataSource.getAllStoredForecasts()
    }

    override suspend fun deleteForecast(forecast: ForecastEntity)
    {
        forecastLocalDataSource.deleteForecast(forecast)
    }

}