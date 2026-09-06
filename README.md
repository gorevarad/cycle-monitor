# Cycle Monitor

A native Android cycling computer: Kotlin + Jetpack Compose + Material 3, built around real GPS
data, an honest (clearly-labeled) power estimate, local ride history/statistics, and Flex Mode
transparent overlay exports.

## Module layout

```
core/   Pure Kotlin/JVM module. No Android dependency at all.
        Domain models, GPS math, the power estimation engine, display smoothing,
        the ride recording state machine + RideEngine, statistics/personal-records
        aggregation, and the LocationProvider / PowerEstimator / RideRepository /
        NavigationProvider interfaces. Fully unit tested (`./gradlew :core:test`).

app/    Android application. Compose UI, Room database, DataStore-backed settings,
        the foreground ride-recording service, Google Maps integration, Flex Mode
        PNG export. Depends on :core and implements its interfaces with real
        Android APIs (FusedLocationProviderClient, Room, etc.) plus a debug-only
        MockLocationProvider for development.
```

This split exists so the parts of the app where correctness actually matters most --
distance/grade calculation, GPS-quality classification, the power model, smoothing, statistics,
and ride state transitions -- can be tested on a plain JVM without an emulator or device, and so
production code can never accidentally ship mock sensor data (`BuildConfig.ALLOW_MOCK_LOCATION`
is hardcoded `false` in release builds).

## Setup

1. Open in Android Studio (a current stable release; see versions below).
2. Copy `local.properties.example` to `local.properties`. Android Studio will fill in `sdk.dir`
   automatically; optionally set `MAPS_API_KEY` for the map panel (see below).
3. Build/run the `app` module normally, or run `./gradlew :core:test` for the unit test suite.

### Google Maps

