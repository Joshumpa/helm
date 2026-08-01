# Contributing to Helm

## Prerequisites

- JDK 17
- Android SDK (API 29+)
- Android Studio or IntelliJ IDEA (optional but recommended)

## Development Setup

1. Clone the repo
2. Open in Android Studio / IntelliJ IDEA
3. Let Gradle sync — no extra configuration needed

## Development Tracks

Know which track you're working on before starting:

**Track A — No root required (active)**
Music, Bluetooth, navigation, settings, OTA. These run on the emulator and the real device.

**Track B — Root required (pending FEL mode)**
FM/AM radio, reverse camera, real MCU data (speed, ADAS), CarPlay via ZLINK. Requires binding to `com.tw.uart`, a system app on the head unit. Track B modules must compile and run with stub data until root is available — the stub is the deliverable until FEL mode is unlocked.

## Emulator vs Real Device

**Track A works on an emulator.** Debug builds include all ABIs (no `abiFilters` restriction), so a standard x86_64 API 29 emulator runs Helm normally. Use it for UI work, music player, settings, and OTA.

**Track B requires the real device.** The MCU service (`com.tw.uart`) only exists on AllWinner A133 head units running the stock firmware. No emulator can replicate it.

**Release builds target armeabi-v7a only** to match the 32-bit firmware on the A133 head unit.

## Build Commands

```bash
./gradlew assembleDebug          # build debug APK
./gradlew :module:test           # unit tests for a module
./gradlew detekt                 # static analysis — must pass before opening a PR
./gradlew lint                   # lint all modules
./gradlew :module:connectedCheck # instrumented tests (requires device)
```

## Module Boundaries

Every feature lives in its own Gradle module. The critical rule:

> All hardware access routes through `:sdk`. Feature modules never call OEM or MCU APIs directly.

If you're adding a new feature, add a new module — don't add it to `:core` or the launcher app.

## Code Style

Detekt enforces style automatically. Run `./gradlew detekt` before opening a PR — CI will reject if it fails.

- Kotlin only
- No comments explaining *what* the code does — use self-documenting names
- One short comment only when the *why* is non-obvious (a hidden constraint, a workaround, a subtle invariant)
- MVVM per module: ViewModel + Repository + DataSource

## Architecture Rules

- No OEM app is ever launched by Helm
- Track B modules must compile and produce a working stub UI without root
- Route all hardware access through `:sdk` — never call MCU or OEM services from feature modules
- Portrait-only layout, max 3 columns, always `.systemBarsPadding()` on the root Column (`enableEdgeToEdge()` is active)
