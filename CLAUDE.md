# CLAUDE.md

## What is Helm?

Helm is a custom Android launcher platform for car head units, designed to completely
replace the stock OEM experience (Dofun) on AllWinner A133-based screens. It is not
a theme or a mod — it is a full platform with its own launcher, SDK, widget engine,
and theme system.

The SDK is the most critical component. It abstracts all OEM/hardware-specific
communication so the launcher never depends on Dofun internals directly.

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
└── Helm SDK          ← isolates all OEM/hardware specifics
        │
        └── Helm Launcher
                │
                ├── :core
                ├── :widgets
                ├── :themes
                ├── :audio
                ├── :navigation
                ├── :bluetooth
                ├── :carplay
                ├── :radio
                ├── :settings
                └── :sdk
```

Each `:module` is an independent Gradle module. The launcher never calls OEM APIs
directly — it always goes through the SDK. Swapping hardware support means updating
`:sdk` only, never the launcher.

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

## Planned SDK Surface

```kotlin
CarSystem.openRadio()
CarSystem.openBluetooth()
CarSystem.openCarPlay()
CarSystem.openReverseCamera()
CarSystem.openNavigation()
CarSystem.getSystemInfo(): HelmDeviceInfo
```

Internally each method resolves the correct OEM Intent, service call, or MCU command.

---

## Rules

- Do not modify Dofun APKs or inject code into OEM apps.
- Every feature lives in its own Gradle module — no monolithic app.
- The launcher never calls OEM Intents directly — always route through the SDK.
- Design for portability — the SDK layer must not be tied to a single unit.
- Do not purchase or copy OEM themes.
- The display is always **portrait** — max 3 columns in grid, use `Column` not `Row` for vertical layouts.
- Always apply `.systemBarsPadding()` on the root `Column` of every screen (`enableEdgeToEdge()` is active).
- ADB command history is logged at `D:\Repos\info apps\adb-commands.md` — check it before suggesting ADB commands.

---

## Roadmap

| Version | Scope |
|---------|-------|
| v1 | Launcher, widget engine, music |
| v2 | Weather, OBD integration, theme system |
| v3 | Voice assistant, automations, gestures |
| v4 | Plugin store, public SDK, third-party widgets |

---

## Reference Docs

Read before touching `:sdk` or any OEM integration:

| File | Contents |
|---|---|
| [`docs/hardware.md`](docs/hardware.md) | SoC specs, firmware versions, native library stack |
| [`docs/oem-platform.md`](docs/oem-platform.md) | Package map, manifest analysis, permissions, entry points, CarMate, reverse camera |
| [`docs/mcu-protocol.md`](docs/mcu-protocol.md) | TWUtil architecture, all message codes, UART frame format, service roles, Helm MCU strategy |
| [`docs/open-questions.md`](docs/open-questions.md) | Unknowns still pending investigation |
