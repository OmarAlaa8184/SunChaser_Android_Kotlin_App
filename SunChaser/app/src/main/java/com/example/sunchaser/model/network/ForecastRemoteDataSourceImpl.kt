package com.example.sunchaser.model.network

import com.example.sunchaser.model.weatherPojo.ForecastResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ForecastRemoteDataSourceImpl(private val weatherService: WeatherService):ForecastRemoteDataSource
{
    private val apiKey = "673e50cb30f2e71e6054d82400ed34f6" // Replace with your API key
    override suspend fun getFiveDayForecast(lat: Double, lon: Double): ForecastResponse
    {
        val result= withContext(Dispatchers.IO)
        {
            val response = weatherService.getFiveDayForecast(lat, lon, apiKey)
            if (response.isSuccessful)
            {
                response.body() ?: throw Exception("Empty response body")
            }
            else
            {
                throw Exception("API error: ${response.code()}")
            }
        }

        return result
    }
}


