package com.aeris.weather

import com.aeris.weather.data.remote.dto.*
import com.aeris.weather.domain.mapper.*
import com.aeris.weather.domain.model.*
import com.aeris.weather.util.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WeatherLogicTest {
    private val now = Instant.parse("2026-10-30T08:45:00Z")
    private val city = City.Default

    @Test fun `all documented WMO codes have descriptions and conditions`() {
        val groups = mapOf(
            WeatherKind.CLEAR to listOf(0, 1), WeatherKind.CLOUD to listOf(2), WeatherKind.OVERCAST to listOf(3),
            WeatherKind.FOG to listOf(45, 48), WeatherKind.RAIN to listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82),
            WeatherKind.SNOW to listOf(71, 73, 75, 77, 85, 86), WeatherKind.STORM to listOf(95, 96, 99),
        )
        groups.forEach { (kind, codes) -> codes.forEach { assertEquals("Code $it", kind, WeatherCodeMapper.map(it).kind) } }
        assertEquals("Thunderstorm with hail", WeatherCodeMapper.map(99).description)
    }

    @Test fun `unknown and absent codes use a neutral fallback`() {
        assertEquals(WeatherKind.UNKNOWN, WeatherCodeMapper.map(999).kind)
        assertEquals(WeatherKind.UNKNOWN, WeatherCodeMapper.map(null).kind)
    }

    @Test fun `converts celsius consistently including negative and zero values`() {
        assertEquals(32.0, UnitConverter.convert(0.0, TemperatureUnit.FAHRENHEIT), .001)
        assertEquals(-40.0, UnitConverter.convert(-40.0, TemperatureUnit.FAHRENHEIT), .001)
        assertEquals(82.4, UnitConverter.convert(28.0, TemperatureUnit.FAHRENHEIT), .001)
        assertEquals("28°", UnitConverter.temperature(28.0, TemperatureUnit.CELSIUS))
        assertEquals("82°", UnitConverter.temperature(28.0, TemperatureUnit.FAHRENHEIT))
        assertEquals("—", UnitConverter.temperature(null, TemperatureUnit.CELSIUS))
        assertEquals("—", UnitConverter.temperature(Double.NaN, TemperatureUnit.CELSIUS))
    }

    @Test fun `hourly forecast starts at selected city current hour in fractional offset zone`() {
        // 08:45 UTC = 14:15 India. Local 14:00 is 08:30 UTC.
        val times = (0..47).map { Instant.parse("2026-10-29T18:30:00Z").plusSeconds(it * 3600L).epochSecond }
        val dto = WeatherDto(timezone = "Asia/Kolkata", current = CurrentDto(now.epochSecond, temperature = 28.0, code = 2),
            hourly = HourlyDto(time = times, temperature = List(48) { 20.0 + it }, code = List(48) { 0 }, isDay = List(48) { if (it in 6..17) 1 else 0 }))
        val model = dto.toWeather(city, now, now)
        assertEquals(24, model.hourly.size)
        assertEquals(Instant.parse("2026-10-30T08:30:00Z"), model.hourly.first().time)
        assertEquals(34.0, model.hourly.first().temperature!!, .001)
        assertTrue(model.hourly.first().isDay)
        assertFalse(model.hourly[6].isDay)
    }

    @Test fun `missing and uneven API arrays preserve available information`() {
        val dto = WeatherDto(timezone = "Asia/Kolkata", current = CurrentDto(now.epochSecond, temperature = 28.0),
            hourly = HourlyDto(time = listOf(now.epochSecond, now.plusSeconds(3600).epochSecond, null), temperature = listOf(28.0)),
            daily = DailyDto(time = listOf(Instant.parse("2026-10-29T18:30:00Z").epochSecond)))
        val model = dto.toWeather(city, now, now)
        assertEquals(2, model.hourly.size)
        assertNull(model.hourly[1].temperature)
        assertNull(model.humidity)
        assertNull(model.daily.first().sunrise)
        assertEquals(LocalDate.of(2026, 10, 30), model.daily.first().date)
    }

    @Test fun `forecast location determines dates independently of device zone`() {
        val time = Instant.parse("2026-10-30T23:00:00Z")
        val model = WeatherDto(timezone = "Asia/Tokyo", current = CurrentDto(time.epochSecond, temperature = 21.0),
            daily = DailyDto(time = listOf(time.epochSecond))).toWeather(city, time, time)
        assertEquals(LocalDate.of(2026, 10, 31), model.daily.first().date)
        assertEquals("08:00", time.formatted("HH:mm", model.zone))
    }

    @Test fun `epoch hours survive daylight saving fall back without duplicate instants`() {
        val time = Instant.parse("2026-11-01T05:15:00Z")
        val times = listOf("2026-11-01T05:00:00Z", "2026-11-01T06:00:00Z", "2026-11-01T07:00:00Z").map { Instant.parse(it).epochSecond }
        val model = WeatherDto(timezone = "America/New_York", current = CurrentDto(time.epochSecond, temperature = 10.0),
            hourly = HourlyDto(time = times)).toWeather(city, time, time)
        assertEquals(3, model.hourly.size)
        assertEquals(3, model.hourly.map { it.time }.distinct().size)
        assertEquals("01:00", model.hourly[0].time.formatted("HH:mm", model.zone))
        assertEquals("01:00", model.hourly[1].time.formatted("HH:mm", model.zone))
    }

    @Test(expected = EmptyWeatherException::class) fun `empty API payload fails explicitly`() {
        WeatherDto().toWeather(city, now, now)
    }

    @Test fun `geocoding mapping rejects missing invalid coordinates and blank cities`() {
        assertNull(CityDto(name = "Paris").toCity())
        assertNull(CityDto(name = " ", latitude = 48.8, longitude = 2.3).toCity())
        assertNull(CityDto(name = "Paris", latitude = 148.8, longitude = 2.3).toCity())
        assertNull(CityDto(name = "Paris", latitude = Double.NaN, longitude = 2.3).toCity())
        val mapped = CityDto(name = "Paris", admin1 = "Île-de-France", country = "France", latitude = 48.8, longitude = 2.3).toCity()!!
        assertEquals("Île-de-France, France", mapped.subtitle)
        assertEquals(48.8, mapped.latitude, .001)
    }

    @Test fun `weekly temperature bars share a common scale`() {
        val first = day(10.0, 20.0)
        val second = day(15.0, 30.0)
        val all = listOf(first, second)
        assertEquals(RangePosition(0f, .5f), temperatureRange(first, all))
        assertEquals(RangePosition(.25f, 1f), temperatureRange(second, all))
        assertEquals(RangePosition(0f, 1f), temperatureRange(day(10.0, 10.0), listOf(day(10.0, 10.0))))
        assertNull(temperatureRange(day(null, null), all))
    }

    @Test fun `sun arc has no daylight indicator outside sunrise and sunset`() {
        val sunrise = Instant.parse("2026-10-30T01:00:00Z")
        val sunset = Instant.parse("2026-10-30T13:00:00Z")
        assertEquals(.5f, sunProgress(Instant.parse("2026-10-30T07:00:00Z"), sunrise, sunset)!!, .001f)
        assertNull(sunProgress(sunrise.minusSeconds(1), sunrise, sunset))
        assertNull(sunProgress(sunset.plusSeconds(1), sunrise, sunset))
        assertNull(sunProgress(now, null, sunset))
        assertNull(sunProgress(now, sunset, sunrise))
    }

    @Test fun `cache freshness uses actual fetch time and a fifteen minute threshold`() {
        assertFalse(isStale(now.minusSeconds(899), now))
        assertTrue(isStale(now.minusSeconds(900), now))
        assertTrue(isStale(now.plusSeconds(60), now))
    }

    @Test fun `wind directions handle north wraparound`() {
        assertEquals("N", compass(359)); assertEquals("E", compass(90))
        assertEquals("SW", compass(225)); assertEquals("—", compass(null))
    }

    private fun day(low: Double?, high: Double?) = DailyForecast(LocalDate.of(2026, 10, 30), low, high, WeatherCodeMapper.map(0), null, null, null, null)
}
