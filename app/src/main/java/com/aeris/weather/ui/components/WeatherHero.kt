package com.aeris.weather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeris.weather.domain.model.*
import com.aeris.weather.util.*
import java.time.Instant

@Composable
fun WeatherHero(weather: WeatherUiModel, unit: TemperatureUnit, now: Instant, search: () -> Unit) {
    val tint = when (weather.condition.kind) {
        WeatherKind.CLEAR -> if (weather.isDay) Color(0xFF8B9C93) else Color(0xFF435878)
        WeatherKind.CLOUD, WeatherKind.OVERCAST -> Color(0xFF6B8797)
        WeatherKind.FOG -> Color(0xFF879392)
        WeatherKind.RAIN -> Color(0xFF58798C)
        WeatherKind.SNOW -> Color(0xFFB3CFDA)
        WeatherKind.STORM -> Color(0xFF52546F)
        WeatherKind.UNKNOWN -> Color(0xFF6B8797)
    }
    Column(Modifier.fillMaxWidth().background(Brush.radialGradient(listOf(tint.copy(alpha = .12f), Color.Transparent), radius = 850f)).padding(top = 32.dp, bottom = 28.dp)) {
        Row(Modifier.clickable(onClickLabel = "Choose location", onClick = search).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (weather.city.isCurrentLocation) Icon(Icons.Outlined.NearMe, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    Eyebrow(if (weather.city.isCurrentLocation) "Your location" else weather.city.subtitle.ifBlank { "Selected location" })
                }
                Spacer(Modifier.height(7.dp))
                Text(weather.city.name, style = MaterialTheme.typography.headlineLarge)
            }
            Icon(Icons.Outlined.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(now.formatted("EEEE, MMMM d", weather.zone), Modifier.padding(top = 5.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            WeatherArtwork(weather.condition.kind, weather.isDay, Modifier.align(Alignment.CenterEnd).size((maxWidth * .55f).coerceAtMost(210.dp), 180.dp))
            Text(UnitConverter.temperature(weather.temperature, unit), Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.displayLarge,
                fontSize = if (maxWidth < 320.dp) 92.sp else 108.sp, fontWeight = FontWeight.Light)
        }
        Text(weather.condition.description, style = MaterialTheme.typography.titleLarge)
        Text("Feels like ${UnitConverter.temperature(weather.feelsLike, unit)}", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val today = weather.daily.firstOrNull { it.date == now.atZone(weather.zone).toLocalDate() }
        Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("H  ${UnitConverter.temperature(today?.maximumTemperature, unit)}", style = MaterialTheme.typography.bodyMedium)
            Text("L  ${UnitConverter.temperature(today?.minimumTemperature, unit)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text(if (unit == TemperatureUnit.CELSIUS) "CELSIUS" else "FAHRENHEIT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
