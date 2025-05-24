package com.example.sunchaser.homeFeature.view.activitiesView


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.productsusingviewbinding.RetrofitClient
import com.example.sunchaser.R
import com.example.sunchaser.databinding.ActivityMainBinding
import com.example.sunchaser.favoriteFeature.view.activitiesView.FavoriteView
import com.example.sunchaser.favoriteFeature.viewmodel.FavoriteViewModel
import com.example.sunchaser.favoriteFeature.viewmodel.FavoriteViewModelFactory
import com.example.sunchaser.homeFeature.view.adapters.CurrentDayAdapter
import com.example.sunchaser.homeFeature.view.adapters.DailyAdapter
import com.example.sunchaser.homeFeature.view.adapters.HourlyAdapter
import com.example.sunchaser.homeFeature.view.adapters.StatisticsAdapter
import com.example.sunchaser.homeFeature.view.listener.OnCurrentDayClickListener
import com.example.sunchaser.homeFeature.view.listener.OnDailyClickListener
import com.example.sunchaser.homeFeature.view.listener.OnHourlyForecastClickListener
import com.example.sunchaser.homeFeature.viewmodel.HomeViewModel
import com.example.sunchaser.homeFeature.viewmodel.HomeViewModelFactory
import com.example.sunchaser.mapFeature.view.activitiesview.MapActivity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private lateinit var favoriteViewModel: FavoriteViewModel
    private lateinit var favoriteViewModelFactory: FavoriteViewModelFactory
    private lateinit var lineChart: LineChart
    private val defaultLat = 30.0333
    private val defaultLng = 31.2333

    private val REQUEST_LOCATION_PERMISSION = 1

    private val mapActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val lat = data?.getDoubleExtra("latitude", defaultLat) ?: defaultLat
                val lng = data?.getDoubleExtra("longitude", defaultLng) ?: defaultLng
                homeViewModel.fetchForecast(lat, lng)
                binding.tvCity.text = "Selected Location"
                binding.tvDateTime.text = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date())
            }
        }

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

        favoriteViewModelFactory= FavoriteViewModelFactory(ForecastRepositoryImpl.getInstance(
            ForecastRemoteDataSourceImpl(RetrofitClient.retrofitService),
            ForecastLocalDataSourceImpl(ForecastDatabase.getInstance(this).forecastDao())))

        // Initialize ViewModel
        favoriteViewModel = ViewModelProvider(this, favoriteViewModelFactory)[FavoriteViewModel::class.java]

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

        binding.navigationView.setCheckedItem(R.id.nav_home)

        binding.btnAddToFavorites.setOnClickListener {
            homeViewModel.forecast.value?.let { response ->
                val lat = response.city.coord.lat.toDouble()
                val lng = response.city.coord.lon.toDouble()
                favoriteViewModel.addFavoriteLocation(lat, lng)
            } ?: run {
                Toast.makeText(this, "No location data available", Toast.LENGTH_SHORT).show()
            }
        }
        // Observe FavoriteViewModel feedback
        favoriteViewModel.toastMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        favoriteViewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
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
        // Handle Intent from FavoriteActivity
            intent.extras?.let { extras ->
            val lat = extras.getDouble("latitude", defaultLat)
            val lng = extras.getDouble("longitude", defaultLng)
            homeViewModel.fetchForecast(lat, lng)
            binding.tvCity.text = "Selected Location"
            binding.tvDateTime.text = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date())
        }
        // Current Weather
        binding.tvCity.text = "${response.city.name}, ${response.city.country}"
        binding.tvDateTime.text = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date())

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

    private fun setupDrawer()
    {
        // Setup navigation item clicks
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId)
            {
                R.id.nav_home ->
                    {
                       binding.drawerLayout.closeDrawer(GravityCompat.START)
                    }
                R.id.nav_setting ->
                    {
                       // Handle Settings
                    }
                R.id.nav_map->
                    {
                       val intent = Intent(this, MapActivity::class.java)
                       mapActivityResultLauncher.launch(intent)
                    }
                R.id.nav_favorites->
                    {
                        val intent = Intent(this, FavoriteView::class.java)
                        startActivity(intent)
                    }
            }
            // Close drawer after click
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onStart() {
        super.onStart()
        binding.navigationView.setCheckedItem(R.id.nav_home)
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

    override fun onBackPressed()
    {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
