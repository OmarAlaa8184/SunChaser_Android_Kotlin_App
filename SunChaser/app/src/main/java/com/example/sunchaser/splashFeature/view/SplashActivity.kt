package com.example.sunchaser.splashFeature.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.sunchaser.homeFeature.view.activitiesView.HomeView
import com.example.sunchaser.R
import com.example.sunchaser.splashFeature.viewmodel.SplashViewModel

class SplashActivity : AppCompatActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)  // Include logo/branding here

        viewModel.navigateToMain.observe(this){ shouldNavigate ->
            if (shouldNavigate) {
                startActivity(Intent(this, HomeView::class.java))
                finish()
            }
        }
    }
}
