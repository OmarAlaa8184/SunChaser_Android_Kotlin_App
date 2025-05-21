package com.example.sunchaser.model.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.sunchaser.model.weatherPojo.ForecastEntity

@Dao
interface ForecastDao
{

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(forecast: ForecastEntity)

    @Query("SELECT * FROM forecasts_table")
    suspend fun getAllStoredForecasts(): List<ForecastEntity>

    @Delete
    suspend fun delete(forecast: ForecastEntity)
}