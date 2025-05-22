package com.example.sunchaser.homeFeature.view.activitiesView


import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.productsusingviewbinding.RetrofitClient
import com.example.sunchaser.homeFeature.view.adapters.DailyAdapter
import com.example.sunchaser.homeFeature.view.adapters.HourlyAdapter
import com.example.sunchaser.homeFeature.view.listener.OnDailyClickListener
import com.example.sunchaser.homeFeature.view.listener.OnHourlyForecastClickListener
import com.example.sunchaser.homeFeature.viewmodel.HomeViewModel
import com.example.sunchaser.homeFeature.viewmodel.HomeViewModelFactory
import com.example.sunchaser.R
import com.example.sunchaser.databinding.ActivityMainBinding
import com.example.sunchaser.homeFeature.view.adapters.CurrentDayAdapter
import com.example.sunchaser.homeFeature.view.adapters.StatisticsAdapter
import com.example.sunchaser.homeFeature.view.listener.OnCurrentDayClickListener
import com.example.sunchaser.model.db.ForecastDatabase
import com.example.sunchaser.model.db.ForecastLocalDataSourceImpl
import com.example.sunchaser.model.network.ForecastRemoteDataSourceImpl
import com.example.sunchaser.model.network.Location
import com.example.sunchaser.model.weatherPojo.ForecastEntity
import com.example.sunchaser.model.weatherPojo.ForecastRepositoryImpl
import com.example.sunchaser.model.weatherPojo.ForecastResponse
import com.example.sunchaser.model.weatherPojo.StatisticItem
import com.example.sunchaser.model.weatherPojo.toEntityList
import com.example.sunchaser.model.weatherPojo.toHourlyFormat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.navigation.NavigationView
import java.util.Date
import java.util.Locale

class HomeView : AppCompatActivity() , OnDailyClickListener,OnHourlyForecastClickListener,OnCurrentDayClickListener
{
    private lateinit var hourlyAdapter: HourlyAdapter
    private lateinit var dailyAdapter: DailyAdapter
    private lateinit var currentDayAdapter: CurrentDayAdapter
    private lateinit var statisticsAdapter: StatisticsAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var homeViewModelFactory: HomeViewModelFactory
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var lineChart: LineChart

    private val REQUEST_LOCATION_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvDailyForecast.layoutManager=LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)
        binding.rvHourlyForecast.layoutManager=LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        binding.rvCurrDayForecast.layoutManager=LinearLayoutManager(this)
        binding.rvStatistics.layoutManager=LinearLayoutManager(this)

        dailyAdapter=DailyAdapter(this)
        hourlyAdapter=HourlyAdapter(this)
        currentDayAdapter=CurrentDayAdapter(this)
        statisticsAdapter=StatisticsAdapter()

        binding.rvDailyForecast.adapter=dailyAdapter
        binding.rvHourlyForecast.adapter=hourlyAdapter
        binding.rvCurrDayForecast.adapter=currentDayAdapter
        binding.rvStatistics.adapter=statisticsAdapter

        lineChart = binding.lineChartTemperature

        homeViewModelFactory=HomeViewModelFactory(
                ForecastRepositoryImpl.getInstance(
                ForecastRemoteDataSourceImpl(RetrofitClient.retrofitService),
                ForecastLocalDataSourceImpl(ForecastDatabase.getInstance(this).forecastDao())),
                Location(this))

        homeViewModel = ViewModelProvider(this, homeViewModelFactory)[HomeViewModel::class.java]
        homeViewModel.fetchForecastByLocation()

        // Request location
        requestLocationPermission()

        // Observe data
        homeViewModel.forecast.observe(this) { response ->
            updateUI(response)
        }

        homeViewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }

        binding.ivMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        setupDrawer()

    }

    private fun requestLocationPermission()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
        }
        else
        {
            homeViewModel.fetchForecastByLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            homeViewModel.fetchForecastByLocation()
        }
        else
        {
            Toast.makeText(this, "Location permission denied. Using default location.", Toast.LENGTH_LONG).show()
            homeViewModel.fetchForecast(30.0444, 31.2357) // Fallback: Cairo
        }
    }



    private fun updateUI(response: ForecastResponse)
    {
        // Current Weather
        findViewById<TextView>(R.id.tvCity).text = "${response.city.name}, ${response.city.country}"
        findViewById<TextView>(R.id.tvDateTime).text = SimpleDateFormat(
            "MMM d, yyyy • h:mm a",
            Locale.getDefault()
        ).format(Date())

        val forecastEntities = response.toEntityList()

        //Card
        currentDayAdapter.submitList(response.toEntityList())

        // Hourly Forecast
        hourlyAdapter.submitList(response.toEntityList())

        // 5-Day Forecast
        dailyAdapter.submitList(response.toEntityList())


        val hourlyForecasts = forecastEntities.take(8)
        updateTemperatureChart(hourlyForecasts)
        updateStatistics(hourlyForecasts)


    }
    private fun setupDrawer() {
        // Setup navigation item clicks
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId)
            {
                R.id.nav_home -> {
                    // Handle Home
                }
                R.id.nav_setting -> {
                    // Handle Settings
                }
                R.id.nav_map->{

                }
                R.id.nav_favorites-> {

                }

            }
            // Close drawer after click
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun updateTemperatureChart(forecasts: List<ForecastEntity>)
    {
        val entries = forecasts.mapIndexed { index, forecast ->
            Entry(index.toFloat(), forecast.temp)
        }
        val dataSet = LineDataSet(entries, "Temperature (°C)").apply {
            color = ContextCompat.getColor(this@HomeView, R.color.white)
            valueTextColor = ContextCompat.getColor(this@HomeView, R.color.white)
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(ContextCompat.getColor(this@HomeView, R.color.white))
            setDrawValues(false)
        }
        val lineData = LineData(dataSet)
        lineChart.apply {
            data = lineData
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setBackgroundColor(Color.TRANSPARENT)
            xAxis.textColor = ContextCompat.getColor(this@HomeView, R.color.white)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return forecasts.getOrNull(value.toInt())?.dt?.toHourlyFormat() ?: ""
                }
            }
            axisLeft.textColor = ContextCompat.getColor(this@HomeView, R.color.white)
            axisRight.isEnabled = false
            legend.textColor = ContextCompat.getColor(this@HomeView, R.color.white)
            invalidate()
        }
    }

    private fun updateStatistics(forecasts: List<ForecastEntity>)
    {
        val stats = mutableListOf<StatisticItem>()
        val avgTemp = forecasts.map { it.temp }.average().toFloat()
        val maxTemp = forecasts.maxOfOrNull { it.temp } ?: 0f
        val minTemp = forecasts.minOfOrNull { it.temp } ?: 0f
        val avgHumidity = forecasts.map { it.humidity }.average().toFloat()

        stats.add(StatisticItem("Average Temperature", "${avgTemp.toInt()}°C"))
        stats.add(StatisticItem("Max Temperature", "${maxTemp.toInt()}°C"))
        stats.add(StatisticItem("Min Temperature", "${minTemp.toInt()}°C"))
        stats.add(StatisticItem("Average Humidity", "${avgHumidity.toInt()}%"))

        statisticsAdapter.submitList(stats)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
