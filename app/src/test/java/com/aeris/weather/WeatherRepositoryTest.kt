package com.aeris.weather

import com.aeris.weather.data.local.*
import com.aeris.weather.data.remote.*
import com.aeris.weather.data.remote.dto.*
import com.aeris.weather.data.repository.*
import com.aeris.weather.domain.model.City
import com.aeris.weather.viewmodel.SearchViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRepositoryTest {
    private val now = Instant.parse("2026-10-30T08:45:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val payload = WeatherDto(timezone = "Asia/Kolkata", current = CurrentDto(now.epochSecond, temperature = 28.0, code = 2))

    @Test fun `Retrofit sends forecast query parameters and decodes nullable measurements`() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody(Gson().toJson(payload)))
            val api = retrofit(server).create(WeatherApiService::class.java)
            val result = api.forecast(31.634, 74.8723)
            val url = server.takeRequest().requestUrl!!
            assertEquals("/v1/forecast", url.encodedPath)
            assertEquals("31.634", url.queryParameter("latitude"))
            assertEquals("auto", url.queryParameter("timezone"))
            assertEquals("7", url.queryParameter("forecast_days"))
            assertEquals("unixtime", url.queryParameter("timeformat"))
            assertEquals("celsius", url.queryParameter("temperature_unit"))
            assertTrue(url.queryParameter("current")!!.contains("wind_direction_10m"))
            assertTrue(url.queryParameter("hourly")!!.contains("is_day"))
            assertEquals(28.0, result.current!!.temperature!!, .001)
            assertNull(result.current!!.humidity)
        } finally { server.shutdown() }
    }

    @Test fun `search uses real geocoding route and handles absent results`() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setBody("{}"))
            val service = retrofit(server).create(GeocodingApiService::class.java)
            val repository = WeatherRepository(FakeWeatherApi(), service, MemoryCache(), clock)
            assertTrue(repository.search("  New York  ").isEmpty())
            val url = server.takeRequest().requestUrl!!
            assertEquals("/v1/search", url.encodedPath)
            assertEquals("New York", url.queryParameter("name"))
            assertEquals("10", url.queryParameter("count"))
        } finally { server.shutdown() }
    }

    @Test fun `successful fetch persists raw data and original fetch timestamp`() = runTest {
        val cache = MemoryCache()
        val api = FakeWeatherApi(payload)
        val repository = WeatherRepository(api, FakeGeocoding(), cache, clock)
        val live = repository.fetch(City.Default)
        val restored = repository.cached(City.Default)!!
        assertEquals(1, api.calls)
        assertEquals(now, live.fetchedAt)
        assertEquals(now, restored.fetchedAt)
        assertEquals(live.temperature, restored.temperature)
        assertNull(repository.cached(City.Default.copy(latitude = 0.0)))
    }

    @Test fun `network failure retains previously saved weather and does not relabel timestamp`() = runTest {
        val cache = MemoryCache(CachedWeather(City.Default, payload, now.minusSeconds(3600).epochSecond))
        val api = FakeWeatherApi(error = IOException("offline"))
        val repository = WeatherRepository(api, FakeGeocoding(), cache, clock)
        try { repository.fetch(City.Default); fail("Expected network failure") } catch (_: IOException) { }
        assertEquals(now.minusSeconds(3600), repository.cached(City.Default)!!.fetchedAt)
        assertTrue(weatherError(IOException()).contains("connection"))
    }

    @Test fun `disk write failure does not erase successful live response`() = runTest {
        val cache = object : WeatherCache {
            override suspend fun read(): CachedWeather? = null
            override suspend fun write(weather: CachedWeather) { throw IOException("disk full") }
        }
        assertEquals(28.0, WeatherRepository(FakeWeatherApi(payload), FakeGeocoding(), cache, clock).fetch(City.Default).temperature!!, .001)
    }

    @Test fun `cancellation propagates through repository`() = runTest {
        val repository = WeatherRepository(FakeWeatherApi(error = CancellationException()), FakeGeocoding(), MemoryCache(), clock)
        try { repository.fetch(City.Default); fail("Cancellation swallowed") } catch (_: CancellationException) { }
    }

    @Test fun `search debounces and cancels old requests before publishing new results`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var searchScope: CoroutineScope? = null
        try {
            val calls = mutableListOf<String>()
            val geo = object : FakeGeocoding() {
                override suspend fun search(name: String, count: Int, language: String, format: String): GeocodingDto {
                    calls += name
                    delay(if (name == "London") 2_000 else 20)
                    return GeocodingDto(listOf(CityDto(name = name, latitude = 1.0, longitude = 2.0)))
                }
            }
            val vm = SearchViewModel(WeatherRepository(FakeWeatherApi(payload), geo, MemoryCache(), clock))
            searchScope = vm.viewModelScope
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect() }
            vm.search("L"); runCurrent(); advanceTimeBy(500); runCurrent()
            assertTrue(calls.isEmpty())
            vm.search("Lon"); runCurrent(); advanceTimeBy(200)
            vm.search("London"); runCurrent(); advanceTimeBy(351); runCurrent()
            assertEquals(listOf("London"), calls)
            vm.search("Tokyo"); runCurrent(); advanceTimeBy(400); runCurrent()
            assertEquals("Tokyo", vm.state.value.results.single().name)
            advanceTimeBy(3_000); runCurrent()
            assertEquals("Tokyo", vm.state.value.results.single().name)
            vm.search(""); runCurrent()
            assertTrue(vm.state.value.results.isEmpty())
        } finally {
            // Dispose collectors and the ViewModel before removing the JVM's Main dispatcher.
            backgroundScope.cancel()
            searchScope?.cancel()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    private fun retrofit(server: MockWebServer) = Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build()
    private class MemoryCache(var entry: CachedWeather? = null) : WeatherCache {
        override suspend fun read() = entry
        override suspend fun write(weather: CachedWeather) { entry = weather }
    }
    private class FakeWeatherApi(private val payload: WeatherDto = WeatherDto(), private val error: Exception? = null) : WeatherApiService {
        var calls = 0
        override suspend fun forecast(latitude: Double, longitude: Double, current: String, hourly: String, daily: String, timezone: String, days: Int, temperatureUnit: String, windUnit: String, timeFormat: String): WeatherDto {
            calls++; error?.let { throw it }; return payload
        }
    }
    private open class FakeGeocoding : GeocodingApiService {
        override suspend fun search(name: String, count: Int, language: String, format: String) = GeocodingDto()
    }
}
