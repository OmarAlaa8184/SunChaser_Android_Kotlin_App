package com.example.sunchaser.model.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.sunchaser.model.weatherPojo.Settings

@Dao
interface SettingsDao
{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: Settings)

    @Update
    suspend fun update(settings: Settings)

    @Query("SELECT * FROM settings LIMIT 1")
    suspend fun getSettings(): Settings?
}