package com.aeris.weather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeris.weather.domain.model.*
import com.aeris.weather.ui.components.*
import com.aeris.weather.util.*
import com.aeris.weather.viewmodel.WeatherScreenState
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: WeatherScreenState, unit: TemperatureUnit, now: Instant, onSearch: () -> Unit, onSettings: () -> Unit, onRefresh: () -> Unit, onLocation: () -> Unit) {
    var details by rememberSaveable { mutableStateOf(false) }
    val weather = state.weather
    val uri = LocalUriHandler.current
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
            item {
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("AERIS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, letterSpacing = 5.sp)
                        Text("WEATHER, REFINED.", Modifier.padding(top = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, letterSpacing = 1.5.sp)
                    }
                    IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Search locations", Modifier.size(22.dp)) }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Tune, "Settings", Modifier.size(22.dp)) }
                }
                if (state.locating) LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
            }
            when {
                weather != null -> {
                    item { WeatherHero(weather, unit, now, onSearch) }
                    item {
                        val stale = isStale(weather.fetchedAt, now)
                        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (stale) Icons.Outlined.History else Icons.Outlined.CheckCircleOutline, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("${if (stale) "Saved forecast" else "Updated"} · ${weather.fetchedAt.formatted("MMM d, h:mm a", weather.zone)}", Modifier.weight(1f).padding(start = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(onClick = onRefresh, enabled = !state.refreshing, modifier = Modifier.size(48.dp)) {
                                if (state.refreshing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 1.5.dp) else Icon(Icons.Outlined.Refresh, "Refresh weather", Modifier.size(19.dp))
                            }
                        }
                        if (state.error != null) ErrorNotice(state.error, onRefresh)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(28.dp))
                    }
                    item { HourlyForecastSection(weather, unit, now); Spacer(Modifier.height(32.dp)) }
                    item { DailyForecastSection(weather, unit, now); Spacer(Modifier.height(24.dp)) }
                    item { WeatherDetailsGrid(weather, unit, now) { details = true }; Spacer(Modifier.height(32.dp)) }
                    item { SunProgressCard(weather, now); Spacer(Modifier.height(24.dp)) }
                }
                state.loading -> item { LoadingWeather(state.city.name) }
                else -> item {
                    Spacer(Modifier.height(70.dp))
                    WeatherGlyph(WeatherKind.UNKNOWN, modifier = Modifier.size(56.dp))
                    Text("A little out of reach.", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.headlineLarge)
                    Text(state.error ?: "No weather information is available.", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onRefresh, modifier = Modifier.padding(top = 24.dp)) { Text("Try again") }
                    TextButton(onClick = onSearch) { Text("Choose another city") }
                    Spacer(Modifier.height(48.dp))
                }
            }
            item {
                if (!state.city.isCurrentLocation) {
                    OutlinedButton(onClick = onLocation, enabled = !state.locating, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.Outlined.MyLocation, null, Modifier.size(16.dp)); Spacer(Modifier.width(10.dp)); Text(if (state.locating) "Finding your location…" else "Use my current location")
                    }
                    Text("Or explore any city using search. Location access is optional.", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { uri.openUri("https://open-meteo.com/") }) { Text("Weather data by Open-Meteo", style = MaterialTheme.typography.bodySmall) }
                    Text("A little perspective on your day.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    if (details && weather != null) WeatherDetailsSheet(weather, unit, now) { details = false }
}

@Composable
private fun ErrorNotice(message: String, retry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = retry) { Text("Retry") }
    }
}

@Composable
private fun LoadingWeather(cityName: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Eyebrow("Finding your forecast")
        Text(cityName, style = MaterialTheme.typography.headlineLarge)
        CircularProgressIndicator(Modifier.padding(vertical = 32.dp).size(40.dp), strokeWidth = 2.dp)
        Text("A clearer view of the day ahead.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        repeat(3) { Box(Modifier.fillMaxWidth().height(72.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f), RoundedCornerShape(20.dp))) }
    }
}
