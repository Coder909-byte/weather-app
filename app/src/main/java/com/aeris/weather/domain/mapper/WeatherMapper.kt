package com.aeris.weather.domain.mapper

import com.aeris.weather.data.remote.dto.CityDto
import com.aeris.weather.data.remote.dto.WeatherDto
import com.aeris.weather.domain.model.*
import com.aeris.weather.util.WeatherCodeMapper
import com.aeris.weather.util.hourStart
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class EmptyWeatherException : Exception("The weather service returned no current conditions.")

fun CityDto.toCity(): City? {
    val cityName = name?.takeIf { it.isNotBlank() } ?: return null
    val lat = latitude?.takeIf { it.isFinite() && it in -90.0..90.0 } ?: return null
    val lon = longitude?.takeIf { it.isFinite() && it in -180.0..180.0 } ?: return null
    return City(id, cityName, admin1, country.orEmpty(), lat, lon, timezone)
}

fun WeatherDto.toWeather(city: City, fetchedAt: Instant, now: Instant): WeatherUiModel {
    val current = current ?: throw EmptyWeatherException()
    val observedAt = current.time?.let(Instant::ofEpochSecond) ?: throw EmptyWeatherException()
    if (current.temperature == null && current.code == null) throw EmptyWeatherException()
    val zone = runCatching { ZoneId.of(timezone ?: city.timezone) }.getOrElse {
        runCatching { ZoneOffset.ofTotalSeconds(utcOffsetSeconds ?: 0) }.getOrDefault(ZoneOffset.UTC)
    }
    val hourlyRows = hourly?.time.orEmpty().mapIndexedNotNull { index, epoch ->
        val time = epoch?.let(Instant::ofEpochSecond) ?: return@mapIndexedNotNull null
        if (time < hourStart(now, zone)) return@mapIndexedNotNull null
        HourlyForecast(time, hourly?.temperature?.getOrNull(index), hourly?.rainProbability?.getOrNull(index),
            WeatherCodeMapper.map(hourly?.code?.getOrNull(index)), hourly?.isDay?.getOrNull(index) == 1)
    }.take(24)
    val dailyRows = daily?.time.orEmpty().mapIndexedNotNull { index, epoch ->
        val date = epoch?.let { Instant.ofEpochSecond(it).atZone(zone).toLocalDate() } ?: return@mapIndexedNotNull null
        DailyForecast(date, daily?.minimum?.getOrNull(index), daily?.maximum?.getOrNull(index),
            WeatherCodeMapper.map(daily?.code?.getOrNull(index)), daily?.rainProbability?.getOrNull(index),
            daily?.sunrise?.getOrNull(index)?.let(Instant::ofEpochSecond),
            daily?.sunset?.getOrNull(index)?.let(Instant::ofEpochSecond), daily?.uv?.getOrNull(index))
    }.take(7)
    return WeatherUiModel(city, zone, observedAt, fetchedAt, current.temperature, current.feelsLike,
        WeatherCodeMapper.map(current.code), current.humidity, current.windSpeed, current.windDirection,
        current.precipitation, current.isDay == 1, hourlyRows, dailyRows)
}
