package com.example.sunchaser.model.db

import com.example.sunchaser.model.weatherPojo.Settings

interface SettingsLocalDataSource
{
    suspend fun getSettings(): Settings?
    suspend fun saveSettings(settings: Settings)
    suspend fun updateSettings(settings: Settings)
}