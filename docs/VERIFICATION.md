# Verification report

Verified on October 30, 2026 (Asia/Kolkata).

## Build and automated checks

The native Android project compiled successfully with Gradle 8.13, AGP 8.13.2,
Kotlin/Compose compiler 2.2.21, SDK Platform 36, and Build Tools 35.0.0.
The host has JDK 21 and 24; verification used JDK 21 with Java/Kotlin bytecode targeting 17.
Android Studio and an emulator were not installed. Official Android command-line tools were
installed under `/private/tmp/aeris-android-sdk` for this verification.

The actual command used the downloaded official Gradle 8.13 distribution directly:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home \
GRADLE_USER_HOME=/private/tmp/aeris-gradle-home \
/private/tmp/aeris-tools/gradle-8.13/bin/gradle \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug \
  --no-daemon --console=plain -Pkotlin.compiler.execution.strategy=in-process
```

Result: **BUILD SUCCESSFUL**. The final invocation reused the passing unit-test result
because test inputs were unchanged. The preceding invocation executed all 20 tests successfully.
The project's Gradle wrapper pins the same Gradle version and its official SHA-256 checksum.

On a normally configured development machine, run:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

| Suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| WeatherLogicTest | 13 | 0 | 0 | 0 |
| WeatherRepositoryTest | 7 | 0 | 0 | 0 |
| Total | 20 | 0 | 0 | 0 |

Coverage includes all documented weather codes, unit conversion, fractional time-zone offsets,
local dates, daylight-saving transitions, uneven/null API arrays, empty responses, geocoding
validation, shared temperature ranges, sun progress at night, cache freshness, request query
parameters, cache persistence, offline failures, disk-write failures, cancellation, and debounced
searches that cannot publish superseded results. Tests use explicit fixtures, a fake cache,
coroutine virtual time, and real Retrofit clients against MockWebServer.

Android lint: **0 errors, 18 warnings**. Seventeen warnings suggest newer dependency/Gradle versions;
one is a backup-configuration advisory. The manifest explicitly disables backup, sets
`fullBackupContent=false`, and supplies Android 12+ extraction rules excluding app data from
cloud backup and device transfer. The dependency set is deliberately pinned and has passed
compilation and tests. Warnings have not been suppressed.

Evidence: [build output](build-and-test.log), [test counts](test-results.json),
[lint report](lint-results.txt), [APK signature verification](apk-signature.txt).
Full HTML test and lint reports are also under `app/build/reports/`.

## Live service checks

`python3 scripts/check_live_api.py` passed against the real Open-Meteo endpoints for
Amritsar, London, and Tokyo, plus an empty-results search. Each city returned current weather,
168 hourly rows, seven daily rows, and its expected location time zone. Tokyo's local date
correctly differed from India's during this check. This verifies the public service contract;
it is separate from running the Android application on a device.

See [captured live results](live-api-check.json) for actual observations and timestamps.
These recorded readings are not used as fallback weather inside the app.

## APK

- Path: `app/build/outputs/apk/debug/app-debug.apk`
- Package: `com.aeris.weather`
- Version: `1.0.0` (code 1)
- Minimum API: 26; target/compile API: 36
- Size: 21,202,085 bytes
- Signing: verified successfully with APK Signature Scheme v2
- SHA-256: `77c3ef7ecd6462cd48cc6f5eea7e755dd7dd79ccb9ba0659ad70069f4d1cc46b`

The APK includes the bundled Manrope font and its SIL license. Manifest inspection confirmed
internet and approximate location permissions; there is no fine or background location permission.

## Remaining device verification

`adb devices` returned an empty device list. No physical device or emulator was available,
so app launch, visual inspection, GPS permission dialogs, device-level offline persistence,
TalkBack/font scaling, navigation, and screenshots have **not** been manually verified.
An APK build and JVM tests do not establish those runtime acceptance criteria.

Follow [the device checklist](DEVICE-TESTING.md) to install the APK, exercise these flows,
and capture the required screenshots. No generated mockups are presented as device screenshots.
Optional saved-city collections and the hourly line chart are not included.
