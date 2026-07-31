## Why

Track A delivers daily-driver features, but the head unit's most distinctive hardware — FM/AM radio, reverse camera, real vehicle speed, day/night mode, and CarPlay via ZLINK — is locked behind the MCU (`android.tw.john.TWUtil`), which requires `android.uid.system` (root). With FEL mode now the planned path to root, it is time to design and spec Track B so implementation can begin the moment root is available, and so stub code today matches exactly what the real code will need.

**Track B depends on root via FEL mode.** None of these features can be live-tested without system app privileges on the unit.

## What Changes

- **`:sdk` MCU layer** — real `TWUtil` binding replaces all stubs: `callbackFlow` wrapping `twUtil.open()` / `handleMessage()`, sealed `DataError` propagation, and `twUtil.write()` for outbound commands.
- **`McuService`** — implements `speed(): Flow<Int>` (code TBD — privileged path), `dayNight(): Flow<DayNight>` (code `0x0204`), power-off countdown (`0x9F10`), heartbeat watchdog (`0x0202`), and time sync (`0x0518` / `0x0107`).
- **`:radio`** — replaces stub with `RadioTuner` backed by `QN8035` via MCU UART (`tuner_8035.so`); FM/AM band switching, frequency seek, RDS name.
- **`:carplay`** — replaces stub with CarPlay session lifecycle driven by MCU code `0x0205` (session active/ended) and audio source `0x0301`.
- **Reverse camera** — MCU codes `0x020B` (gear), `0x9F1C` (stream live), `0x0304` (ack), `0x0506` (resolution) drive a full-screen `SurfaceView`; parking radar overlay from `0x0507`.
- **Day/night theme switch** — `0x0204` feeds `HelmTheme` day/night toggle without user interaction.
- **Volume/audio source routing** — MCU codes `0x0106`, `0x0203`, `0x0301` keep Helm's audio state in sync with steering-wheel controls and source switches.

## Capabilities

### New Capabilities

- `mcu-service`: Core MCU binding layer in `:sdk` — TWUtil lifecycle, subscription management, outbound write API, sealed error propagation, and stub-vs-real switching.
- `radio-tuner`: FM/AM radio feature in `:radio` — frequency control via MCU, band switching, RDS, seek.
- `reverse-camera`: Reverse gear detection, camera stream display, parking radar overlay.
- `carplay-session`: CarPlay/ZLINK session lifecycle driven by MCU events.
- `mcu-vehicle-data`: Vehicle data flows — speed, day/night, door state, ambient temperature, AC/climate.
- `mcu-audio-routing`: Steering-wheel volume and audio source synchronisation with MCU.

### Modified Capabilities

<!-- No existing specs yet — all capabilities are new. -->

## Impact

- **`:sdk`** — major additions to MCU abstraction; new `McuService`, `RadioTuner`, `ReverseCamera`, `CarPlay` SDK surface classes.
- **`:radio`**, **`:carplay`** — stubs replaced with real implementations.
- **New `:camera` module** (or feature inside `:sdk`) — `SurfaceView`-based reverse camera display.
- **`:themes`** — day/night auto-switch consumer added.
- **`:settings`** — no requirement changes; MCU firmware version surfaced in about screen (implementation detail only).
- **Dependencies** — `android.tw.john.TWUtil` (platform SDK, `@hide`, requires system UID); `tuner_8035.so` (native, already in `/system_tw/lib/`); `libimobiledevice.so` stack for CarPlay USB.
- **Prerequisite** — root via FEL mode is required before any Track B feature can run on hardware.
