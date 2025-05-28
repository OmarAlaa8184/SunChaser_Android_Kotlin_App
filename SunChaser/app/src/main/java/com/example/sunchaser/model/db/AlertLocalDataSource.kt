package com.example.sunchaser.model.db

import com.example.sunchaser.model.weatherPojo.Alert


interface AlertLocalDataSource
{
//    suspend fun addAlert(alert: Alert)
//    suspend fun updateAlert(alert: Alert)
    suspend fun addAlert(alert: Alert, temperature: Float, unitLabel: String)
    suspend fun updateAlert(alert: Alert, temperature: Float, unitLabel: String)
    suspend fun deleteAlert(alert: Alert)
    suspend fun getActiveAlerts(): List<Alert>
    suspend fun getAllAlerts(): List<Alert>
    suspend fun getAlertById(alertId: Int): Alert?

}