# Yantra

Yantra is an open-source Android application that presents the Hindu lunisolar calendar as a living astronomical instrument rather than a traditional Panchang or calendar application.

The application should feel like a precision observatory instrument: elegant, minimal, and timeless. Opening Yantra should immediately reveal the current astronomical and calendrical state without requiring menus, scrolling, or explanatory text.

## Core Principles

- The primary interface is a single astronomical instrument occupying nearly the entire screen.
- The app has one primary interaction: a date plaque at the bottom of the screen.
- Selecting a date recalculates the instrument and animates it into the corresponding astronomical state.
- All calendar values are derived from celestial mechanics using date, time, latitude, and longitude.
- No static Panchang database or pre-generated calendar data should drive the instrument.

## Instrument Layout

Outer to inner:

1. Samvatsara ring
2. Rashi ring
3. Nakshatra ring
4. Tithi ring
5. Moon phase display

Bottom:

- Date plaque

## Technical Direction

The astronomy and calendar layers should remain independent of Android UI code so future desktop and web versions can reuse the same core engine.

Recommended structure:

```text
astronomical-engine/
calendar-engine/
instrument-renderer/
android-app/
assets/
  sigils/
documentation/
```

## Android Build

Yantra is currently scaffolded as a native Android app using Kotlin, Jetpack Compose, CMake, and vendored Swiss Ephemeris C sources.

Swiss Ephemeris is vendored under `third_party/swisseph` and carries its upstream AGPL licensing terms. Keep this in mind before distributing binaries.

From Windows, open this repository in Android Studio or run:

```powershell
gradle :app:assembleDebug
```

If you prefer the wrapper, generate it from the repository root:

```powershell
gradle wrapper --gradle-version 8.9 --distribution-type bin
.\gradlew :app:assembleDebug
```

The app packages the current-era Swiss Ephemeris files:

- `app/src/main/assets/ephe/sepl_18.se1`
- `app/src/main/assets/ephe/semo_18.se1`

At startup, Yantra copies those assets into internal app storage and calls Swiss Ephemeris through JNI. The instrument state is computed from the device's current local time.

## Visual Direction

Yantra should feel like a scientific instrument inspired by astrolabes, observatories, precision chronometers, and temple astronomical instruments.

Materials and lighting:

- Aged brass
- Bronze
- Silver moonlight
- Ivory daylight

Avoid:

- Dashboard aesthetics
- Modern flat design
- Neon glows
- Excessive animation
