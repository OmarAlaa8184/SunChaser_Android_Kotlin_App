package com.example.sunchaser.settingsFeature.viewmodel


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sunchaser.model.db.SettingsLocalDataSource
import com.example.sunchaser.model.weatherPojo.Settings
import com.example.sunchaser.model.weatherPojo.SettingsManager
import kotlinx.coroutines.launch

class SettingsViewModelFactory(private val settingsLocalDataSource: SettingsLocalDataSource): ViewModelProvider.Factory
{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(SettingsViewModel::class.java))
        {
             SettingsViewModel(settingsLocalDataSource) as T
        }
        else
        {
            throw IllegalArgumentException("ViewModel Class not found")
        }
    }
}


class SettingsViewModel (private val settingsLocalDataSource: SettingsLocalDataSource): ViewModel()
{
    private val _settings = MutableLiveData<Settings>()
    val settings: LiveData<Settings> get() = _settings

    init {
        viewModelScope.launch {
            val initialSettings = settingsLocalDataSource.getSettings() ?: Settings()
            _settings.value = initialSettings
            SettingsManager.updateSettings(initialSettings)
        }
    }

    fun saveSettings(settings: Settings) {

        viewModelScope.launch {
            settingsLocalDataSource.saveSettings(settings)
            _settings.value = settings
            SettingsManager.updateSettings(settings)
        }
    }

    fun updateSettings(settings: Settings) {

        viewModelScope.launch {
            settingsLocalDataSource.updateSettings(settings)
            _settings.value = settings
            SettingsManager.updateSettings(settings)
        }
    }

}