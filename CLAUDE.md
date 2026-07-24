# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## What is Helm?

Helm is a custom Android launcher platform for car head units, designed to completely
replace the stock OEM experience (Dofun) on AllWinner A133-based screens. It is not
a theme or a mod — it is a full platform with its own launcher, SDK, widget engine,
and theme system.

The SDK is the most critical component. It abstracts all OEM/hardware-specific
communication so the launcher never depends on Dofun internals directly.

---

## Current Status

**Phase 1 — Reverse Engineering & Documentation. No production code exists yet.**

Do not write production code until Phase 1 is complete. Building on assumptions about
OEM behavior will cause failures that are hard to debug on-device.

### Phase 1 — Reverse Engineering & Documentation
Understand the platform completely before writing any production code:
- Map all system apps, services, and activities on the unit
- Document all available Intents and required permissions
- Understand the MCU communication protocol (serial, socket, or proprietary service)
- Identify CarPlay and reverse camera entry points
- Determine ADB accessibility and root feasibility

**Phase 1 is done when** the following artifacts exist:
- Intent map for `com.dofun.variety` (all exported activities, services, receivers)
- MCU protocol documentation (message format, known commands)
- Permission list Dofun holds that Helm will need
- Answer to each open question listed at the bottom of this file

### Phase 2 — Infrastructure
Build the foundation:
- Helm SDK with OEM abstraction layer
- Launcher skeleton with multi-module Gradle structure
- CI/CD pipeline

### Phase 3 — User Experience
Build the interface:
- Automotive-grade UI with Jetpack Compose
- Widget engine and theme system
- Animations and transitions

---

## Build Commands

The project has not been scaffolded yet (Phase 1). Once Phase 2 begins, expect standard
Android/Gradle commands. This section will be updated then.

```bash
# These will apply once the Gradle project exists:
./gradlew assembleDebug          # build debug APK
./gradlew :module:test           # run tests for a specific module
./gradlew lint                   # lint all modules
./gradlew :module:connectedCheck # instrumented tests (requires device)
```

---

## Dev Tools (Phase 1)

ADB is available without additional installation:
```
C:\Users\Josh\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

jadx (decompiler) is installed at:
```
D:\Repos\helm\dofun-analysis\jadx\bin\jadx.bat
```

jadx requires the Android Studio JBR — set before running:
```powershell
$env:JAVA_HOME = "D:\AndroidStudio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

---

## Hardware & Firmware (reference unit)

| Field | Value |
|-------|-------|
| SoC | AllWinner A133 |
| Android | 14 |
| RAM | 4 GB |
| Storage | 64 GB |
| Kernel | 4.9.170 |
| Build | QP1A.191105.004 test-keys |
| Audio IC | PT2313 |
| Radio IC | QN8035 |
| Bluetooth | 5.4 |
| MCU version | T13.1.1-100-10-A6939D-241125(6) |
| Boot version | JBDxGFH_M8_WD097SHM30AA-D6_90_240522 |
| System version | V13.1.1_20260408.141817_OS-EN |
| App/Theme version | \13_20260408.140850_APP-THEME1 |

The `test-keys` build tag suggests root may be achievable via standard test-key exploits.
This matters because direct MCU communication likely requires system-level access.

The USB-A ports on this unit are **host-mode only** (confirmed green-colored ports),
so USB ADB is not possible from the standard ports. Use wireless ADB.

Developer Options unlock code: **7890** (confirmed working on this unit).
Factory settings unlock code: **8888**.
Wireless Debugging option is **not present** in Developer Options on this unit — must
enable ADB via terminal sideload instead.

---

## OEM Details (Dofun)

| Field | Value |
|-------|-------|
| Package | `com.dofun.variety` |
| Main activity | `com.dofun.variety.online.DomesticOnlineActivity` |
| APK version | V7.10.28.168.240313 |
| APK protection | 360 Jiagu (code encrypted — jadx will not decompile main dex) |
| Plugin framework | RePlugin by Qihoo 360 |
| Internal URI scheme | `launcher://variety/theme/category/one` |

