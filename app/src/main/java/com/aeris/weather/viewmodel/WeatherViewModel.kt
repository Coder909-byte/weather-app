package com.aeris.weather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aeris.weather.data.local.PreferencesManager
import com.aeris.weather.data.location.LocationProvider
import com.aeris.weather.data.repository.WeatherRepository
import com.aeris.weather.data.repository.weatherError
import com.aeris.weather.domain.model.*
import com.aeris.weather.util.isStale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

data class WeatherScreenState(
    val city: City = City.Default,
    val weather: WeatherUiModel? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val locating: Boolean = false,
    val notice: String? = null,
)

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val preferencesManager: PreferencesManager,
    private val locationProvider: LocationProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(WeatherScreenState())
    val state: StateFlow<WeatherScreenState> = _state.asStateFlow()
    val preferences = preferencesManager.preferences.stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())
    private var weatherJob: Job? = null
    private var locationJob: Job? = null
    private var initialized = false

    init {
        viewModelScope.launch {
            val saved = preferencesManager.preferences.first()
            load(saved.city, allowCache = true)
            initialized = true
        }
    }

    private fun load(city: City, allowCache: Boolean = false) {
        weatherJob?.cancel()
        val previous = _state.value.weather?.takeIf { it.city.sameCoordinates(city) }
        _state.update { it.copy(city = city, weather = previous, loading = previous == null, refreshing = previous != null, error = null) }
        weatherJob = viewModelScope.launch {
            try {
                val cached = if (allowCache) repository.cached(city) else null
                if (cached != null) _state.update { it.copy(weather = cached, loading = false, refreshing = isStale(cached.fetchedAt, Instant.now())) }
                if (cached == null || isStale(cached.fetchedAt, Instant.now())) {
                    val weather = repository.fetch(city)
                    _state.update { it.copy(weather = weather, loading = false, refreshing = false, error = null) }
                }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { _state.update { it.copy(loading = false, refreshing = false, error = weatherError(error)) } }
        }
    }

    fun refresh() { if (initialized && weatherJob?.isActive != true) load(_state.value.city) }
    fun refreshIfStale() {
        val weather = _state.value.weather ?: return
        if (isStale(weather.fetchedAt, Instant.now())) refresh()
    }
    fun selectCity(city: City) {
        locationJob?.cancel()
        _state.update { it.copy(locating = false) }
        load(city)
        savePreference { preferencesManager.setCity(city) }
    }
    fun setUnit(unit: TemperatureUnit) = savePreference { preferencesManager.setUnit(unit) }
    fun setAppearance(appearance: Appearance) = savePreference { preferencesManager.setAppearance(appearance) }
    private fun savePreference(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { showNotice("Couldn't save this preference. Please try again.") }
        }
    }

    fun useCurrentLocation() {
        if (locationJob?.isActive == true) return
        _state.update { it.copy(locating = true, notice = null) }
        locationJob = viewModelScope.launch {
            try {
                val city = locationProvider.currentCity()
                preferencesManager.setCity(city)
                load(city)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                showNotice(when (error) {
                    is SecurityException -> "Location access is off. Enable approximate location in Android app settings, or choose a city manually."
                    is IllegalStateException -> error.message ?: "Your location is unavailable. Choose a city manually."
                    else -> "Your location is unavailable. Try again or choose a city manually."
                })
            }
            finally { _state.update { it.copy(locating = false) } }
        }
    }

    fun showNotice(message: String) { _state.update { it.copy(notice = message) } }
    fun clearNotice() { _state.update { it.copy(notice = null) } }
}
