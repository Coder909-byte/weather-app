package com.aeris.weather

import android.app.Application
import com.aeris.weather.data.local.FileWeatherCache
import com.aeris.weather.data.local.PreferencesManager
import com.aeris.weather.data.location.LocationProvider
import com.aeris.weather.data.remote.RetrofitProvider
import com.aeris.weather.data.repository.WeatherRepository

class AerisApplication : Application() {
    val preferences by lazy { PreferencesManager(this) }
    val repository by lazy { WeatherRepository(RetrofitProvider.weather, RetrofitProvider.geocoding, FileWeatherCache(filesDir)) }
    val location by lazy { LocationProvider(this) }
}
