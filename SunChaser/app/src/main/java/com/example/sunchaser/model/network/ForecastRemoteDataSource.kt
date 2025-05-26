package com.example.sunchaser.model.network

import com.example.sunchaser.model.weatherPojo.ForecastResponse

interface ForecastRemoteDataSource
{
    suspend fun getFiveDayForecast(lat: Double, lon: Double, units: String, lang: String): ForecastResponse
}