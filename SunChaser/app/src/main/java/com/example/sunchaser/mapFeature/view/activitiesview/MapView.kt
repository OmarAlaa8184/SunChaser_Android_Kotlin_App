package com.example.sunchaser.mapFeature.view.activitiesview
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import androidx.appcompat.app.AppCompatActivity
import com.example.sunchaser.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity()
{
     private lateinit var binding: ActivityMapBinding

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding=ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val webView = binding.webView
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(WebAppInterface(), "Android")
        webView.loadUrl("file:///android_asset/map.html")
    }

    inner class WebAppInterface
    {
        @JavascriptInterface
        fun onLocationSelected(lat: Double, lng: Double)
        {
            val result = Intent().apply {
                putExtra("latitude", lat)
                putExtra("longitude", lng)
            }
            setResult(RESULT_OK, result)
            finish()
        }
    }
}