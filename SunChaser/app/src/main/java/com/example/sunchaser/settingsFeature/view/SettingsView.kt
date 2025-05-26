package com.example.sunchaser.settingsFeature.view
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.sunchaser.R
import com.example.sunchaser.databinding.ActivitySettingsBinding
import com.example.sunchaser.mapFeature.view.activitiesview.MapActivity
import com.example.sunchaser.model.db.ForecastDatabase
import com.example.sunchaser.model.db.SettingsLocalDataSourceImpl
import com.example.sunchaser.model.network.Location
import com.example.sunchaser.model.weatherPojo.Settings
import com.example.sunchaser.settingsFeature.viewmodel.SettingsViewModel
import com.example.sunchaser.settingsFeature.viewmodel.SettingsViewModelFactory
import java.util.Locale


class SettingsView : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel
    private lateinit var location: Location
    private val defaultLat = 30.0333
    private val defaultLng = 31.2333

    private val REQUEST_LOCATION_PERMISSION = 1

    private val mapActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val lat = data?.getDoubleExtra("latitude", defaultLat) ?: defaultLat
                val lng = data?.getDoubleExtra("longitude", defaultLng) ?: defaultLng
                val settings = viewModel.settings.value?.copy(
                    locationSource = "MAP",
                    latitude = lat,
                    longitude = lng,
                    locationName = "Selected Location"
                ) ?: Settings(
                    locationSource = "MAP",
                    latitude = lat,
                    longitude = lng,
                    locationName = "Selected Location"
                )
                viewModel.saveSettings(settings)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        location = Location(this)
        val db = ForecastDatabase.getInstance(this)
        val repository = SettingsLocalDataSourceImpl(db.settingsDao())
        viewModel = ViewModelProvider(
            this,
            SettingsViewModelFactory(repository)
        )[SettingsViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.radioGroupLocation.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioGps -> {
                    if (checkLocationPermission()) {
                        updateLocationFromGPS()
                    }
                    else
                    {
                        requestLocationPermission()
                    }
                }
                R.id.radioMap -> {
                    val intent = Intent(this, MapActivity::class.java)
                    mapActivityResultLauncher.launch(intent)
                }
            }
        }

        binding.spinnerTempUnit.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                updateSettings()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        binding.spinnerWindUnit.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                updateSettings()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        binding.spinnerLanguage.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                updateSettings()
                //updateLocale()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }

    private fun updateSettings() {
        val settings = viewModel.settings.value?.copy(
            locationSource = if (binding.radioGps.isChecked) "GPS" else "MAP",
            temperatureUnit = binding.spinnerTempUnit.selectedItem.toString(),
            windSpeedUnit = binding.spinnerWindUnit.selectedItem.toString(),
            language = binding.spinnerLanguage.selectedItem.toString()
        ) ?: Settings(
            locationSource = if (binding.radioGps.isChecked) "GPS" else "MAP",
            temperatureUnit = binding.spinnerTempUnit.selectedItem.toString(),
            windSpeedUnit = binding.spinnerWindUnit.selectedItem.toString(),
            language = binding.spinnerLanguage.selectedItem.toString()
        )
        viewModel.saveSettings(settings)
    }

    private fun checkLocationPermission(): Boolean
    {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSION
        )
    }

    private fun updateLocationFromGPS()
    {
        if (checkLocationPermission())
        {
            location.getLastLocation(
                callback = { lat, lon ->
                    val settings = viewModel.settings.value?.copy(
                        locationSource = "GPS",
                        latitude = lat,
                        longitude = lon,
                        locationName = "Current Location"
                    ) ?: Settings(
                        locationSource = "GPS",
                        latitude = lat,
                        longitude = lon,
                        locationName = "Current Location"
                    )
                    viewModel.saveSettings(settings)
                },
                onError = {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateLocationFromGPS()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.settings.observe(this) { settings ->
            binding.radioGps.isChecked = settings.locationSource == "GPS"
            binding.radioMap.isChecked = settings.locationSource == "MAP"
            binding.spinnerTempUnit.setSelection(
                when (settings.temperatureUnit) {
                    "Kelvin" -> 0
                    "Celsius" -> 1
                    "Fahrenheit" -> 2
                    else -> 1
                }
            )
            binding.spinnerWindUnit.setSelection(
                if (settings.windSpeedUnit == "miles/hour") 1 else 0
            )
            binding.spinnerLanguage.setSelection(
                if (settings.language == "Arabic") 1 else 0
            )
        }
    }
}