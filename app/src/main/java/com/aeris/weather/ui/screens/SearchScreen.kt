package com.aeris.weather.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.aeris.weather.domain.model.City
import com.aeris.weather.ui.components.*
import com.aeris.weather.viewmodel.SearchState

@Composable
fun SearchScreen(state: SearchState, locating: Boolean, currentCity: City, onQuery: (String) -> Unit, onRetry: () -> Unit, onSelect: (City) -> Unit, onLocation: () -> Unit, onBack: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(state.query) }
    LaunchedEffect(Unit) { onQuery(text) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp)) {
        item {
            PageHeading("Find your place", "A forecast for wherever life takes you.", onBack)
            OutlinedTextField(value = text, onValueChange = { text = it; onQuery(it) }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for a city") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (text.isNotEmpty()) IconButton(onClick = { text = ""; onQuery("") }) { Icon(Icons.Outlined.Close, "Clear search") } },
                singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onLocation, enabled = !locating, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                if (locating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Outlined.MyLocation, null, Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp)); Text(if (locating) "Finding your location…" else "Use current location")
            }
            Spacer(Modifier.height(28.dp))
            Eyebrow(if (text.trim().length < 2) "Your selected location" else "Search results")
            Spacer(Modifier.height(12.dp))
        }
        when {
            text.trim().length < 2 -> item {
                CityResult(currentCity, onSelect)
                Text("Enter at least two characters to explore another city.", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.loading || state.query != text -> item {
                Row(Modifier.padding(vertical = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Searching locations…", Modifier.padding(start = 16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
            state.error != null -> item {
                Text(state.error, Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onRetry) { Text("Try again") }
            }
            state.searched && state.results.isEmpty() -> item {
                Text("No locations found.", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.titleLarge)
                Text("Check the spelling or try a nearby city.", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> items(state.results, key = { "${it.id}-${it.latitude}-${it.longitude}" }) { city -> CityResult(city, onSelect) }
        }
    }
}

@Composable
private fun CityResult(city: City, onSelect: (City) -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onSelect(city) }.padding(vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(city.name, style = MaterialTheme.typography.titleLarge)
                Text(city.subtitle, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.NorthEast, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
