package com.aeris.weather.util

import com.aeris.weather.domain.model.WeatherCondition
import com.aeris.weather.domain.model.WeatherKind

object WeatherCodeMapper {
    fun map(code: Int?): WeatherCondition = when (code) {
        0 -> WeatherCondition("Clear sky", WeatherKind.CLEAR)
        1 -> WeatherCondition("Mainly clear", WeatherKind.CLEAR)
        2 -> WeatherCondition("Partly cloudy", WeatherKind.CLOUD)
        3 -> WeatherCondition("Overcast", WeatherKind.OVERCAST)
        45, 48 -> WeatherCondition("Fog", WeatherKind.FOG)
        51, 53, 55 -> WeatherCondition("Drizzle", WeatherKind.RAIN)
        56, 57 -> WeatherCondition("Freezing drizzle", WeatherKind.RAIN)
        61, 63, 65 -> WeatherCondition("Rain", WeatherKind.RAIN)
        66, 67 -> WeatherCondition("Freezing rain", WeatherKind.RAIN)
        71, 73, 75 -> WeatherCondition("Snow", WeatherKind.SNOW)
        77 -> WeatherCondition("Snow grains", WeatherKind.SNOW)
        80, 81, 82 -> WeatherCondition("Rain showers", WeatherKind.RAIN)
        85, 86 -> WeatherCondition("Snow showers", WeatherKind.SNOW)
        95 -> WeatherCondition("Thunderstorm", WeatherKind.STORM)
        96, 99 -> WeatherCondition("Thunderstorm with hail", WeatherKind.STORM)
        else -> WeatherCondition("Conditions unavailable", WeatherKind.UNKNOWN)
    }
}
