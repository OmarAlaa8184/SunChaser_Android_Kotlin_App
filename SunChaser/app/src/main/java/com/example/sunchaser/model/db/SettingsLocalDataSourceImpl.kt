package com.example.sunchaser.model.db

import com.example.sunchaser.model.weatherPojo.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsLocalDataSourceImpl(private val settingsDao: SettingsDao):SettingsLocalDataSource
{
    override suspend fun getSettings(): Settings?
    {
        val result= withContext(Dispatchers.IO)
        {
            settingsDao.getSettings()
        }
        return result
    }
    override suspend fun saveSettings(settings: Settings)
    {
        withContext(Dispatchers.IO)
        {
            settingsDao.insert(settings)
        }
    }
    override suspend fun updateSettings(settings: Settings)
    {

        withContext(Dispatchers.IO)
        {
            settingsDao.update(settings)
        }

    }

}