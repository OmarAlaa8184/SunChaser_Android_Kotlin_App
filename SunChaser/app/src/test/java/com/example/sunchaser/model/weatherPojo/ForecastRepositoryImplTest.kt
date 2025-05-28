package com.example.sunchaser.model.weatherPojo

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4



@RunWith(JUnit4::class)
class ForecastRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ForecastRepositoryImpl
    private lateinit var fakeRemoteDataSource: FakeForecastRemoteDataSource
    private lateinit var fakeLocalDataSource: FakeForecastLocalDataSource

    @Before
    fun setup() {

        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)
        fakeRemoteDataSource = mockk()
        fakeLocalDataSource = mockk()
        repository = ForecastRepositoryImpl(fakeRemoteDataSource, fakeLocalDataSource)
        testDispatcher.scheduler.advanceUntilIdle()

    }

    @Test
    fun insertForecasts_Should_Delegate_To_LocalDatasource() = runTest {
        // Given
        val testForecasts = listOf(
            ForecastEntity(
                id = 1,
                cityName = "Test City",
                country = "TC",
                latitude = 30f,
                longitude = 30f,
                dt = System.currentTimeMillis(),
                temp = 25f,
                feelsLike = 26f,
                tempMin = 24f,
                tempMax = 27f,
                pressure = 1013,
                humidity = 50,
                weatherId = 800,
                weatherMain = "Clear",
                weatherDescription = "clear sky",
                weatherIcon = "01d",
                clouds = 0,
                windSpeed = 5f,
                windDeg = 180,
                dtTxt = "2025-05-28 12:00:00"
            )
        )
        coEvery { fakeLocalDataSource.insertForecasts(testForecasts) } returns Unit

        // When
        repository.insertForecasts(testForecasts)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { fakeLocalDataSource.insertForecasts(testForecasts) }
    }

    @Test
    fun getAllStoredForecasts_should_return_filtered_forecasts_from_LocalDatasource() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val validForecast = ForecastEntity(
            id = 1,
            cityName = "Valid",
            country = "V",
            latitude = 30f,
            longitude = 30f,
            dt = currentTime,
            temp = 25f,
            feelsLike = 26f,
            tempMin = 24f,
            tempMax = 27f,
            pressure = 1013,
            humidity = 50,
            weatherId = 800,
            weatherMain = "Clear",
            weatherDescription = "clear sky",
            weatherIcon = "01d",
            clouds = 0,
            windSpeed = 5f,
            windDeg = 180,
            dtTxt = "2025-05-28 12:00:00",
            timestamp = currentTime - 1000
        )
        val expiredForecast = ForecastEntity(
            id = 2,
            cityName = "Expired",
            country = "E",
            latitude = 30f,
            longitude = 30f,
            dt = currentTime,
            temp = 25f,
            feelsLike = 26f,
            tempMin = 24f,
            tempMax = 27f,
            pressure = 1013,
            humidity = 50,
            weatherId = 800,
            weatherMain = "Clear",
            weatherDescription = "clear sky",
            weatherIcon = "01d",
            clouds = 0,
            windSpeed = 5f,
            windDeg = 180,
            dtTxt = "2025-05-28 12:00:00",
            timestamp = currentTime - 2 * 60 * 60 * 1000
        )
        coEvery { fakeLocalDataSource.getAllStoredForecasts() } returns listOf(validForecast)

        // When
        val result = repository.getAllStoredForecasts()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { fakeLocalDataSource.getAllStoredForecasts() }
        assertThat(result, hasSize(1))
        assertThat(result[0].cityName, `is`("Valid"))
    }

    @Test
    fun getAllStoredForecasts_should_return_empty_list_when_no_valid_cached_data() = runTest {
        // Given
        coEvery { fakeLocalDataSource.getAllStoredForecasts() } returns emptyList()

        // When
        val result = repository.getAllStoredForecasts()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify { fakeLocalDataSource.getAllStoredForecasts() }
        assertThat(result, `is`(empty()))
    }
}

