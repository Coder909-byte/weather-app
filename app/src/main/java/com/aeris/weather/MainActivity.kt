package com.aeris.weather

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.*
import com.aeris.weather.ui.screens.*
import com.aeris.weather.ui.theme.*
import com.aeris.weather.viewmodel.*
import kotlinx.coroutines.delay
import java.time.Instant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as AerisApplication
        setContent {
            val weatherVm: WeatherViewModel = viewModel(factory = viewModelFactory { initializer { WeatherViewModel(app.repository, app.preferences, app.location) } })
            val state by weatherVm.state.collectAsStateWithLifecycle()
            val preferences by weatherVm.preferences.collectAsStateWithLifecycle()
            AerisTheme(preferences.appearance) {
                val dark = useDarkTheme(preferences.appearance)
                val background = MaterialTheme.colorScheme.background.toArgb()
                DisposableEffect(dark, background) {
                    enableEdgeToEdge(statusBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                        navigationBarStyle = if (dark) SystemBarStyle.dark(background) else SystemBarStyle.light(background, background))
                    onDispose { }
                }
                val nav = rememberNavController()
                val snackbar = remember { SnackbarHostState() }
                val permissionHistory = remember { getSharedPreferences("location_permission", MODE_PRIVATE) }
                var requestedLocation by remember { mutableStateOf(false) }
                val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) { requestedLocation = true; weatherVm.useCurrentLocation() }
                    else weatherVm.showNotice("Choose a city to see the weather. Location access is optional.")
                }
                val requestLocation: () -> Unit = {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        requestedLocation = true; weatherVm.useCurrentLocation()
                    } else if (permissionHistory.getBoolean("asked", false)) {
                        weatherVm.showNotice("Location access is off. Choose a city, or enable approximate location in Android app settings.")
                    } else {
                        permissionHistory.edit { putBoolean("asked", true) }
                        permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                }
                LaunchedEffect(state.city, state.locating) {
                    if (requestedLocation && state.city.isCurrentLocation && !state.locating) {
                        nav.popBackStack("home", false)
                        requestedLocation = false
                    }
                }
                LaunchedEffect(state.notice) {
                    state.notice?.let { snackbar.showSnackbar(it, duration = SnackbarDuration.Long); weatherVm.clearNotice() }
                }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { weatherVm.refreshIfStale() }
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                val now by produceState(Instant.now(), lifecycleOwner) {
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        while (true) { value = Instant.now(); delay(30_000) }
                    }
                }
                Scaffold(containerColor = MaterialTheme.colorScheme.background, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                        NavHost(navController = nav, startDestination = "home", modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
                            enterTransition = { fadeIn() }, exitTransition = { fadeOut() }) {
                            composable("home") {
                                HomeScreen(state, preferences.unit, now, { nav.navigate("search") { launchSingleTop = true } }, { nav.navigate("settings") { launchSingleTop = true } }, weatherVm::refresh, requestLocation)
                            }
                            composable("search") {
                                val searchVm: SearchViewModel = viewModel(factory = viewModelFactory { initializer { SearchViewModel(app.repository) } })
                                val searchState by searchVm.state.collectAsStateWithLifecycle()
                                SearchScreen(searchState, state.locating, state.city, searchVm::search, searchVm::retry, { city -> weatherVm.selectCity(city); nav.popBackStack("home", false) }, requestLocation, { nav.popBackStack() })
                            }
                            composable("settings") {
                                SettingsScreen(preferences, state.locating, weatherVm::setUnit, weatherVm::setAppearance, requestLocation, { nav.navigate("search") }, { nav.popBackStack() })
                            }
                        }
                    }
                }
            }
        }
    }
}
