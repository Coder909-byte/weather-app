package com.aeris.weather.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aeris.weather.BuildConfig
import com.aeris.weather.domain.model.*
import com.aeris.weather.ui.components.*

@Composable
fun SettingsScreen(preferences: UserPreferences, locating: Boolean, onUnit: (TemperatureUnit) -> Unit, onAppearance: (Appearance) -> Unit, onLocation: () -> Unit, onSearch: () -> Unit, onBack: () -> Unit) {
    val uri = LocalUriHandler.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item { PageHeading("Make it yours", "The weather, your way.", onBack) }
        item {
            SectionHeading("Temperature")
            QuietCard(Modifier.fillMaxWidth()) {
                SettingChoice("Celsius", "°C", preferences.unit == TemperatureUnit.CELSIUS) { onUnit(TemperatureUnit.CELSIUS) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingChoice("Fahrenheit", "°F", preferences.unit == TemperatureUnit.FAHRENHEIT) { onUnit(TemperatureUnit.FAHRENHEIT) }
            }
        }
        item {
            SectionHeading("Appearance")
            QuietCard(Modifier.fillMaxWidth()) {
                Appearance.entries.forEachIndexed { index, appearance ->
                    SettingChoice(when (appearance) { Appearance.SYSTEM -> "System default"; Appearance.LIGHT -> "Light"; Appearance.DARK -> "Dark" }, "", preferences.appearance == appearance) { onAppearance(appearance) }
                    if (index < Appearance.entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        item {
            SectionHeading("Location")
            QuietCard(Modifier.fillMaxWidth()) {
                Text(preferences.city.name, style = MaterialTheme.typography.titleLarge)
                Text(preferences.city.subtitle, Modifier.padding(top = 4.dp, bottom = 16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onLocation, enabled = !locating) {
                    Icon(Icons.Outlined.MyLocation, null, Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text(if (locating) "Finding location…" else "Use current location")
                }
                TextButton(onClick = onSearch) {
                    Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Text("Choose a city manually")
                }
                Text("Approximate location is used only when you request it. No background tracking. Your selected coordinates are sent to Open-Meteo to retrieve the forecast.", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionHeading("About")
            QuietCard(Modifier.fillMaxWidth()) {
                Text("AERIS", style = MaterialTheme.typography.titleLarge)
                Text("Weather, Refined. · Version ${BuildConfig.VERSION_NAME}", Modifier.padding(top = 6.dp, bottom = 12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { uri.openUri("https://open-meteo.com/") }) { Text("Weather data by Open-Meteo"); Icon(Icons.Outlined.NorthEast, null, Modifier.padding(start = 8.dp).size(14.dp)) }
                TextButton(onClick = { uri.openUri("https://creativecommons.org/licenses/by/4.0/") }) { Text("Weather data: CC BY 4.0") }
                Text("Typography: Manrope · SIL Open Font License\nDesigned for a calmer everyday.", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingChoice(label: String, suffix: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.RadioButton, onClick = onClick).padding(vertical = 5.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (suffix.isNotEmpty()) Text(suffix, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(start = 12.dp))
    }
}
