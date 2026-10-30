# AERIS

**Weather, Refined.** A native Android weather app built with Kotlin and Jetpack Compose.
Original atmospheric Canvas artwork, quiet slate surfaces, bundled Manrope typography, and
live Open-Meteo forecasts form a focused, scrollable weather experience.

## Features

- Current weather, feels-like temperature, humidity, wind speed/direction, and precipitation.
- Up to 24 upcoming hourly forecasts with precipitation probability and day/night icons.
- Seven-day forecast with condition-specific icons and temperature bars on a shared scale.
- Sunrise/sunset arc with a daylight-only progress indicator; daily UV maximum.
- Worldwide city search with a 350 ms debounce, a two-character minimum, cancellation of
  superseded requests, and loading, empty, error, and retry states.
- Optional approximate GPS location with timeout, permission denial handling, and manual search.
- Celsius/Fahrenheit conversion throughout the app; system, light, and dark appearance.
- Persistent DataStore preferences and selected location; atomic disk cache for the latest
  successful forecast. A 15-minute freshness threshold triggers refresh on startup or resume.
- Pull-to-refresh and an explicit refresh button; failed refreshes preserve visible weather.
- Reusable details sheet and accessible labels for interactive icons.
- No API keys, sign-in, backend, background tracking, or fabricated fallback weather.

## Stack and architecture

| Component | Technology |
| --- | --- |
| Android | API 26 minimum; compile/target API 36 |
| UI | Jetpack Compose BOM 2025.10.01, Material 3, Navigation Compose |
| Language/build | Kotlin 2.2.21 + matching Compose compiler plugin; AGP 8.13.2; Gradle 8.13 |
| Bytecode | Java/Kotlin JVM target 17 |
| Networking | Retrofit 2.11.0, OkHttp 4.12.0, Gson |
| State | ViewModel, StateFlow, lifecycle-aware collection, coroutines |
| Persistence | Preferences DataStore; AtomicFile JSON weather cache |
| Location | Fused Location Provider, approximate permission only |
| Tests | JUnit 4, coroutine virtual time, OkHttp MockWebServer |

```text
Compose screens → ViewModels → WeatherRepository → Retrofit → Open-Meteo
                        ↓              ↓
                 DataStore        AtomicFile cache
```

`AerisApplication` constructs dependencies manually. API DTOs remain separate from domain/UI
models. Networking never runs inside Composables. The repository validates and maps payloads;
ViewModels handle cancellation, loading, refresh, failure, and persistence. Celsius is the
single source unit, converted locally at every display boundary.

```text
app/src/main/java/com/aeris/weather/
  data/remote/        Retrofit interfaces, clients, nullable response DTOs
  data/local/         Preferences and atomic forecast cache
  data/repository/    Live forecasts, city search, cache restore
  data/location/      Foreground, cancellable approximate location request
  domain/model/       City, conditions, forecasts, preferences
  domain/mapper/      DTO validation and forecast transformation
  viewmodel/          Weather and debounced search state
  ui/components/      Hero, original artwork, forecasts, details, sun arc
  ui/screens/         Dashboard, search, settings
  ui/theme/           Palette and bundled variable Manrope font
  util/               WMO mapping, units, time, ranges, daylight calculations
```

## Open in Android Studio

1. Open this directory in an Android Studio version supporting AGP 8.13.
2. Install Android SDK Platform 36 and the SDK Build Tools when prompted.
3. Use JDK 17 or 21 for Gradle (the generated Java/Kotlin bytecode targets 17).
4. Set the SDK path in Android Studio, or create an untracked `local.properties` containing
   `sdk.dir=/absolute/path/to/Android/sdk`. The development machine's temporary SDK path is
   not portable; replace it on another computer.
5. Allow Gradle sync to download dependencies, then run `app` on an API 26+ device or emulator.
   Google Play services are needed for the optional GPS feature; city search works without GPS.

## Build and test

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows: `gradlew.bat testDebugUnitTest lintDebug assembleDebug`.

Output: `app/build/outputs/apk/debug/app-debug.apk`.

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.aeris.weather/.MainActivity
```

The public API contract can also be checked independently:

```sh
python3 scripts/check_live_api.py
```

See [verification results](docs/VERIFICATION.md) for commands actually executed, and
[the device checklist](docs/DEVICE-TESTING.md) for remaining device validation and screenshot capture.

## Data, timestamps, and permissions

Forecasts use [Open-Meteo](https://open-meteo.com/en/docs) and city search uses the
[Open-Meteo Geocoding API](https://open-meteo.com/en/docs/geocoding-api).
Requests use Retrofit query parameters with `timezone=auto`, seven forecast days, explicit
Celsius/km/h units, and `timeformat=unixtime`. Epoch timestamps are interpreted in the
response's IANA time zone so device time zones and daylight-saving transitions do not
distort the forecast. Hourly `is_day` controls day/night symbols. Missing values render as
unavailable, and unknown weather codes receive a neutral symbol.

Internet permission retrieves forecasts and search results. Approximate location permission
is requested only after tapping a current-location action, never automatically or repeatedly
after denial. The fallback starting city is Amritsar, with live API weather. GPS mode displays
real coordinates as “Current location”; it does not invent a reverse-geocoded city name.
Users can always choose a city manually. The app sends selected coordinates to Open-Meteo,
stores the selected location and last forecast in private app storage, and does no background
location tracking. Android backup is disabled for these files.

The original fetch timestamp remains visible for cached forecasts, including offline launches.
Only the most recently successful city's forecast is cached. Choosing a different city while
offline shows an error rather than displaying the previous city's data under a new name.

## Artwork, attribution, and limitations

Weather artwork is original Compose Canvas drawing. Interface icons use Material Icons.
Manrope is bundled under the [SIL Open Font License](docs/Manrope-OFL.txt).
Weather data is attributed to Open-Meteo inside the dashboard and settings under
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).

Saved-city collections and an hourly line chart are optional PRD enhancements and are not
included. GPS is foreground-only and requires Google Play services. There is no background
refresh or weather alert infrastructure. Real device screenshots and manual device checks
remain pending until a device/emulator is available; see the verification report for precise
build and test status.
# weather-app
