package com.aeris.weather.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class City(
    val id: Long? = null,
    val name: String,
    val region: String? = null,
    val country: String = "",
    val latitude: Double,
    val longitude: Double,
    val timezone: String? = null,
    val isCurrentLocation: Boolean = false,
) {
    val subtitle: String get() = listOfNotNull(region?.takeIf { it != name }, country.takeIf { it.isNotBlank() }).joinToString(", ")
    fun sameCoordinates(other: City) = latitude == other.latitude && longitude == other.longitude

    companion object {
        // A starting location, never a source of weather values.
        val Default = City(name = "Amritsar", region = "Punjab", country = "India", latitude = 31.634, longitude = 74.8723, timezone = "Asia/Kolkata")
    }
}

enum class WeatherKind { CLEAR, CLOUD, OVERCAST, FOG, RAIN, SNOW, STORM, UNKNOWN }
data class WeatherCondition(val description: String, val kind: WeatherKind)
data class HourlyForecast(
    val time: Instant,
    val temperature: Double?,
    val precipitationProbability: Int?,
    val condition: WeatherCondition,
    val isDay: Boolean,
)
data class DailyForecast(
    val date: LocalDate,
    val minimumTemperature: Double?,
    val maximumTemperature: Double?,
    val condition: WeatherCondition,
    val precipitationProbability: Int?,
    val sunrise: Instant?,
    val sunset: Instant?,
    val uvIndex: Double?,
)
data class WeatherUiModel(
    val city: City,
    val zone: ZoneId,
    val observedAt: Instant,
    val fetchedAt: Instant,
    val temperature: Double?,
    val feelsLike: Double?,
    val condition: WeatherCondition,
    val humidity: Int?,
    val windSpeed: Double?,
    val windDirection: Int?,
    val precipitation: Double?,
    val isDay: Boolean,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
)

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }
enum class Appearance { SYSTEM, LIGHT, DARK }
data class UserPreferences(
    val unit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val appearance: Appearance = Appearance.SYSTEM,
    val city: City = City.Default,
)
