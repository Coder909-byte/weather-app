package com.aeris.weather.data.local

import android.util.AtomicFile
import com.aeris.weather.data.remote.dto.WeatherDto
import com.aeris.weather.domain.model.City
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class CachedWeather(val city: City, val response: WeatherDto, val fetchedAtEpoch: Long)

interface WeatherCache {
    suspend fun read(): CachedWeather?
    suspend fun write(weather: CachedWeather)
}

class FileWeatherCache(directory: File, private val gson: Gson = Gson()) : WeatherCache {
    private val file = AtomicFile(File(directory, "last_weather.json"))
    override suspend fun read(): CachedWeather? = withContext(Dispatchers.IO) {
        runCatching { file.openRead().bufferedReader().use { gson.fromJson(it, CachedWeather::class.java) } }.getOrNull()
    }
    override suspend fun write(weather: CachedWeather) = withContext(Dispatchers.IO) {
        val stream = file.startWrite()
        try {
            stream.write(gson.toJson(weather).toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }
}
