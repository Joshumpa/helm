# CLAUDE.md

## What is Helm?

Helm is a complete car OS for AllWinner A133 head units. It replaces the stock OEM experience
(Dofun) entirely — not just the launcher, but every built-in app. Radio, Bluetooth, navigation,
music, camera, and settings are all Helm's own implementations.

The only external apps Helm launches are user-installed apps (YouTube, WhatsApp, etc.).
No OEM app is ever launched by Helm.

The SDK is the critical abstraction layer. It isolates all hardware communication (MCU, UART,
system services) so feature modules never depend on OEM internals.

---

## Development Tracks

Helm runs on two parallel tracks determined by hardware access:

**Track A — No root (active now)**
Features that work without system app privileges:
music, Bluetooth (Android APIs), navigation, user app launcher, settings UI, self-update mechanism.

**Track B — Requires root (pending FEL mode)**
Features that require binding to `com.tw.uart` (system app only):
FM/AM radio, reverse camera trigger, real MCU data (speed, ADAS), day/night from MCU, CarPlay via ZLINK.

Track B modules must compile and run with stub data sources until root is obtained.
FEL mode is the planned path to root on this unit.

---

## Current Status

**Phase 1 (RE) — Complete. Phase 2 (infrastructure) — Complete. Phase 3 (UX) — In progress.**

### Phase 3 checklist
- ✅ Theme system (`:themes` — 4 variants: Helm/Tesla/AndroidAuto/CarPlay, DataStore, settings UI)
- ✅ Screen transition animations (NowPlaying slides vertical, Settings slides horizontal)
- ✅ Boot screen (wordmark fade-in 700ms, auto-dismiss 1.6s → fade to Home)
- ✅ Speed badge pill (animated color: neutral < 50 km/h, primary 50–89, error ≥ 90)
- ✅ Button press animation (NoBouncy+StiffnessHigh shadow, MediumBouncy scale 0.96f pop-back)
- ✅ Icon launch animation (Animatable 1.0→0.78→pop back in AppGrid and Hotseat)
- ✅ Music player with ExoPlayer + Media3
- ⬜ Stub modules wired up (`:audio`, `:radio`, `:bluetooth`, `:settings`, `:carplay`, `:navigation`)

---

## Roadmap

| Version | Track | Scope |
|---------|-------|-------|
| v1 | A | Daily-driver: music, Bluetooth, navigation, settings, OTA via WiFi (requires root for silent install) |
| v2 | B | Post-FEL MCU integration: radio, reverse camera, real speed data, CarPlay/ZLINK |
| v3 | — | Weather, OBD, voice assistant, automations |
| v4 | — | Portability to other units, plugin store, public SDK |

---

## Build Commands

```bash
./gradlew assembleDebug          # build debug APK
./gradlew :module:test           # run tests for a specific module
./gradlew lint                   # lint all modules
./gradlew detekt                 # static analysis
./gradlew :module:connectedCheck # instrumented tests (requires device)
```

---

## Architecture

```
Android 10 (API 29)
│
├── System services (Bluetooth, GPS, Audio, MCU)
│
└── Helm SDK          ← isolates all hardware/OEM specifics
        │
        └── Helm feature modules
                │
                ├── :core
                ├── :widgets
                ├── :themes
                ├── :audio       ← Helm's own music player (Track A)
                ├── :navigation  ← Helm's own navigation (Track A)
                ├── :bluetooth   ← Helm's own BT manager (Track A)
                ├── :radio       ← FM/AM via MCU (Track B)
                ├── :carplay     ← ZLINK via MCU (Track B)
                ├── :settings
                └── :sdk
```

Each `:module` is an independent Gradle module. The launcher never calls OEM APIs
directly — always routes through the SDK.

---

## Tech Stack

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Clean Architecture per module |
| Build | Gradle multi-module |
| Min SDK | Android 10 (API 29) |
| ABI | armeabi-v7a (firmware is 32-bit despite 64-bit SoC) |

---

## SDK Surface

Track A (available now):
```kotlin
HelmAudio.nowPlaying(): Flow<NowPlayingState>
HelmBluetooth.scan(): Flow<List<BluetoothDevice>>
HelmBluetooth.connect(device: BluetoothDevice): Flow<BtState>
HelmNavigation.startNavigation(destination: LatLng)
```

Track B (post-FEL, via McuService):
```kotlin
McuService.speed(): Flow<Int>            // km/h from MCU
McuService.dayNight(): Flow<DayNight>    // MCU code 0x0204
RadioTuner.tune(frequency: Float)        // FM/AM via MCU UART
ReverseCamera.state(): Flow<CameraState> // triggered by MCU signal
CarPlay.state(): Flow<CarPlayState>      // ZLINK adapter via MCU
```

---

## Rules

- No OEM app is ever launched by Helm — every feature is a Helm implementation.
- User-installed apps (YouTube, WhatsApp, etc.) are opened normally via launcher intent.
- Do not modify Dofun APKs or inject code into OEM apps.
- Every feature lives in its own Gradle module — no monolithic app.
- Route all hardware access through the SDK — never call MCU or OEM services directly from a feature module.
- Track B modules must compile and run on stub data sources before root is available.
- The display is always **portrait** — max 3 columns in grid, use `Column` not `Row` for vertical layouts.
- Always apply `.systemBarsPadding()` on the root `Column` of every screen (`enableEdgeToEdge()` is active).
- ADB command history is logged at `D:\Repos\info apps\adb-commands.md` — check it before suggesting ADB commands.

---

## Reference Docs

Read before touching `:sdk` or any OEM integration:

| File | Contents |
|---|---|
| [`docs/hardware.md`](docs/hardware.md) | SoC specs, firmware versions, native library stack |
| [`docs/oem-platform.md`](docs/oem-platform.md) | Package map, manifest analysis, permissions, entry points, CarMate, reverse camera |
| [`docs/mcu-protocol.md`](docs/mcu-protocol.md) | TWUtil architecture, all message codes, UART frame format, service roles, Helm MCU strategy |
| [`docs/open-questions.md`](docs/open-questions.md) | Unknowns still pending investigation |
