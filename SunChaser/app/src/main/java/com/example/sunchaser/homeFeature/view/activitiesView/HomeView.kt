package com.example.sunchaser.homeFeature.view.activitiesView


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.productsusingviewbinding.RetrofitClient
import com.example.sunchaser.R
import com.example.sunchaser.alertFeature.view.activitiesView.AlertView
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
import com.example.sunchaser.model.db.SettingsLocalDataSource
import com.example.sunchaser.model.db.SettingsLocalDataSourceImpl
import com.example.sunchaser.model.network.ForecastRemoteDataSourceImpl
import com.example.sunchaser.model.network.Location
import com.example.sunchaser.model.weatherPojo.ForecastEntity
import com.example.sunchaser.model.weatherPojo.ForecastRepositoryImpl
import com.example.sunchaser.model.weatherPojo.ForecastResponse
import com.example.sunchaser.model.weatherPojo.Settings
import com.example.sunchaser.model.weatherPojo.SettingsManager
import com.example.sunchaser.model.weatherPojo.StatisticItem
import com.example.sunchaser.model.weatherPojo.toEntityList
import com.example.sunchaser.model.weatherPojo.toHourlyFormat
import com.example.sunchaser.settingsFeature.view.SettingsView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale


class HomeView : AppCompatActivity(), OnDailyClickListener, OnHourlyForecastClickListener, OnCurrentDayClickListener {
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
        applyLocale() // Apply locale on creation
        initializeSettings()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvDailyForecast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rvHourlyForecast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvCurrDayForecast.layoutManager = LinearLayoutManager(this)
        binding.rvStatistics.layoutManager = LinearLayoutManager(this)

        dailyAdapter = DailyAdapter(this)
        hourlyAdapter = HourlyAdapter(this)
        currentDayAdapter = CurrentDayAdapter(this)
        statisticsAdapter = StatisticsAdapter()

        binding.rvDailyForecast.adapter = dailyAdapter
        binding.rvHourlyForecast.adapter = hourlyAdapter
        binding.rvCurrDayForecast.adapter = currentDayAdapter
        binding.rvStatistics.adapter = statisticsAdapter

        lineChart = binding.lineChartTemperature

        homeViewModelFactory = HomeViewModelFactory(
            ForecastRepositoryImpl.getInstance(
                ForecastRemoteDataSourceImpl(RetrofitClient.retrofitService),
                ForecastLocalDataSourceImpl(ForecastDatabase.getInstance(this).forecastDao())), Location(this),
            SettingsLocalDataSourceImpl(this)
        )
        homeViewModel = ViewModelProvider(this, homeViewModelFactory)[HomeViewModel::class.java]

        favoriteViewModelFactory = FavoriteViewModelFactory(
            ForecastRepositoryImpl.getInstance(
                ForecastRemoteDataSourceImpl(RetrofitClient.retrofitService),
                ForecastLocalDataSourceImpl(ForecastDatabase.getInstance(this).forecastDao())
            ),
            SettingsLocalDataSourceImpl(this)
        )
        favoriteViewModel = ViewModelProvider(this, favoriteViewModelFactory)[FavoriteViewModel::class.java]

