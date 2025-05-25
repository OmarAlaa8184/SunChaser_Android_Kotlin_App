package com.example.sunchaser.model.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.sunchaser.model.weatherPojo.Alert

@Dao
interface AlertDao
{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: Alert)

    @Update
    suspend fun update(alert: Alert)

    @Delete
    suspend fun delete(alert: Alert)

    @Query("SELECT * FROM weather_alerts WHERE isActive = 1 ORDER BY startTime ASC")
    fun getActiveAlerts(): List<Alert>

    @Query("SELECT * FROM weather_alerts WHERE id = :alertId")
    suspend fun getAlertById(alertId: Int): Alert?

    @Query("SELECT * FROM weather_alerts")
    suspend fun getAll(): List<Alert>

}
