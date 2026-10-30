package com.aeris.weather.util

import com.aeris.weather.domain.model.DailyForecast
import com.aeris.weather.domain.model.TemperatureUnit
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

object UnitConverter {
    fun convert(celsius: Double, unit: TemperatureUnit): Double =
        if (unit == TemperatureUnit.FAHRENHEIT) celsius * 9.0 / 5.0 + 32.0 else celsius

    fun temperature(celsius: Double?, unit: TemperatureUnit): String =
        celsius?.takeIf { it.isFinite() }?.let { "${convert(it, unit).roundToInt()}°" } ?: "—"
}

fun Instant.formatted(pattern: String, zone: ZoneId): String = DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).withZone(zone).format(this)
fun Double?.decimal(): String = this?.takeIf { it.isFinite() }?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: "—"
fun compass(degrees: Int?): String = degrees?.let { listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")[((it.mod(360) + 22.5) / 45).toInt() % 8] } ?: "—"
fun uvLabel(value: Double?): String = when {
    value == null -> "Unavailable"
    value < 3 -> "Low exposure"
    value < 6 -> "Moderate · use SPF"
    value < 8 -> "High · use sun protection"
    value < 11 -> "Very high · seek shade"
    else -> "Extreme · limit sun exposure"
}
fun isStale(fetchedAt: Instant, now: Instant): Boolean = fetchedAt.plusSeconds(15 * 60) <= now || fetchedAt > now
fun hourStart(now: Instant, zone: ZoneId): Instant = now.atZone(zone).truncatedTo(ChronoUnit.HOURS).toInstant()

data class RangePosition(val start: Float, val end: Float)
fun temperatureRange(day: DailyForecast, all: List<DailyForecast>): RangePosition? {
    val low = day.minimumTemperature ?: return null
    val high = day.maximumTemperature ?: return null
    val min = all.mapNotNull { it.minimumTemperature }.minOrNull() ?: return null
    val max = all.mapNotNull { it.maximumTemperature }.maxOrNull() ?: return null
    if (max <= min) return RangePosition(0f, 1f)
    return RangePosition(((low - min) / (max - min)).toFloat().coerceIn(0f, 1f), ((high - min) / (max - min)).toFloat().coerceIn(0f, 1f))
}

fun sunProgress(now: Instant, sunrise: Instant?, sunset: Instant?): Float? {
    if (sunrise == null || sunset == null || sunset <= sunrise || now < sunrise || now > sunset) return null
    return ((now.epochSecond - sunrise.epochSecond).toFloat() / (sunset.epochSecond - sunrise.epochSecond)).coerceIn(0f, 1f)
}
