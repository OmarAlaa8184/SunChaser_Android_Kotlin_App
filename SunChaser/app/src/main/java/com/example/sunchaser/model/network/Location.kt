package com.example.sunchaser.model.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

class Location(private val context: Context)
{
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fun getLastLocation(callback: (Double, Double) -> Unit, onError: (String) -> Unit)
    {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null)
                    {
                        callback(location.latitude, location.longitude)
                    }
                    else
                    {
                        onError("Location not available")
                    }
                }
                .addOnFailureListener { e ->
                    onError("Error getting location: ${e.message}")
                }
        }
        else
        {
            onError("Location permission not granted")
        }
    }
}