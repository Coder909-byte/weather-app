package com.aeris.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aeris.weather.domain.model.*
import com.aeris.weather.ui.theme.Sunlight
import com.aeris.weather.util.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HourlyForecastSection(weather: WeatherUiModel, unit: TemperatureUnit, now: Instant) {
    SectionHeading("Hourly forecast", "Next 24 hours")
    val hours = weather.hourly.filter { it.time >= hourStart(now, weather.zone) }
    if (hours.isEmpty()) Text("Hourly forecast unavailable. Refresh to check again.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(hours, key = { _, hour -> hour.time.epochSecond }) { _, hour ->
            val isNow = hour.time == hourStart(now, weather.zone)
            Column(Modifier.widthIn(min = 66.dp).background(if (isNow) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(if (isNow) "Now" else hour.time.formatted("h a", weather.zone), style = MaterialTheme.typography.bodySmall, color = if (isNow) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                WeatherGlyph(hour.condition.kind, Modifier.size(27.dp).semantics { contentDescription = hour.condition.description }, isDay = hour.isDay)
                Text(UnitConverter.temperature(hour.temperature, unit), style = MaterialTheme.typography.titleMedium)
                Text(hour.precipitationProbability?.let { "$it%" } ?: "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun DailyForecastSection(weather: WeatherUiModel, unit: TemperatureUnit, now: Instant) {
    SectionHeading("7-day forecast")
    QuietCard(Modifier.fillMaxWidth()) {
        if (weather.daily.isEmpty()) Text("Daily forecast unavailable.", style = MaterialTheme.typography.bodyMedium)
        weather.daily.forEachIndexed { i, day ->
            val range = temperatureRange(day, weather.daily)
            val track = MaterialTheme.colorScheme.outlineVariant
            val accent = MaterialTheme.colorScheme.primary
            val label = if (day.date == now.atZone(weather.zone).toLocalDate()) "Today" else day.date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
            Row(Modifier.fillMaxWidth().heightIn(min = 54.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.width(48.dp)) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (label == "Today") FontWeight.SemiBold else FontWeight.Normal)
                    if ((day.precipitationProbability ?: 0) > 0) Text("${day.precipitationProbability}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                WeatherGlyph(day.condition.kind, modifier = Modifier.size(24.dp).semantics { contentDescription = day.condition.description })
                Text(UnitConverter.temperature(day.minimumTemperature, unit), Modifier.widthIn(min = 32.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Canvas(Modifier.weight(1f).height(5.dp)) {
                    drawLine(track, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height, StrokeCap.Round)
                    if (range != null) drawLine(Brush.horizontalGradient(listOf(accent, Sunlight)), Offset(size.width * range.start, size.height / 2), Offset(size.width * range.end, size.height / 2), size.height, StrokeCap.Round)
                }
                Text(UnitConverter.temperature(day.maximumTemperature, unit), Modifier.widthIn(min = 32.dp), style = MaterialTheme.typography.bodyMedium)
            }
            if (i < weather.daily.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
        }
    }
}
