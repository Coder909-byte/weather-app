package com.aeris.weather.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aeris.weather.domain.model.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore by preferencesDataStore("aeris_settings")

class PreferencesManager(context: Context, private val gson: Gson = Gson()) {
    private val store = context.settingsStore
    private val unitKey = stringPreferencesKey("temperature_unit")
    private val appearanceKey = stringPreferencesKey("appearance")
    private val cityKey = stringPreferencesKey("selected_city")

    val preferences = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { values ->
        UserPreferences(
            unit = runCatching { TemperatureUnit.valueOf(values[unitKey].orEmpty()) }.getOrDefault(TemperatureUnit.CELSIUS),
            appearance = runCatching { Appearance.valueOf(values[appearanceKey].orEmpty()) }.getOrDefault(Appearance.SYSTEM),
            city = runCatching {
                val city = gson.fromJson(values[cityKey], City::class.java) ?: return@runCatching null
                require(city.name.isNotBlank())
                requireNotNull(city.country)
                require(city.latitude.isFinite() && city.longitude.isFinite())
                require(city.latitude in -90.0..90.0 && city.longitude in -180.0..180.0)
                city
            }.getOrNull() ?: City.Default,
        )
    }

    suspend fun setUnit(unit: TemperatureUnit) { store.edit { it[unitKey] = unit.name } }
    suspend fun setAppearance(appearance: Appearance) { store.edit { it[appearanceKey] = appearance.name } }
    suspend fun setCity(city: City) { store.edit { it[cityKey] = gson.toJson(city) } }
}
