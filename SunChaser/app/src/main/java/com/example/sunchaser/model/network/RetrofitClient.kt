package com.example.productsusingviewbinding

import com.example.sunchaser.model.network.WeatherService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient
{
    private const val BASE_URL = "https://api.openweathermap.org/"

    private val retrofit=Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val retrofitService: WeatherService = retrofit.create(WeatherService::class.java)
}