# Helm

> An open platform for Android car head units.

Helm is not just a launcher — it is a complete software platform designed to replace
the stock OEM experience on Android-based car screens with a modern, modular, and
fully customizable interface built from the ground up.

---

## Why Helm?

Most Android car head units ship with locked-down OEM software that is slow to update,
hard to customize, and impossible to extend. Helm takes a different approach:

instead of patching what the manufacturer gave you, Helm replaces it entirely.

The goal is to build a platform where the launcher, widgets, themes, and system
integrations are all independent modules — so any part can be improved without
breaking the rest.

---

## Architecture

```
Android 14
│
├── System services (Bluetooth, GPS, Audio, MCU)
│
└── Helm SDK          ← hardware abstraction layer
        │
        └── Helm Launcher
                │
                ├── Core
                ├── Widgets
                ├── Themes
                ├── Audio
                ├── Navigation
                ├── Bluetooth
                ├── CarPlay
                ├── Radio
                ├── Settings
                └── SDK
```

The **Helm SDK** is the most important piece. It abstracts all hardware-specific
communication so the launcher never talks directly to OEM services — it only
talks to the SDK. This makes the platform portable across different head units.

---

## SDK (planned)

```kotlin
CarSystem.openRadio()
CarSystem.openBluetooth()
CarSystem.openCarPlay()
CarSystem.openReverseCamera()
CarSystem.openNavigation()
```

Each method internally handles OEM-specific Intents, services, or MCU communication,
keeping the rest of the codebase clean and hardware-agnostic.

---

## Features

### Launcher
- Fully custom home screen
- Replaces OEM launcher as the default home app
- Fast, low-latency UI built for in-vehicle use

### Widget Engine
- Independent, composable widget blocks
- Live data: clock, speed, media, weather
- Configurable grid layout

### Theme System
- External theme format (no recompilation needed)
- Controls: colors, typography, backgrounds, icons, animations
- Install themes at runtime

### Modular Architecture
- Each feature is an independent module
- Update one module without affecting others
- Clean boundaries between UI and system integration

---

## Target Hardware

Helm is being developed on and for the following hardware:

| Component | Details |
|-----------|---------|
| SoC | AllWinner A133 |
| Android | 14 |
| RAM | 4 GB |
| Storage | 64 GB |
| Bluetooth | 5.4 |
| Audio IC | PT2313 |
| Radio IC | QN8035 |
| MCU | T13.1.1 |

The SDK layer is designed to be portable. Future targets may include UIS7862,
Snapdragon-based units, and other Android automotive SoCs.

---

## Roadmap

| Version | Scope |
|---------|-------|
| v1 | Launcher, widget engine, music |
| v2 | Weather, OBD integration, theme system |
| v3 | Voice assistant, automations, gestures |
| v4 | Plugin store, public SDK, third-party widgets |

---

## Project Phases

### Phase 1 — Reverse Engineering & Documentation
Understand the platform completely before writing production code:
- Map all system apps, services, and activities
- Document available Intents and permissions
- Understand MCU communication protocol
- Identify CarPlay and reverse camera entry points

### Phase 2 — Infrastructure
Build the foundation:
- Helm SDK with OEM abstraction
- Launcher skeleton with modular architecture
- CI/CD pipeline

### Phase 3 — User Experience
Build the interface:
- Modern, automotive-grade UI
- Widget system
- Theme engine
- Animations and transitions

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
interfaz moderna, rápida y completamente desarrollada por nosotros, sin depender de
actualizaciones externas.

El proyecto está estructurado en módulos independientes: launcher, widgets, temas,
audio, navegación, CarPlay, radio y un SDK propio que abstrae la comunicación con
el hardware. Esto permite mejorar cualquier parte del sistema sin romper las demás,
y adaptar la plataforma a diferentes unidades Android cambiando únicamente el módulo
de integración de hardware.

El desarrollo comenzó sobre hardware AllWinner A133 con Android 14, pero la
arquitectura está diseñada para ser portable a otros SoCs de pantallas de auto.
