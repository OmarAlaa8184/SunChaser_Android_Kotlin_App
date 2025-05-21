package com.example.sunchaser.homeFeature.view.activitiesView


import android.Manifest
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.example.sunchaser.homeFeature.view.listener.OnCurrentDayClickListener
import com.example.sunchaser.model.db.ForecastDatabase
import com.example.sunchaser.model.db.ForecastLocalDataSourceImpl
import com.example.sunchaser.model.network.ForecastRemoteDataSourceImpl
import com.example.sunchaser.model.network.Location
import com.example.sunchaser.model.weatherPojo.ForecastRepositoryImpl
import com.example.sunchaser.model.weatherPojo.ForecastResponse
import com.example.sunchaser.model.weatherPojo.toEntityList
import java.util.Date
import java.util.Locale

class HomeView : AppCompatActivity() , OnDailyClickListener,OnHourlyForecastClickListener,OnCurrentDayClickListener
{
    private lateinit var hourlyAdapter: HourlyAdapter
    private lateinit var dailyAdapter: DailyAdapter
    private lateinit var currentDayAdapter: CurrentDayAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var homeViewModelFactory: HomeViewModelFactory
    private lateinit var homeViewModel: HomeViewModel
    private val REQUEST_LOCATION_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvDailyForecast.layoutManager=LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)
        binding.rvHourlyForecast.layoutManager=LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        binding.rvCurrDayForecast.layoutManager=LinearLayoutManager(this)

        dailyAdapter=DailyAdapter(this)
        hourlyAdapter=HourlyAdapter(this)
        currentDayAdapter=CurrentDayAdapter(this)

        binding.rvDailyForecast.adapter=dailyAdapter
        binding.rvHourlyForecast.adapter=hourlyAdapter
        binding.rvCurrDayForecast.adapter=currentDayAdapter

        homeViewModelFactory=HomeViewModelFactory(
                ForecastRepositoryImpl.getInstance(
                ForecastRemoteDataSourceImpl(RetrofitClient.retrofitService),
                ForecastLocalDataSourceImpl(ForecastDatabase.getInstance(this).forecastDao())),
                Location(this))

        homeViewModel = ViewModelProvider(this, homeViewModelFactory)[HomeViewModel::class.java]
        homeViewModel.fetchForecastByLocation()


        // Observe data
        homeViewModel.forecast.observe(this) { response ->
            updateUI(response)
        }

        homeViewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }

        // Request location
        requestLocationPermission()
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
        val current = response.list.first()
        findViewById<TextView>(R.id.tvCity).text = "${response.city.name}, ${response.city.country}"
        findViewById<TextView>(R.id.tvDateTime).text = SimpleDateFormat(
            "MMM d, yyyy • h:mm a",
            Locale.getDefault()
        ).format(Date())

        //Card
        currentDayAdapter.submitList(response.toEntityList())

        // Hourly Forecast
        hourlyAdapter.submitList(response.toEntityList())

        // 5-Day Forecast
        dailyAdapter.submitList(response.toEntityList())
    }
}
