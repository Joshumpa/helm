# Helm

> An open platform for Android car head units.

Helm is not a launcher mod or a theme — it is a complete software platform designed
to replace the stock OEM experience on Android-based car screens with a modern,
modular, and fully customizable system built from the ground up.

---

## Why Helm?

Most Android car head units ship with locked-down OEM software that is slow, hard to
customize, and impossible to extend. Helm takes a different approach: instead of
patching what the manufacturer gave you, Helm replaces it entirely.

The platform is built around one principle — the launcher never talks directly to OEM
services. Every hardware interaction goes through the Helm SDK, which keeps the rest
of the system clean and portable across different head units.

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

Each module has a defined contract: it exposes an interface to `:app` and declares
the interfaces it needs from other modules. No module imports concrete classes from
another — dependencies flow through the SDK or are injected by `:app`.

---

## SDK

The SDK abstracts three layers of hardware communication:

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

**Public AIDL service** — what third-party apps and widgets can bind to:

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
- Replaces OEM launcher as the default home app (dynamically toggleable)
- Horizontal app grid — 2 rows, 4 columns always visible, scrollable
- Pinned app shortcuts with drag-to-reorder
- Debounced touch input — designed for gloves and road vibration

### Widget Engine
- Configurable grid layout with drag-and-drop repositioning
- Widgets: clock, vehicle speed, now playing, weather, ADAS alerts, compass, A/C status
- Per-sensor reactive data streams — independent polling rates per data source
- Live album art, playback controls, Bluetooth metadata

### Theme System
- Day / Night / Auto (solar calculation) / System modes
- Accent, background, and font colors — user configurable
- Frosted-glass card aesthetic — works on any wallpaper
- UI scale override without affecting font sizes

### MCU Integration
- Vehicle speed, RPM, gear, handbrake, seat belt, turn signals
- A/C state: temperature zones, fan speed, airflow mode
- Steering wheel media buttons, voice button, phone controls
- Parking radar distances (front and rear)
- ADAS events: lane departure, forward collision, stop/go

### Radio
- FM/AM tuner with hardware MCU backend
- MediaSession fallback — works on any vendor unit without root
- Frequency parsing from media metadata
- Per-band presets with long-press to save

---

## Target Hardware

Helm is developed on the following reference hardware:

| Component | Details |
|-----------|---------|
| SoC | AllWinner A133 |
| Android | 10 (API 29) |
| CPU mode | 32-bit (armeabi-v7a) |
| RAM | 4 GB |
| Storage | 64 GB |
| Bluetooth | 5.4 |
| Audio IC | PT2313 |
| Radio IC | QN8035 |
| MCU | T13.1.1 |

The SDK layer is designed to be portable. Swapping support for a different head unit
means updating `:sdk` only — the rest of the codebase is hardware-agnostic.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture per module |
| Build | Gradle multi-module |
| Persistence | DataStore (preferences) + Room (app list) |
| Async | Coroutines + StateFlow |
| DI | Hilt |
| Min SDK | Android 10 (API 29) |
| ABI | armeabi-v7a |

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
Platform fully mapped: system APKs, AIDL interfaces, MCU communication protocol,
CarPlay entry points, permissions required.

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

---

## Español

**Helm** es una plataforma de software de código abierto para pantallas Android de
automóvil. El objetivo es reemplazar la experiencia del fabricante (OEM) por una
interfaz moderna, modular y completamente construida desde cero.

El proyecto está organizado en módulos independientes: `:core`, `:widgets`, `:themes`,
`:audio`, `:navigation`, `:bluetooth`, `:carplay`, `:radio`, `:settings` y `:sdk`.
El SDK abstrae toda la comunicación con el hardware — la pantalla MCU, el UART de
serie, las señales del vehículo y los servicios OEM — de modo que el launcher nunca
depende de implementaciones específicas del fabricante.

El hardware de referencia es AllWinner A133 con Android 10 (API 29, armeabi-v7a),
pero la arquitectura está diseñada para ser portable a otros SoCs de pantallas de auto
cambiando únicamente el módulo `:sdk`.
