package com.example.sunchaser.settingsFeature.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.example.sunchaser.model.db.SettingsLocalDataSource
import com.example.sunchaser.model.weatherPojo.Settings
import com.example.sunchaser.model.weatherPojo.SettingsManager
import getOrAwaitValue
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4



@RunWith(JUnit4::class)
class SettingsViewModelTest {

    // Rule to make LiveData execute synchronously
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Test dispatcher for coroutines
    private val testDispatcher = StandardTestDispatcher()

    // Mocks
    private lateinit var settingsLocalDataSource: SettingsLocalDataSource
    private lateinit var settingsObserver: Observer<Settings>
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        // Initialize MockK
        MockKAnnotations.init(this, relaxed = true)
        settingsLocalDataSource = mockk()
        settingsObserver = mockk(relaxed = true)

        // Mock SettingsManager (singleton/object)
        mockkObject(SettingsManager)

        // Mock getSettings to avoid exception during ViewModel initialization
        coEvery { settingsLocalDataSource.getSettings() } returns Settings()

        // Set the main dispatcher for testing
        Dispatchers.setMain(testDispatcher)

        // Initialize ViewModel
        viewModel = SettingsViewModel(settingsLocalDataSource)

        // Advance dispatcher to complete initialization
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun saveSettings_updatesLiveData_savesToDataSource_notifiesSettingsManager() = runTest {
        // Given: A settings object and mocked behavior
        val testSettings = Settings(
            id = 1,
            locationSource = "GPS",
            latitude = 25.0,
            longitude = 55.0,
            locationName = "Test City",
            temperatureUnit = "Celsius",
            windSpeedUnit = "meters/sec",
            language = "English"
        )
        coEvery { settingsLocalDataSource.saveSettings(testSettings) } returns Unit

        // Observe LiveData
        viewModel.settings.observeForever(settingsObserver)

        // When: saveSettings is called
        viewModel.saveSettings(testSettings)
        advanceUntilIdle() // Process coroutines

        // Then: Verify interactions and state
        coVerify { settingsLocalDataSource.saveSettings(testSettings) }
        verify { SettingsManager.updateSettings(testSettings) }
        verify { settingsObserver.onChanged(testSettings) }
        assertThat(viewModel.settings.getOrAwaitValue(), `is`(testSettings))
    }
}