# Device verification and screenshots

Run this checklist on an Android 8.0+ emulator or physical device with Google Play services.
The build machine has no connected Android device or installed emulator; do not interpret
unit tests as device verification.

1. Build with `./gradlew testDebugUnitTest lintDebug assembleDebug`.
2. Install using `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
3. Launch using `adb shell am start -n com.aeris.weather/.MainActivity`.
4. Confirm current Open-Meteo weather appears for Amritsar and the observed local time is correct.
5. Pull down or tap refresh. Data must remain visible while refreshing.
6. Scroll through hourly and weekly forecasts. Verify that hourly starts at the selected city's
   current hour and weekly icons match conditions.
7. Search for London, Paris, Tokyo, and a nonsense name. Check real results, city selection,
   no-results feedback, and system Back behavior. Type quickly to exercise cancellation.
8. Open details. Check precipitation, wind direction, UV maximum, and local sun times.
9. Choose Fahrenheit; verify current, feels-like, hourly, daily, and details all convert.
10. Select light, dark, and system appearance. Restart and verify preferences persist.
11. Grant approximate location from Search or Settings; confirm GPS coordinates appear and
    weather refreshes. Search manually afterward.
12. Deny permission, including permanently. Confirm search remains available, no crash occurs,
    and permission prompts do not repeat. Enable permission from Android settings to retry.
13. Disable location services and try GPS. Verify the explanation and manual search fallback.
14. After one successful load, enable airplane mode and refresh. Confirm cached weather and its
    original update time stay visible alongside Retry. Relaunch offline to verify disk caching.
15. Clear app storage, remain offline, and launch. Verify a full error state with Retry and search.
16. Test a nighttime location: the sun arc must have no daytime progress dot.
17. Increase font size, enable TalkBack, rotate the device, and test a small display.
    Check readable controls, scrollability, status bar contrast, and navigation.

Capture actual device screenshots (never substitute mockups):

```sh
adb shell screencap -p /sdcard/aeris.png
adb pull /sdcard/aeris.png docs/screenshots/dashboard-dark.png
```

Repeat after scrolling or navigating, saving `forecast.png`, `search.png`, `settings.png`,
and `dashboard-light.png` under `docs/screenshots/`. These images are pending until a device
is available.
