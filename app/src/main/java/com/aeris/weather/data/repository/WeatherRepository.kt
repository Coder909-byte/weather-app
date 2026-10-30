package com.aeris.weather.data.repository

import com.aeris.weather.data.local.CachedWeather
import com.aeris.weather.data.local.WeatherCache
import com.aeris.weather.data.remote.GeocodingApiService
import com.aeris.weather.data.remote.WeatherApiService
import com.aeris.weather.domain.mapper.toCity
import com.aeris.weather.domain.mapper.toWeather
import com.aeris.weather.domain.model.City
import com.aeris.weather.domain.model.WeatherUiModel
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import java.time.Clock
import java.time.Instant

class WeatherRepository(
    private val weatherApi: WeatherApiService,
    private val geocodingApi: GeocodingApiService,
    private val cache: WeatherCache,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun cached(city: City): WeatherUiModel? {
        return try {
            cache.read()?.takeIf { it.city.sameCoordinates(city) }?.let {
                it.response.toWeather(city, Instant.ofEpochSecond(it.fetchedAtEpoch), clock.instant())
            }
        } catch (error: CancellationException) { throw error }
        catch (_: Exception) { null }
    }

    suspend fun fetch(city: City): WeatherUiModel {
        val response = weatherApi.forecast(city.latitude, city.longitude)
        val fetchedAt = clock.instant()
        val weather = response.toWeather(city, fetchedAt, fetchedAt)
        // A disk failure must not discard a successful live response.
        try { cache.write(CachedWeather(city, response, fetchedAt.epochSecond)) }
        catch (error: CancellationException) { throw error }
        catch (_: Exception) { /* Live weather is still available for this session. */ }
        return weather
    }

    suspend fun search(query: String): List<City> = geocodingApi.search(query.trim())
        .results.orEmpty().mapNotNull { it.toCity() }.distinctBy { it.latitude to it.longitude }
}

fun weatherError(error: Throwable): String = when (error) {
    is IOException -> "Unable to update weather. Check your connection and try again."
    is HttpException -> if (error.code() == 429) "The weather service is busy. Please try again shortly." else "Unable to update weather. Please try again."
    else -> "Unable to update weather. Please try again."
}