All reverse engineering starts from this package. Useful ADB starting points:

```bash
# Dump the full package manifest
adb shell dumpsys package com.dofun.variety

# List all exported components
adb shell cmd package dump com.dofun.variety

# Watch Dofun's logcat output live (PowerShell syntax)
$pid = & adb shell pidof com.dofun.variety; adb logcat --pid=$pid

# Dump all running services
adb shell dumpsys activity services

# List all broadcast receivers registered at runtime
adb shell dumpsys activity broadcasts

# Try root immediately on this test-keys build
adb shell su
adb root
```

Local analysis artifacts in `dofun-analysis/`:
- `dofun.apk` — downloaded from PGYER (v168)
- `apps_config.json`, `apps_match_config.json`, `link_icon_config.json` — extracted assets
- `manifest_raw.txt` — decoded AndroidManifest via aapt2
- `kp.jar` — extracted default theme plugin
- `kp-assets/` — key JSON configs from kp.jar (ADAS, speed, media, weather, hotseat)
- `kp-src/` — kp.jar decompiled via jadx (RePlugin framework only, no business logic)
- `dofun-src/` — main APK decompiled via jadx (Jiagu stub only, no business logic)
- `jadx/` — jadx 1.5.1 binary

---

## Phase 1 Findings

### System Package Map

Packages identified from static APK analysis. Confirm presence with
`adb shell pm list packages` on the actual unit.

| Function | Package(s) |
|---|---|
| Radio | `com.tw.radio`, `android.car.app.radio`, `com.syu.radio` |
| Bluetooth | `com.tw.bt`, `com.autochips.bluetooth`, `com.aotochips.bluetooth` |
| Music (local) | `com.tw.music`, `android.car.app.media`, `com.syu.music` |
| Video | `com.tw.video`, `android.car.app.mp4`, `com.syu.video` |
| Navigation | `android.car.app.gps`, `com.syu.onekeynavi` |
| CarPlay / Mirror | `com.tima.carnet.vt` (Tima), `com.zjinnova.zlink` (ZLINK), `net.easyconn` (Yilian) |
| MCU bridge | `com.dofun.carassistant.car` (CarMate) |
| Car settings | `com.dofun.carsetting.ui.MainActivity` |
| AUX / ext. input | `com.tw.auxin.AuxInActivity`, `com.autochips.android.backcar` |
| 360 camera | `cn.cardoor.zt360`, `com.autochips.android.backcar` |
| Dashcam / DVR | `com.dofun.recorder`, `com.tw.recorder`, `com.softwinner.dvr` |
| Voice assistant | `com.dofun.aios.voice`, `com.dofun.overseasvoice`, `com.dofun.bridge` |
| EQ / DSP | `com.tw.eq.EQActivity`, `com.tw.eq.DSPActivity` |
| Steering wheel | `com.tw.keypad`, `com.android.settings.KeypadActivity` |
| Tire pressure | `com.dofun.tpms` |
| Weather | `com.dofun.dofunweather.main` |
| APK installer | `com.dofun.carsetting.activity.apkinstall.InstallActivity` |
| Air conditioning | `com.tw.ac.AirConditionActivity` |
| Boot animation | `com.tw.bootanimation.BootAnimationActivity` |

### AndroidManifest Analysis (static, from downloaded APK v168)

#### Permissions Dofun declares (Helm will need most of these)

