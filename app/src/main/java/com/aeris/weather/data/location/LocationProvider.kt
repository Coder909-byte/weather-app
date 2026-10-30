package com.aeris.weather.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import com.aeris.weather.domain.model.City
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

class LocationProvider(private val context: Context) {
    @SuppressLint("MissingPermission") // Permission is checked by the Activity before this call.
    suspend fun currentCity(): City {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(manager)) throw IllegalStateException("Location services are off. Turn them on or choose a city manually.")
        val client = LocationServices.getFusedLocationProviderClient(context)
        val token = CancellationTokenSource()
        val location = try {
            withTimeoutOrNull(15_000) {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token).await()
            }
        } finally { token.cancel() }
        if (location == null) throw IllegalStateException("Your location is unavailable. Try again or choose a city manually.")
        // Open-Meteo has no reverse-geocoding endpoint; show the real coordinates instead of inventing a city.
        return City(name = "Current location", region = String.format(Locale.getDefault(), "%.2f°, %.2f°", location.latitude, location.longitude),
            latitude = location.latitude, longitude = location.longitude, isCurrentLocation = true)
    }
}
