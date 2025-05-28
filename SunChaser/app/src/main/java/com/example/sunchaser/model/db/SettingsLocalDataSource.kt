package com.example.sunchaser.model.db

import android.content.Context
import com.example.sunchaser.model.weatherPojo.Settings

interface SettingsLocalDataSource
{
    val context: Context // Add context property
    suspend fun getSettings(): Settings?
    suspend fun saveSettings(settings: Settings)
    suspend fun updateSettings(settings: Settings)
}