`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`,
`ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `INTERNET`,
`READ_PHONE_STATE`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`,
`FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED`, `BROADCAST_STICKY`,
`READ_LOGS`, `SYSTEM_ALERT_WINDOW`, `EXPAND_STATUS_BAR`, `BLUETOOTH`,
`CAMERA`, `REQUEST_INSTALL_PACKAGES`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`,
`MEDIA_CONTENT_CONTROL` *(signature-level — may require system app),*
`com.tencent.wecarflow.PLAY_CONTROL` *(Tencent CarPlay/CarLife integration)*

#### Custom permissions Dofun defines

| Permission | Purpose |
|---|---|
| `cn.cardoor.variety.permission.ENTRANCE_ICON_FILTER` | Gates which apps appear in the launcher icon grid |
| `com.dofun.variety.permission.provider.exported.WRITE_KV_CONFIG` | Required to write to the ExportedProvider content provider |

#### Exported activities (callable from outside)

| Activity | How to reach |
|---|---|
| `com.dofun.overseasvariety.Launcher` | Home screen — launched on boot via `MAIN`+`HOME` |
| `com.dofun.variety.online.DomesticOnlineActivity` | `MAIN` action; also `launcher://variety/theme/category/one`, `.../two`, `.../online/theme` |
| `com.dofun.variety.theme.details.ThemeDetailsActivity` | `launcher://variety/theme/category/details` |
| `com.dofun.overseasvariety.activity.ScreenSaverActivity` | `cn.cardoor.intent.action.DAY_DREAM` or direct `am start` |
| `com.dofun.variety.router.EnterSettingActivity` | `launcher://variety/setting` (autoVerify=true) |

Test with ADB once connected:
```bash
# Open Dofun settings
adb shell am start -a android.intent.action.VIEW -d "launcher://variety/setting"

# Trigger screensaver
adb shell am start -n com.dofun.variety/.overseasvariety.activity.ScreenSaverActivity

# Send DAY_DREAM broadcast
adb shell am broadcast -a cn.cardoor.intent.action.DAY_DREAM
```

#### Services of interest

| Service | Action | Notes |
|---|---|---|
| `cn.cardoor.basic.media.NotifyService` | `android.service.notification.NotificationListenerService` | Reads all notifications — how Dofun shows music metadata on the home screen widget. Helm `:widgets` needs its own NotificationListenerService. |
| `cn.cardoor.desktop.window.DesktopWindowService` | `cn.cardoor.desktop.window.DESKTOP_WINDOW_SERVICE` | Floating multi-window overlay service. Test: `adb shell am startservice -a cn.cardoor.desktop.window.DESKTOP_WINDOW_SERVICE` |

#### Broadcast receivers

Only **one** exported receiver: `com.dofun.variety.VarietyReceiver`
Listens to: `BOOT_COMPLETED`, `LOCALE_CHANGED`.

**No MCU or reverse-camera receiver is exposed in this APK.** MCU signals come
from a separate system APK or HAL layer — not from `com.dofun.variety`.

#### Content providers

| Authority | Exported | Notes |
|---|---|---|
| `com.dofun.variety.ExportedProvider` | **yes** | The only external hook. Reads are open; writes require `WRITE_KV_CONFIG` permission. |

Query from ADB:
```bash
adb shell content query --uri content://com.dofun.variety.ExportedProvider
```

### MCU → Android Data Channel (confirmed)

The launcher theme plugin (`kp.jar`) receives live vehicle data from the MCU.
The delivery mechanism (broadcast, service, or provider) is confirmed to exist but
the source APK is not `com.dofun.variety` — it is a separate system app.

**Speed:** delivered continuously, thresholds at 5, 10, 20, 30, 50, 70, 90 km/h.
Displayed as large numeric text (`tv_carspeed_text`, 60sp) + unit (`tv_carspeed_unit`, 22sp).

**ADAS events** received by the launcher:
- `car_effect_adas_warning_stop_go` — stop/go warning
- `car_effect_adas_warning_car` — close-distance warning
- `car_effect_adas_warning_lane` — lane departure
- `car_effect_adas_warning_lane_left` — lane departure left
- `car_effect_adas_warning_lane_right` — lane departure right

**Media widget** displays: song name, 170×170 round album art, progress bar,
prev/play/next controls. Supports: local music, FM radio, Bluetooth, Spotify,
Apple Music, YouTube Music, Kugou, Kuwo, Ximalaya.

**Weather widget** shows animated effects for: fine, cloudy, rain, thunderstorm,
snow, haze.

