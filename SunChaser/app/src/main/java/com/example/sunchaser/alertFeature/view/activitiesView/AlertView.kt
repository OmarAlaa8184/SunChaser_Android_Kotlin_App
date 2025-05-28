package com.example.sunchaser.alertFeature.view.activitiesView

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.productsusingviewbinding.RetrofitClient
import com.example.sunchaser.R
import com.example.sunchaser.alertFeature.view.adapters.AlertsAdapter
import com.example.sunchaser.alertFeature.viewmodel.AlertViewModel
import com.example.sunchaser.alertFeature.viewmodel.AlertViewModelFactory
import com.example.sunchaser.databinding.ActivityAlertBinding
import com.example.sunchaser.favoriteFeature.viewmodel.FavoriteViewModel
import com.example.sunchaser.favoriteFeature.viewmodel.FavoriteViewModelFactory
import com.example.sunchaser.homeFeature.viewmodel.HomeViewModel
import com.example.sunchaser.homeFeature.viewmodel.HomeViewModelFactory
import com.example.sunchaser.model.db.AlertLocalDataSourceImpl
import com.example.sunchaser.model.db.ForecastDatabase
import com.example.sunchaser.model.db.ForecastLocalDataSourceImpl
import com.example.sunchaser.model.db.SettingsLocalDataSourceImpl
import com.example.sunchaser.model.network.ForecastRemoteDataSourceImpl
import com.example.sunchaser.model.network.Location
import com.example.sunchaser.model.weatherPojo.Alert
import com.example.sunchaser.model.weatherPojo.ForecastRepositoryImpl
import com.example.sunchaser.model.weatherPojo.Settings
import com.example.sunchaser.model.weatherPojo.SettingsManager
import java.util.Calendar



class AlertView : AppCompatActivity()
{
    private lateinit var binding: ActivityAlertBinding
    private lateinit var viewModel: AlertViewModel
    private lateinit var adapter: AlertsAdapter
    private lateinit var homeViewModel: HomeViewModel // Add HomeViewModel
    private lateinit var alertViewModelFactory: AlertViewModelFactory
    private var currentLocation: Triple<String, Double, Double>? = null // Cache location data
    private var currentTemperature: Float = 0f // Cache temperature

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
    {
        isGranted: Boolean ->
        if (!isGranted)
        {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            homeViewModel.fetchForecastByLocation()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request notification permission
        checkNotificationPermission()

        // Initialize ViewModel
        alertViewModelFactory=AlertViewModelFactory(AlertLocalDataSourceImpl(ForecastDatabase.getInstance(applicationContext).alertDao(), this),this)
        viewModel = ViewModelProvider(this,alertViewModelFactory)[AlertViewModel::class.java]

        // Setup NumberPicker
        binding.durationPicker.minValue = 1
        binding.durationPicker.maxValue = 100
        binding.durationPicker.value = 5
        // Disable selection of past dates
        binding.datePicker.minDate = System.currentTimeMillis()

        val homeViewModelFactory = HomeViewModelFactory(
            ForecastRepositoryImpl.getInstance(
                ForecastRemoteDataSourceImpl(RetrofitClient.retrofitService),
                ForecastLocalDataSourceImpl(ForecastDatabase.getInstance(this).forecastDao())
            ), Location(this),
            SettingsLocalDataSourceImpl(this)
        )
        homeViewModel = ViewModelProvider(this, homeViewModelFactory)[HomeViewModel::class.java]

       homeViewModel.forecast.observe(this) { response ->
            response?.let {
                currentLocation = Triple(
                    "${it.city.name}, ${it.city.country}",
                    it.city.coord.lat.toDouble(),
                    it.city.coord.lon.toDouble()
                )
                currentTemperature = it.list.firstOrNull()?.main?.temp ?: 0f
            }
        }
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        createNotificationChannel()
    }

    private fun checkNotificationPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupRecyclerView()
    {
        adapter = AlertsAdapter(
            viewModel,
            onToggle = { id, active -> viewModel.toggleAlert(id, active) },
            onDeleteClick = { alert ->
                val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_alert_confirmation, null)
                val alertDialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create()

                dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                    alertDialog.dismiss()
                }

                dialogView.findViewById<Button>(R.id.btnDelete).setOnClickListener {
                    viewModel.deleteAlert(alert)
                    alertDialog.dismiss()
                }
                alertDialog.show()
            }
        )
        binding.alertsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AlertView)
            adapter = this@AlertView.adapter
        }
    }

    private fun setupClickListeners()
    {
        binding.btnAddAlert.setOnClickListener {
            addAlert()
        }
    }

    private fun addAlert() {
        val calendar = Calendar.getInstance().apply {
            set(
                binding.datePicker.year,
                binding.datePicker.month,
                binding.datePicker.dayOfMonth,
                binding.startTimePicker.hour,
                binding.startTimePicker.minute,
                0
            )
        }

        val startTime = calendar.timeInMillis
        val endTime = startTime + (binding.durationPicker.value * 60 * 1000L)

        if (endTime <= startTime) {
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show()
            return
        }

        if (startTime < System.currentTimeMillis()) {
            Toast.makeText(this, "Start time cannot be in the past", Toast.LENGTH_SHORT).show()
            return
        }

        val alertType = if (binding.switchType.isChecked) {
            Alert.AlertType.ALARM_SOUND
        } else {
            Alert.AlertType.NOTIFICATION
        }
        val location = currentLocation ?: Triple("Current Location", 30.0333, 31.2333) // Fallback to default (Cairo)
        val temperature=currentTemperature
        viewModel.addAlert(
            locationName = location.first,
            lat = location.second,
            lon = location.third,
            startTime = startTime,
            endTime = endTime,
            alertType = alertType,
            temperature = temperature
        )
    }

    private fun observeViewModel() {
        viewModel.alerts.observe(this) { alerts ->
            adapter.submitList(alerts)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            NotificationChannel(
                "weather_alerts",
                "Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Weather alert notifications"
                notificationManager.createNotificationChannel(this)
            }
            NotificationChannel(
                "alarm_channel",
                "Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Weather alarm notifications"
                notificationManager.createNotificationChannel(this)
            }
        }
    }
}
