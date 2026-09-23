# MyHotspot — Phase 1: Project & Architecture

## What this phase delivers

A compiling Android project skeleton with:

- Kotlin + Jetpack Compose, Material 3
- Clean Architecture package layout (`ui`, `domain`, `data`, `platform`, `service`, `utils`)
- Hilt for DI (so platform wrappers around `WifiManager` / `ConnectivityManager`
  can be swapped for fakes in unit tests later)
- `minSdk 26` / `compileSdk & targetSdk 35`
- A single placeholder Compose screen confirming the app launches

No networking, permission, or capability-detection code yet — that's Phase 2
onward, on purpose, so each phase has a known-good baseline.

## Package layout

```
app/src/main/java/com/myhotspot/app/
├── MyHotspotApplication.kt      # @HiltAndroidApp entry point
├── MainActivity.kt              # Compose host (placeholder screen for now)
├── ui/
│   ├── home/                    # Phase 8
│   ├── hotspot/                 # Phase 4/5
│   ├── clients/                 # Phase 6
│   ├── network/                 # Phase 3
│   ├── diagnostics/             # Phase 7
│   └── theme/                   # Color.kt / Type.kt / Theme.kt (populated now)
├── domain/
│   ├── models/                  # Pure Kotlin data classes — no Android imports
│   └── usecases/                # Interfaces implemented by data/
├── data/
│   ├── network/                 # Phase 3, 6
│   ├── wifi/                    # Phase 2, 4
│   └── connectivity/            # Phase 2, 3
├── platform/                    # Thin wrappers over WifiManager, ConnectivityManager,
│                                 # and (where publicly available) tethering APIs.
│                                 # Isolating these here means version-specific
│                                 # branching (`Build.VERSION.SDK_INT >= ...`) lives
│                                 # in exactly one place per API.
├── service/                     # Phase 4 (HotspotService), Phase 6 (NetworkMonitorService)
└── utils/
```

### Why `domain` has no Android imports

`domain/models` and `domain/usecases` are plain Kotlin. That means the
capability-detection logic, validation rules, and hotspot state machine
(Phase 2, 4, 15) can be unit-tested on the JVM in milliseconds, without an
emulator. `platform/` is where the actual `android.net.wifi.*` /
`android.net.*` calls happen, behind interfaces `data/` implements.

## Build

Open in Android Studio (Koala/2024.1+ recommended for compileSdk 35 support)
and let it sync, or from the command line:

```bash
./gradlew assembleDebug
```

## Run on a physical device

A physical device is required from Phase 4 onward — hotspot/Wi-Fi behavior
is unreliable-to-nonexistent on the emulator. For this phase, an emulator
is fine since we're just confirming the scaffold compiles and launches:

```bash
./gradlew installDebug
```

You should see "MyHotspot" with the Phase 1 placeholder text.

## Next: Phase 2

Device & network capability detection — Android version, Wi-Fi/Wi-Fi Direct/
LocalOnlyHotspot support, tethering availability, root detection — surfaced
as a `CapabilityReport` domain model with a first pass at the Diagnostics
data source.
