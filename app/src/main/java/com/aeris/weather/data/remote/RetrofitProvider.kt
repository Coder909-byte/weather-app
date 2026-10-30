package com.aeris.weather.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl).client(client)
        .addConverterFactory(GsonConverterFactory.create()).build()

    val weather: WeatherApiService by lazy { retrofit("https://api.open-meteo.com/").create(WeatherApiService::class.java) }
    val geocoding: GeocodingApiService by lazy { retrofit("https://geocoding-api.open-meteo.com/").create(GeocodingApiService::class.java) }
}