### APK Code Analysis

The main `classes.dex` is a 360 Jiagu loader stub (`com.stub.StubApp`) — the real
application code is encrypted inside `libjiagu.so` and decrypted at runtime. jadx
produces no useful business logic from the main dex. String extraction from the dex
yielded no passwords or keys.

The Developer Options password for this unit's system Settings APK is embedded in
the firmware (`/system/priv-app/Settings/Settings.apk`), which requires ADB to pull.
This is a circular dependency — ADB is needed to read the password, but the password
is needed to enable wireless ADB.

### ADB Access Attempts

| Method | Result |
|---|---|
| USB via green USB-A port | Failed — port is host-mode only |
| Bugjaeger (Android OTG) | Failed — same host-mode issue |
| `adb connect 192.168.1.79:5555` | Refused (port closed) |
| Ports 5037, 4444, 7777, 8888 | All timed out |

Unit IP confirmed: **192.168.1.79** (DHCP — may change on reboot; verify in
Settings → Wi-Fi → tap connected network before each session)

**Current unblocking plan:** Sideload a terminal emulator APK via USB drive, then
run `setprop service.adb.tcp.port 5555 && stop adbd && start adbd` from the terminal.
APK to use: **Terminal Emulator for Android** (`jackpal.androidterm`) — download from
APKPure or APKMirror, copy to USB drive, install from the unit's file manager or
APK installer (`com.dofun.carsetting.activity.apkinstall.InstallActivity`).

### What is still unknown
- Root via `adb root` or `adb shell su` (test-keys build makes this likely — try immediately after connecting)
- Which system APK delivers MCU data (speed, ADAS) to the launcher
- How the reverse camera signal is delivered to Android
- How CarPlay is launched — which of the three mirror apps and via what Intent
- What the `ExportedProvider` KV keys contain
- What plugin APKs are loaded by RePlugin at runtime (need root to read `/data/data/com.dofun.variety/`)

---

## Architecture

```
Android 14
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
directly — it always goes through the SDK. This keeps the platform portable: swapping
in support for a new head unit means updating `:sdk` only.

---

## Tech Stack

- Language: **Kotlin**
- UI: **Jetpack Compose**
- Architecture: **MVVM + Clean Architecture per module**
- Build: **Gradle (multi-module)**
- Min SDK: Android 14 (API 34)

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

Internally, each method resolves the correct OEM Intent, service call, or MCU command
for the current hardware target. The launcher calls only these — never raw Intents.

---

## Roadmap

| Version | Scope |
|---------|-------|
| v1 | Launcher, widget engine, music |
| v2 | Weather, OBD integration, theme system |
| v3 | Voice assistant, automations, gestures |
| v4 | Plugin store, public SDK, third-party widgets |

---

## What NOT to do

- Do not modify Dofun APKs or inject code into OEM apps.
- Do not build a monolithic app — every feature lives in its own Gradle module.
- Do not let the launcher call OEM Intents directly — always route through the SDK.
- Do not design for a single unit — the SDK layer must remain portable.
- Do not purchase or copy OEM themes.

---

## Key Open Questions (Phase 1)

1. ~~Is ADB accessible wirelessly?~~ **Partially answered** — unit IP is 192.168.1.79, port 5555 is closed. Need to enable via terminal sideload.
2. Can we obtain root? Try `adb root` and `adb shell su` immediately after first ADB connection.
3. What Intents does `com.dofun.variety` expose at runtime? (manifest analyzed statically — confirm with `dumpsys package com.dofun.variety` on device)
4. How does the MCU bridge deliver data to the launcher? (broadcast, service, or provider?)
5. How is the reverse camera signal delivered to Android?
6. How does Dofun launch CarPlay — which of the three mirror apps does it use?
7. What system permissions does Dofun hold that Helm will need?
8. What does the `ExportedProvider` KV store contain? (`adb shell content query --uri content://com.dofun.variety.ExportedProvider`)
