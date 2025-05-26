package com.example.sunchaser.model.weatherPojo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SettingsManager {
    private val _settingsFlow = MutableSharedFlow<Settings>(replay = 1)
    val settingsFlow: SharedFlow<Settings> = _settingsFlow.asSharedFlow()

    private val _currentSettings = MutableLiveData<Settings>()
    val currentSettings: LiveData<Settings> get() = _currentSettings

    fun updateSettings(settings: Settings) {
        _currentSettings.postValue(settings)
        _settingsFlow.tryEmit(settings)
    }
}
