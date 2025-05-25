package com.example.sunchaser.model.db

import com.example.sunchaser.model.weatherPojo.Alert


interface AlertLocalDataSource
{
//    suspend fun insertAlert(alert: WeatherAlert)
//    suspend fun deleteAlert(id: Int)
//    suspend fun getAllAlerts(): List<WeatherAlert>
//    suspend fun updateAlert(alert: WeatherAlert)

    suspend fun addAlert(alert: Alert)
    suspend fun updateAlert(alert: Alert)
    suspend fun deleteAlert(alert: Alert)
    suspend fun getActiveAlerts(): List<Alert>
    suspend fun getAllAlerts(): List<Alert>
    suspend fun getAlertById(alertId: Int): Alert?



}