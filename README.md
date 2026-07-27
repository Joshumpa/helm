<div align="center">

![](.github/assets/helm-banner.svg)

[![CI](https://github.com/Joshumpa/helm/actions/workflows/ci.yml/badge.svg)](https://github.com/Joshumpa/helm/actions/workflows/ci.yml)
![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)
![Android API](https://img.shields.io/badge/Android-10%2B-3DDC84.svg?style=flat-square)
![Language](https://img.shields.io/badge/Kotlin-100%25-7F52FF.svg?style=flat-square)
![Status](https://img.shields.io/badge/status-Phase%202-orange.svg?style=flat-square)

</div>

---

**Helm is a complete software platform that replaces the stock OEM experience on Android car head units.** Not a theme. Not a launcher mod. A full replacement — built from scratch, with its own SDK, widget engine, and theme system.

The hardware shipping inside most aftermarket Android screens is actually capable. The software holding it back is not. Helm fixes that.

---

## Architecture

```
Android 10 (API 29)
│
├── System services (Bluetooth, GPS, Audio, MCU/UART)
│
└── Helm SDK                  ← hardware abstraction layer
        │
        └── Helm Launcher
                │
                ├── :core       launcher shell, app grid, HOME toggle
                ├── :widgets    clock, speed, media, weather, ADAS
                ├── :themes     colors, typography, backgrounds, animations
                ├── :audio      EQ, DSP, source routing
                ├── :navigation map integration, speed overlay
                ├── :bluetooth  A2DP media, AVRCP controls
                ├── :carplay    ZLINK / CarPlay session management
                ├── :radio      FM/AM tuner, MediaSession bridge
                ├── :settings   in-app configuration UI
                └── :sdk        OEM abstraction, MCU data pipeline
```

The SDK is the critical piece. Every hardware interaction — MCU events, OEM Intents, serial frames — goes through it. The launcher never calls OEM APIs directly. Swapping support for a different head unit means updating `:sdk` only; everything else stays untouched.

---

## SDK

**High-level surface** — what the launcher calls:

```kotlin
CarSystem.openRadio()
CarSystem.openBluetooth()
CarSystem.openCarPlay()
CarSystem.openReverseCamera()
CarSystem.openNavigation()
CarSystem.getSystemInfo(): HelmDeviceInfo
```

**MCU data pipeline** — typed vehicle events from the UART bus:

```kotlin
interface McuDataSource {
    fun start()
    fun stop()
    fun writeFrame(cmdType: Byte, payload: ByteArray): Boolean
    fun registerFrameHandler(cmdType: Byte, handler: (ByteArray) -> Unit)
    fun addListener(listener: McuEventListener)
}

interface McuEventListener {
    fun onCarData(data: CarData)           // speed, RPM, gear, temperature
    fun onAcData(data: AcData)             // climate control state
    fun onBrakeBelt(data: BrakeBeltData)   // handbrake, belt, turn signals
    fun onDoor(data: DoorData)
    fun onSteeringButton(event: SteeringButtonEvent)
    fun onSourceSwitch(toOem: Boolean)
    fun onParkingRadar(active: Boolean)
}
```

**Public AIDL service** — what third-party apps and widgets bind to:

```kotlin
interface IHelmSdkService {
    fun sendMcuCommand(cmdType: Int, data: ByteArray)
    fun registerMcuListener(listener: IMcuListener)
    fun unregisterMcuListener(listener: IMcuListener)
    fun remapSteeringButton(button: SteeringButtonEvent, action: EventAction)
    fun getConfig(): String
    fun setConfig(json: String)
}
```

---

## Features

### Launcher
- Replaces the OEM launcher as the default home app — dynamically toggleable
- Horizontal app grid: 2 rows × 4 columns always visible, scrollable
- Pinned shortcuts with drag-to-reorder
- Debounced touch input designed for gloves and road vibration

### Widget Engine
- Configurable drag-and-drop grid
- Widgets: clock, vehicle speed, now playing, weather, ADAS alerts, compass, A/C status
- Per-sensor reactive streams with independent polling rates
- Live album art, playback controls, Bluetooth metadata

### Theme System
- Day / Night / Auto (solar calculation) / System modes
- Accent, background, and font colors — fully user configurable
- Frosted-glass card aesthetic that works on any wallpaper
- UI scale override without affecting font sizes

### MCU Integration
- Vehicle speed, RPM, gear, handbrake, seat belt, turn signals
- A/C state: temperature zones, fan speed, airflow mode
- Steering wheel media buttons, voice button, phone controls
- Parking radar distances (front and rear, 8 sensors)
- ADAS events: lane departure, forward collision, stop/go

### Radio
- FM/AM tuner with hardware MCU backend (QN8035 IC)
- MediaSession fallback — works without root on any vendor unit
- Per-band presets with long-press to save

---

## How Helm Talks to the Car

Most Android car head units share a common pattern: a microcontroller (MCU) manages
all the physical inputs — steering wheel buttons, gear sensor, A/C controls, door
contacts, parking radar — and communicates with Android over a serial UART bus.

On AllWinner A133 firmware, the full chain looks like this:

```
MCU (hardware)
  ──serial──▶ libtwutil2.so  (native JNI, PID 1941)
                  │  android.tw.john.TWUtil
       ┌──────────┴─────────────────────┐
       ▼                                ▼
  com.tw.service                   privileged apps
  (system events:                  (vehicle data: speed,
  volume, night mode,              AC, ADAS — via TWUtil
  source switch)                   directly)
```

`android.tw.john.TWUtil` is a hidden platform class provided by AllWinner. It exposes
30 typed event codes — reverse gear, door state, parking radar, ambient temperature,
climate control, steering buttons, screen rotation, power-off countdown, and more.

The UART frame format is simple:

```
0xF2  dataType  cmdType  payloadLen  [payload...]  checksum
```

Helm subscribes to these codes directly (with system UID) and maps each one to a
typed Kotlin event in `McuEventListener`. No polling. No OEM middleware.

---

## Target Hardware

Helm is developed on the following reference unit:

| Component  | Details |
|------------|---------|
| SoC        | AllWinner A133 |
| Android    | 10 (API 29) |
| CPU mode   | 32-bit (armeabi-v7a) |
| RAM        | 4 GB |
| Storage    | 64 GB |
| Bluetooth  | 5.4 |
| Audio IC   | PT2313 |
| Radio IC   | QN8035 |
| MCU        | T13.1.1 |
| SELinux    | Permissive |

The architecture is designed to be portable. A different SoC or OEM means updating `:sdk` only.

---

## Tech Stack

| Layer        | Technology |
|--------------|-----------|
| Language     | Kotlin |
| UI           | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture per module |
| Build        | Gradle multi-module |
| Persistence  | DataStore + Room |
| Async        | Coroutines + StateFlow |
| DI           | Hilt |
| Min SDK      | Android 10 (API 29) |
| ABI          | armeabi-v7a (release) |

---

## Roadmap

| Version | Scope | Status |
|---------|-------|--------|
| v1 | Launcher, widget engine, music | In progress |
| v2 | Weather, OBD integration, theme system | Planned |
| v3 | Voice assistant, automations, gestures | Planned |
| v4 | Plugin store, public SDK, third-party widgets | Planned |

### Development Phases

**Phase 1 — Reverse Engineering** ✅  
Platform fully mapped: system APKs, AIDL interfaces, MCU protocol (30 event codes
decoded), CarPlay entry points, all required permissions.

**Phase 2 — Infrastructure** 🚧  
Gradle multi-module scaffold, Helm SDK interfaces, CI/CD pipeline.

**Phase 3 — User Experience** ⬜  
Automotive-grade Compose UI, widget engine, theme system, animations.

---

## Philosophy

> Instead of asking "how do we modify what the manufacturer gave us?"  
> we ask "how do we make the manufacturer irrelevant?"

---

## License

MIT — see [LICENSE](LICENSE).
