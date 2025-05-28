package com.example.sunchaser.favoriteFeature.view.activitiesView

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sunchaser.R
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.example.productsusingviewbinding.RetrofitClient
import com.example.sunchaser.favoriteFeature.view.adapters.FavoriteAdapter
import com.example.sunchaser.homeFeature.view.activitiesView.HomeView
import com.example.sunchaser.model.db.ForecastDatabase
import com.example.sunchaser.model.db.ForecastLocalDataSourceImpl
import com.example.sunchaser.model.network.ForecastRemoteDataSourceImpl
import com.example.sunchaser.model.weatherPojo.ForecastRepositoryImpl
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.sunchaser.alertFeature.view.activitiesView.AlertView
import com.example.sunchaser.databinding.ActivityFavoriteBinding
import com.example.sunchaser.databinding.ItemFavoriteBinding
import com.example.sunchaser.favoriteFeature.viewmodel.FavoriteViewModel
import com.example.sunchaser.favoriteFeature.viewmodel.FavoriteViewModelFactory
import com.example.sunchaser.mapFeature.view.activitiesview.MapActivity
import com.example.sunchaser.model.db.SettingsLocalDataSourceImpl
import com.example.sunchaser.model.network.Location
import com.example.sunchaser.model.weatherPojo.Settings
import com.example.sunchaser.model.weatherPojo.SettingsManager
import com.example.sunchaser.settingsFeature.view.SettingsView
import java.util.Date
import java.util.Locale

class FavoriteView : AppCompatActivity() {
    private lateinit var favoriteViewModel: FavoriteViewModel
    private lateinit var favoriteAdapter: FavoriteAdapter
    private lateinit var binding: ActivityFavoriteBinding
    private lateinit var favoriteViewModelFactory: FavoriteViewModelFactory

    private val mapActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val lat = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val lng = data?.getDoubleExtra("longitude", 0.0) ?: 0.0
            favoriteViewModel.addFavoriteLocation(lat, lng)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLocale()
        binding = ActivityFavoriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvFavorites.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        favoriteAdapter = FavoriteAdapter(
            onDeleteClick = { forecast ->
                val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_confirmation, null)
                val alertDialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create()
                dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                    alertDialog.dismiss()
                }
                dialogView.findViewById<Button>(R.id.btnDelete).setOnClickListener {
                    favoriteViewModel.deleteFavorite(forecast)
                    alertDialog.dismiss()
                }
                alertDialog.show()
            },
            onItemClick = { forecast ->
                val intent = Intent(this, HomeView::class.java).apply {
                    putExtra("latitude", forecast.latitude.toDouble())
                    putExtra("longitude", forecast.longitude.toDouble())
                }
                startActivity(intent)
            }
        )
        binding.rvFavorites.adapter = favoriteAdapter

        favoriteViewModelFactory = FavoriteViewModelFactory(
            ForecastRepositoryImpl.getInstance(
                ForecastRemoteDataSourceImpl(RetrofitClient.retrofitService),
                ForecastLocalDataSourceImpl(ForecastDatabase.getInstance(this).forecastDao())
            ),
            SettingsLocalDataSourceImpl(this)
        )
        favoriteViewModel = ViewModelProvider(this, favoriteViewModelFactory)[FavoriteViewModel::class.java]

        binding.ivMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.navigationView.setCheckedItem(R.id.nav_favorites)
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeView::class.java))
                    finish()
                    true
                }
                R.id.nav_favorites -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_setting -> {
                    startActivity(Intent(this, SettingsView::class.java))
                    true
                }
                R.id.nav_alarm -> {
                    startActivity(Intent(this, AlertView::class.java))
                    true
                }
                R.id.nav_map -> {
                    val intent = Intent(this, MapActivity::class.java)
                    mapActivityResultLauncher.launch(intent)
                    true
                }
                else -> false
            }
        }

        favoriteViewModel.favorites.observe(this) { favorites ->
            favoriteAdapter.submitList(favorites)
        }
        favoriteViewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
        favoriteViewModel.toastMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        favoriteViewModel.isOffline.observe(this) { isOffline ->
            if (isOffline) {
                Toast.makeText(this, "Offline mode: Displaying cached data", Toast.LENGTH_SHORT).show()
            }
        }
        observeSettingsChanges()
        favoriteViewModel.loadFavorites()
    }

    private fun applyLocale() {
        val settings = SettingsManager.currentSettings.value ?: Settings()
        val locale = if (settings.language == "Arabic") Locale("ar") else Locale("en")
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun observeSettingsChanges() {
        lifecycleScope.launchWhenStarted {
            SettingsManager.settingsFlow.collect { settings ->
                val currentLocale = resources.configuration.locale
                val newLocale = if (settings.language == "Arabic") Locale("ar") else Locale("en")
                if (currentLocale.language != newLocale.language) {
                    recreate()
                } else {
                    binding.navigationView.menu.clear()
                    binding.navigationView.inflateMenu(R.menu.nav_menu)
                    favoriteAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}