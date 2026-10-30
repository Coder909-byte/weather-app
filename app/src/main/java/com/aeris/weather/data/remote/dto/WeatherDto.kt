package com.aeris.weather.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WeatherDto(
    val timezone: String? = null,
    @SerializedName("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    val current: CurrentDto? = null,
    val hourly: HourlyDto? = null,
    val daily: DailyDto? = null,
)
data class CurrentDto(
    val time: Long? = null,
    @SerializedName("temperature_2m") val temperature: Double? = null,
    @SerializedName("relative_humidity_2m") val humidity: Int? = null,
    @SerializedName("apparent_temperature") val feelsLike: Double? = null,
    @SerializedName("is_day") val isDay: Int? = null,
    val precipitation: Double? = null,
    @SerializedName("weather_code") val code: Int? = null,
    @SerializedName("wind_speed_10m") val windSpeed: Double? = null,
    @SerializedName("wind_direction_10m") val windDirection: Int? = null,
)
data class HourlyDto(
    val time: List<Long?>? = null,
    @SerializedName("temperature_2m") val temperature: List<Double?>? = null,
    @SerializedName("precipitation_probability") val rainProbability: List<Int?>? = null,
    @SerializedName("weather_code") val code: List<Int?>? = null,
    @SerializedName("relative_humidity_2m") val humidity: List<Int?>? = null,
    @SerializedName("is_day") val isDay: List<Int?>? = null,
)
data class DailyDto(
    val time: List<Long?>? = null,
    @SerializedName("temperature_2m_max") val maximum: List<Double?>? = null,
    @SerializedName("temperature_2m_min") val minimum: List<Double?>? = null,
    @SerializedName("weather_code") val code: List<Int?>? = null,
    val sunrise: List<Long?>? = null,
    val sunset: List<Long?>? = null,
    @SerializedName("uv_index_max") val uv: List<Double?>? = null,
    @SerializedName("precipitation_probability_max") val rainProbability: List<Int?>? = null,
)

data class GeocodingDto(val results: List<CityDto>? = null)
data class CityDto(
    val id: Long? = null,
    val name: String? = null,
    val admin1: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
)