The map panel needs a Maps SDK for Android API key
([get one here](https://developers.google.com/maps/documentation/android-sdk/get-api-key)),
read from `local.properties` (`MAPS_API_KEY=...`, never committed) and wired into the manifest's
`com.google.android.geo.API_KEY` meta-data by `app/build.gradle.kts`. Without a key, the app
still builds and runs fully -- the map panel just shows an explicit "MAP UNAVAILABLE" state
instead of rendering, and ride recording is completely independent of the map either way (see
`RideRecordingService`, which has no map dependency at all).

## IMPORTANT: what has and hasn't been verified

This project was built in a sandboxed environment **with no Android SDK installed and no network
access to Google's Maven repository** (`dl.google.com` / `maven.google.com` are blocked by the
environment's egress policy). That means:

- **`:core` is fully built and verified**: `./gradlew :core:test` actually runs in this
  environment and all 86 tests pass. This covers distance calculation, GPS-quality
  classification, the power estimation model (including edge cases: GPS gaps/jumps, invalid
  coordinates, zero/extreme speed, coasting/descending, missing elevation, weak GPS), display
  smoothing, ride-state transitions, and statistics/personal-records aggregation.
- **`:app` (the Android/Compose code) has NOT been compiled in this environment.** There was no
  way to install the Android SDK or resolve `com.android.application`/AndroidX/Compose/Room/Maps
  dependencies here, so none of the Kotlin in `app/src/main` has been through a real Kotlin/AGP
  compile pass, and the UI has not been run on a device or emulator. It was written carefully,
  reviewed by hand for consistent types/imports/API usage, and follows current (verified via
  official docs/release notes) library APIs -- but treat it as **unverified until you build it**
  in a normal Android Studio environment with SDK/network access.
- The dependency versions in `gradle/libs.versions.toml` (AGP 8.13.0, Kotlin 2.3.21, Compose BOM
  2026.08.00, Room 2.8.0, play-services-location 21.4.0, maps-compose 8.5.0) were checked against
  their official release notes as of this build, not guessed -- but re-check them if time has
  passed. Room's annotation processing uses `kapt` rather than KSP because KSP's own version has
  to be matched to the exact Kotlin release separately and that pairing could not be verified
  here; switching to KSP once you've confirmed the right version is a one-line change (see the
  comment in `app/build.gradle.kts`).

**First thing to do after opening this in a real environment:** build `:app`, fix whatever the
compiler flags (expect this to be mostly nothing-to-minor, since everything was hand-reviewed
against the actual library APIs, but this genuinely has not been proven by a compiler yet), and
run through the golden path on a device: grant location permission, start a ride (mock location
is available via Settings > Developer in debug builds if you don't want to physically ride),
watch the dashboard update, finish the ride, check it in History, open Ride Details, check
Progress/Personal Records, and try a Flex Mode export.

## Architecture notes / deliberate simplifications

- **Dependency injection** is a small hand-rolled `AppContainer` (`app/di/AppContainer.kt`), not
  Hilt/Koin -- one less unverified external dependency, and the graph is small enough that this
  stays readable.
- **RideRecordingService** is a foreground `LifecycleService` and the single source of truth for
  ride state while a ride is active (`RideState` from `:core`, driven through
  `RideStateMachine`). It periodically checkpoints the in-progress ride to Room (every 15s) so a
  crash mid-ride loses at most a few seconds of data, not the whole ride. It has no dependency on
  the map or any UI, so a map failure or UI crash can never stop or corrupt a recording.
- **Power estimation** (`core/power/PowerEstimationEngine.kt`) is a physics model (gravity +
  rolling resistance + aerodynamic drag + acceleration, divided by drivetrain efficiency), not a
  measurement -- the UI always labels it "EST." and downgrades to "LOW CONFIDENCE" or
  "UNAVAILABLE" per documented rules rather than showing a falsely precise number. See the
  class doc for the full list of assumptions.
- **Navigation/routing**: `NavigationProvider` (route calculation) is deliberately separate from
  the map display. Only a `StraightLineNavigationProvider` fallback is implemented -- real
  cycling-aware turn-by-turn routing needs a routing backend (e.g. Google's Routes API with a
  bicycling profile) that requires its own API key/billing and was out of scope to wire up
  blind; the map panel itself is real Google Maps (Maps Compose) once an API key is configured.
- **Dashboard customization** ships four presets (Road/Climb/Race/Casual) selectable in Settings,
  not a full drag-and-drop profile editor -- `DashboardProfile` in `:core` is designed to support
  arbitrary custom profiles later without changes to the ride engine or UI state shape.
- **DashboardProfile / UserCyclingProfile** persistence uses DataStore Preferences rather than
  Room, since they're small, mostly-singleton configuration rather than a growing dataset;
  `Ride`/`TrackPoint` (the data that actually needs to scale and be queried) use Room as
  specified.
- **Flex Mode's PNG export** uses plain `android.graphics` (Bitmap/Canvas/Paint), not a captured
  Compose composable, so the exported file's rendering doesn't depend on newer
  Compose-graphics-layer capture APIs -- the in-app preview is a close Compose approximation of
  the same layout, and the exported PNG is the authoritative pixel output. Transparency is
  real (ARGB_8888 bitmap, PNG encoding preserves alpha).
- **Graphs** (Ride Details) are a small hand-rolled Canvas line chart with tap/drag inspection,
  not an external charting library, to avoid adding an unverified dependency for something this
  simple.

## Testing

`./gradlew :core:test` -- 86 tests, all passing in this environment. Covers:
GPS math (including invalid/null-island coordinates), GPS quality classification, the power
model (steady state, climbing, descending/coasting, acceleration, GPS jitter/jumps, weak GPS,
missing elevation, stale/large time gaps, rider-weight sensitivity), display smoothing (EMA
behavior, windowed averages, gap handling), the ride engine (distance/time accumulation, pause
handling, elevation-gain noise filtering, invalid-coordinate rejection, GPS gaps), the ride state
machine (every legal/illegal transition), and statistics/personal-records aggregation (weighted
averages, missing-data handling, calendar boundaries, excluding invalid rides).

No `:app`-level tests exist yet since they'd need Robolectric/instrumented-test infra that
couldn't be exercised in this environment -- see the "what hasn't been verified" note above.
