package com.aeris.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aeris.weather.domain.model.*
import com.aeris.weather.ui.theme.Sunlight
import com.aeris.weather.util.*
import java.time.Instant
import kotlin.math.cos
import kotlin.math.sin

@Composable
private fun DetailCard(icon: ImageVector, label: String, value: String, description: String, modifier: Modifier, onClick: () -> Unit) {
    QuietCard(modifier.clickable(onClickLabel = "View $label details", onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, Modifier.padding(top = 18.dp, bottom = 6.dp), style = MaterialTheme.typography.headlineMedium)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WeatherDetailsGrid(weather: WeatherUiModel, unit: TemperatureUnit, now: Instant, onDetails: () -> Unit) {
    val today = weather.daily.firstOrNull { it.date == now.atZone(weather.zone).toLocalDate() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Eyebrow("The details")
        TextButton(onClick = onDetails) { Text("View all"); Icon(Icons.Outlined.NorthEast, null, Modifier.padding(start = 4.dp).size(14.dp)) }
    }
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailCard(Icons.Outlined.WaterDrop, "Humidity", weather.humidity?.let { "$it%" } ?: "—", "Relative humidity", Modifier.weight(1f).fillMaxHeight(), onDetails)
            DetailCard(Icons.Outlined.Air, "Wind", weather.windSpeed?.let { "${it.decimal()} km/h" } ?: "—", "From ${compass(weather.windDirection)}", Modifier.weight(1f).fillMaxHeight(), onDetails)
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailCard(Icons.Outlined.Thermostat, "Feels like", UnitConverter.temperature(weather.feelsLike, unit), "Perceived temperature", Modifier.weight(1f).fillMaxHeight(), onDetails)
            DetailCard(Icons.Outlined.LightMode, "UV index", today?.uvIndex.decimal(), uvLabel(today?.uvIndex), Modifier.weight(1f).fillMaxHeight(), onDetails)
        }
    }
}

@Composable
fun SunProgressCard(weather: WeatherUiModel, now: Instant) {
    val today = weather.daily.firstOrNull { it.date == now.atZone(weather.zone).toLocalDate() }
    val sunrise = today?.sunrise
    val sunset = today?.sunset
    val progress = sunProgress(now, sunrise, sunset)
    SectionHeading("Sunrise & sunset")
    QuietCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Eyebrow("Sunrise")
                Text(sunrise?.formatted("h:mm a", weather.zone) ?: "Unavailable", Modifier.padding(top = 7.dp), style = MaterialTheme.typography.titleLarge)
            }
            Column(horizontalAlignment = Alignment.End) {
                Eyebrow("Sunset")
                Text(sunset?.formatted("h:mm a", weather.zone) ?: "Unavailable", Modifier.padding(top = 7.dp), style = MaterialTheme.typography.titleLarge)
            }
        }
        val track = MaterialTheme.colorScheme.outline
        Canvas(Modifier.fillMaxWidth().height(112.dp).padding(top = 14.dp)) {
            val left = size.width * .04f
            val width = size.width * .92f
            val baseY = size.height * .90f
            val height = size.height * 1.45f
            drawArc(track, 180f, 180f, false, Offset(left, baseY - height / 2), Size(width, height), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))))
            drawLine(track.copy(alpha = .5f), Offset(0f, baseY), Offset(size.width, baseY), 1.dp.toPx())
            if (progress != null) {
                val angle = Math.PI * (1 - progress)
                val position = Offset(left + width / 2 + cos(angle).toFloat() * width / 2, baseY - sin(angle).toFloat() * height / 2)
                drawCircle(Sunlight.copy(alpha = .15f), 14.dp.toPx(), position)
                drawCircle(Sunlight, 5.dp.toPx(), position)
            }
        }
        Text(when {
            sunrise == null || sunset == null -> "Sun times are unavailable for this location."
            progress != null -> "The sun is above the horizon."
            now < sunrise -> "A new day is on the horizon."
            else -> "The sun has set for the day."
        }, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailsSheet(weather: WeatherUiModel, unit: TemperatureUnit, now: Instant, onDismiss: () -> Unit) {
    val today = weather.daily.firstOrNull { it.date == now.atZone(weather.zone).toLocalDate() }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 24.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Eyebrow("${weather.city.name} · conditions")
                Text("A closer look", Modifier.padding(top = 8.dp, bottom = 12.dp), style = MaterialTheme.typography.headlineLarge)
                Text("Observed ${weather.observedAt.formatted("MMM d, h:mm a", weather.zone)} · ${weather.zone.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
            }
            val rows = listOf(
                Triple("Temperature", UnitConverter.temperature(weather.temperature, unit), "Air temperature measured two metres above ground."),
                Triple("Feels like", UnitConverter.temperature(weather.feelsLike, unit), "Perceived temperature based on atmospheric conditions."),
                Triple("Humidity", weather.humidity?.let { "$it%" } ?: "Unavailable", "The amount of moisture in the air relative to saturation."),
                Triple("Wind", weather.windSpeed?.let { "${it.decimal()} km/h" } ?: "Unavailable", "From ${compass(weather.windDirection)}${weather.windDirection?.let { " · $it°" }.orEmpty()}."),
                Triple("Precipitation", weather.precipitation?.let { "${it.decimal()} mm" } ?: "Unavailable", "Precipitation accumulated over the current reporting interval."),
                Triple("UV index", today?.uvIndex.decimal(), "Today's maximum. ${uvLabel(today?.uvIndex)}."),
                Triple("Sunrise", today?.sunrise?.formatted("h:mm a", weather.zone) ?: "Unavailable", "Local time in ${weather.zone.id}."),
                Triple("Sunset", today?.sunset?.formatted("h:mm a", weather.zone) ?: "Unavailable", "Local time in ${weather.zone.id}."),
            )
            items(rows.size) { index ->
                val (label, value, explanation) = rows[index]
                Column(Modifier.padding(vertical = 14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(explanation, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
