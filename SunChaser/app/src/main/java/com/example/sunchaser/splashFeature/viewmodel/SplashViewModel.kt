package com.example.sunchaser.splashFeature.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

    private val _navigateToMain = MutableLiveData<Boolean>()
    val navigateToMain: LiveData<Boolean> get() = _navigateToMain

    init {
        startTimer()
    }

    private fun startTimer()
    {
        viewModelScope.launch{
            delay(3000)  // Simulate splash delay (2 seconds)
            _navigateToMain.value = true
        }
    }
}