        requestLocationPermission()
        homeViewModel.forecast.observe(this) { response ->
            updateUI(response)
        }
        homeViewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
        favoriteViewModel.toastMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        favoriteViewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
        homeViewModel.isOffline.observe(this) { isOffline ->
            if (isOffline) {
                Toast.makeText(this, "Offline mode: Displaying cached data", Toast.LENGTH_SHORT).show()
            }
        }//loadFavorites
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
        binding.btnRetryPermission.setOnClickListener {
            requestLocationPermission()
        }
        setupDrawer()
        observeSettingsChanges()
        updateUILabels()
        fetchInitialForecast()
    }

    private fun fetchInitialForecast()
    {
        val settings = SettingsManager.currentSettings.value ?: Settings()
        if (settings.locationSource == "GPS") {
            requestLocationPermission()
        } else {
            homeViewModel.fetchForecast(settings.latitude, settings.longitude)
            binding.tvCity.text = settings.locationName
            binding.tvDateTime.text = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date())
        }
    }

    private fun initializeSettings()
    {
        val settingsLocalDataSource = SettingsLocalDataSourceImpl(this)
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            val settings = settingsLocalDataSource.getSettings() ?: Settings()
            SettingsManager.updateSettings(settings)
        }
    }

    private fun applyLocale()
    {
        val settings = SettingsManager.currentSettings.value ?: Settings()
        val locale = if (settings.language == "Arabic") Locale("ar") else Locale("en")
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun observeSettingsChanges()
    {
       // Apply initial settings
       applySettings(SettingsManager.currentSettings.value ?: Settings())
       // Observe future changes
       lifecycleScope.launch {
           SettingsManager.settingsFlow.collect { settings ->
               applySettings(settings)
           }
       }
   }

    private fun applySettings(settings: Settings)
    {
        // Update locale
        val currentLocale = resources.configuration.locale
        val newLocale = if (settings.language == "Arabic") Locale("ar") else Locale("en")
        if (currentLocale.language != newLocale.language) {
            Locale.setDefault(newLocale)
            val config = resources.configuration
            config.setLocale(newLocale)
            resources.updateConfiguration(config, resources.displayMetrics)
            recreate() // Recreate for language changes
        }
        else
        {
            // Update UI elements
            updateUILabels()
            homeViewModel.forecast.value?.let { response ->
                val hourlyForecasts = response.toEntityList().take(8)
                updateTemperatureChart(hourlyForecasts)
                updateStatistics(hourlyForecasts)
                currentDayAdapter.notifyDataSetChanged()
                hourlyAdapter.notifyDataSetChanged()
                dailyAdapter.notifyDataSetChanged()
                statisticsAdapter.notifyDataSetChanged()
            }
            binding.navigationView.menu.clear()
            binding.navigationView.inflateMenu(R.menu.nav_menu)
        }
    }

    private fun updateUILabels()
    {
        binding.tvDateTime.text = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date())
        homeViewModel.forecast.value?.let { response ->
            binding.tvCity.text = "${response.city.name}, ${response.city.country}"
        }
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

     if (requestCode == REQUEST_LOCATION_PERMISSION) {
         when {
//             grantResults.isNotEmpty() && permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) &&
//                     grantResults[permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION)] == PackageManager.PERMISSION_GRANTED
                  grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED-> {
                 // Permission granted
                 Toast.makeText(this, "Location permission granted. Fetching forecast...", Toast.LENGTH_SHORT).show()
                 binding.contentContainer.visibility = View.VISIBLE
                 binding.cardRetryPermission.visibility = View.GONE
                 homeViewModel.fetchForecastByLocation()
             }
             grantResults.isNotEmpty() && permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                 // Permission denied
                 if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
                 {
                     // Permission denied permanently
                     Toast.makeText(this, "Location permission denied permanently. Please enable it in settings.", Toast.LENGTH_LONG).show()

                     val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                         data = Uri.fromParts("package", packageName, null)
                     }
                     startActivity(intent)
                 }
                 else
                 {
                     // Permission denied temporarily
                     Toast.makeText(this, "Location permission required to fetch forecast.", Toast.LENGTH_SHORT).show()
                 }

                 binding.contentContainer.visibility = View.GONE
                 binding.cardRetryPermission.visibility = View.VISIBLE
             }
             else -> {
                 // Unexpected case
                 Toast.makeText(this, "Error processing permission request.", Toast.LENGTH_SHORT).show()
                 binding.contentContainer.visibility = View.GONE
                 binding.cardRetryPermission.visibility = View.VISIBLE
             }
         }
     }
 }

    private fun updateUI(response: ForecastResponse)
    {
        intent.extras?.let { extras ->
            val lat = extras.getDouble("latitude", defaultLat)
            val lng = extras.getDouble("longitude", defaultLng)
            homeViewModel.fetchForecast(lat, lng)
            binding.tvCity.text = "Selected Location"
            binding.tvDateTime.text = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date())
        }
        binding.tvCity.text = "${response.city.name}, ${response.city.country}"
        binding.tvDateTime.text = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date())

        currentDayAdapter.submitList(response.toEntityList())
        hourlyAdapter.submitList(response.toEntityList())
        dailyAdapter.submitList(response.toEntityList())

        val hourlyForecasts = response.toEntityList().take(8)
        updateTemperatureChart(hourlyForecasts)
        updateStatistics(hourlyForecasts)
    }

    private fun setupDrawer()
    {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_setting -> {
                    startActivity(Intent(this, SettingsView::class.java))
                }
                R.id.nav_map -> {
                    val intent = Intent(this, MapActivity::class.java)
                    mapActivityResultLauncher.launch(intent)
                }
                R.id.nav_favorites -> {
                    startActivity(Intent(this, FavoriteView::class.java))
                }
                R.id.nav_alarm -> {
                    startActivity(Intent(this, AlertView::class.java))
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onStart()
    {
        super.onStart()
        binding.navigationView.setCheckedItem(R.id.nav_home)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            binding.contentContainer.visibility = View.VISIBLE
            binding.cardRetryPermission.visibility = View.GONE
            homeViewModel.fetchForecastByLocation()
        }
        else
        {
            binding.contentContainer.visibility = View.GONE
            binding.cardRetryPermission.visibility = View.VISIBLE
        }
    }

    private fun updateTemperatureChart(forecasts: List<ForecastEntity>)
    {
        val settings = SettingsManager.currentSettings.value ?: Settings()
        val unitLabel = when (settings.temperatureUnit) {
            "Kelvin" -> "K"
            "Fahrenheit" -> "°F"
            else -> "°C"
        }
        val entries = forecasts.mapIndexed { index, forecast ->
            Entry(index.toFloat(), forecast.temp)
        }
        val dataSet = LineDataSet(entries, "Temperature ($unitLabel)").apply {
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

        stats.add(StatisticItem(this.getString(R.string.average_temperature), avgTemp.toInt().toString()))
        stats.add(StatisticItem(this.getString(R.string.max_temperature), maxTemp.toInt().toString()))
        stats.add(StatisticItem(this.getString(R.string.min_temperature), minTemp.toInt().toString()))
        stats.add(StatisticItem(this.getString(R.string.average_humidity), "${avgHumidity.toInt()}%"))

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