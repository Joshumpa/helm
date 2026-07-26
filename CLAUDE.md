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

**Phase 1 — Complete. Phase 2 — In progress.**

Phase 1 RE is done. The Gradle multi-module scaffold exists. Development can proceed
on all UI/launch features. MCU data integration requires root (system app install).

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

### Phase 2 — Infrastructure (in progress)
Build the foundation:
- ✅ Helm SDK with OEM abstraction layer (`sdk/` module — CarSystem, AdasEvent, McuDataSource interface)
- ✅ Launcher skeleton with multi-module Gradle structure (11 modules, minSdk 29, armeabi-v7a)
- ⬜ CI/CD pipeline

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
| Android | 10 (API 29) — Dofun UI reporta "14" incorrectamente |
| CPU mode | 32-bit (armeabi-v7a) — A133 es 64-bit físico pero firmware es 32-bit |
| RAM | 4 GB |
| Storage | 64 GB |
| Kernel | 4.9.170 |
| Build | QP1A.191105.004 test-keys |
| SELinux | permissive — no bloquea operaciones no autorizadas |
| Security patch | 2021-01-05 |
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
- `apks/` — APKs pulled from device via TermOne Plus `cp /system_tw/priv-app/... /sdcard/`
  - `com.tw.uart.apk` — MCU UART service (713 KB)
  - `CarMate.apk` — MCU bridge app layer (7.3 MB)
  - `com.tw.reverse_3f4d.apk` — reverse camera app (10.4 MB)
- `uart-src/` — com.tw.uart decompiled — **fully readable, critical IPC findings**
- `carmate-src/` — CarMate decompiled — partial (18 errors), business logic readable
- `reverse-src/` — com.tw.reverse decompiled — partial, manifest + activities readable

---

## Phase 1 Findings

### System Package Map

Packages confirmed on-device via `pm list packages -f` from TermOne Plus (no root).
All OEM packages live in the `/system_tw/` partition, separate from `/system/`.

| Function | Package (confirmed) |
|---|---|
| MCU UART (low-level) | `com.tw.uart` ← **primary MCU channel** (system UID, AIDL) |
| MCU intermediary (?) | `com.tw.carinfoservice` ← **bind action found in CarMate source — NOT in original package map, may not be installed** |
| MCU bridge (app layer) | `com.dofun.carassistant.car` (CarMate) ← binds to carinfoservice, not uart directly |
| Radio | `com.tw.radio` |
| Bluetooth | `com.tw.bt` |
| Music (local) | `com.tw.music` |
| Video | `com.tw.video` |
| EQ / DSP | `com.tw.eq` |
| AUX input | `com.tw.auxin` |
| Reverse camera | `com.tw.reverse` |
| Right/lateral camera | `com.tw.rightview` |
| 360 camera | `cn.cardoor.zt360` |
| Dashcam / DVR | `com.tw.dvr` |
| CarPlay / Mirror | `com.zjinnova.zlink` (ZLINK — only one confirmed installed) |
| Steering wheel | `com.tw.keypad` |
| Car settings | `com.dofun.carsetting` |
| Weather | `com.dofun.dofunweather.main` |
| Launcher | `com.dofun.variety` |
| Dofun market | `com.dofun.market` |
| Boot animation | `com.tw.bootanimation` |
| Core service | `com.tw.coreservice` |
| Core base | `com.tw.core` |
| Extended service | `com.tw.service.xt` |
| Main service | `com.tw.service` |
| Network | `com.tw.net` |
| File explorer | `com.tw.twfileexplore` |
| Car model selector | `com.tw.carchoose` |
| Unknown hardware | `com.ms.ms2160` (MS912X.apk) — function TBD |

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

### MCU → Android Data Channel (fully documented)

The launcher theme plugin (`kp.jar`) receives live vehicle data from the MCU.
The source APK is **`com.tw.uart`** — confirmed present in `/system_tw/priv-app/`.
It handles raw UART/serial communication with the MCU using the `android-serialport-api`
library (JNI, native `libserial_port.so`). Data flows upward to
`com.dofun.carassistant.car` (CarMate) via AIDL callback.

**IPC mechanism: AIDL Binder** — confirmed via jadx decompilation of `com.tw.uart.apk`.

AIDL interface name: `com.tw.uart.IUartController`
Bind action: `com.tw.uart.UartService.Bind`

| Transaction | Method | Purpose |
|-------------|--------|---------|
| 1 | `registerCallback(IUartReceiver cb)` | Subscribe to raw MCU bytes |
| 2 | `unregisterCallback(IUartReceiver cb)` | Unsubscribe |
| 3 | `openUart(int baudRate, String dev_ttyS)` | Open serial port — path passed by caller, NOT hardcoded |
| 4 | `closeUart()` | Close serial port |
| 5 | `writeUartData(byte[] data)` | Send raw command bytes to MCU |
| 6 | `updateDataTime(Bundle b)` | Set polling interval (default 50 ms, min 10 ms) |

**Correction from initial docs:** The callback interface is `IUartReceiver` (not `IUartCallback`),
method is `onUartDataUpdate(byte[] bytes)` (not `onUartData`). Confirmed from `uart-src/resources/com/tw/uart/IUartReceiver.aidl`.

Callback delivers: `void onUartDataUpdate(byte[] bytes)` — raw bytes, format unknown (needs logcat).

Log tags from `com.tw.uart` (confirmed from source):
- `"UartService"` — service lifecycle, openUart events (prints the actual dev_ttyS path)
- `"SerialPort"` — native serial port open/error events

When ADB is available, this command reveals the serial device path:
```bash
adb logcat -s UartService:D   # look for: "dev_ttyS:/dev/ttyXXX"
```

**Critical constraint: `android:sharedUserId="android.uid.system"`**
`com.tw.uart` and `com.tw.reverse` both run as the Android system user.
Only apps with the same UID can bind to `UartService`. Helm must be installed as a
system app (requires root) to receive MCU data directly. This is the architectural
reason CarMate exists as the bridge — it is also a system app.

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

### Reverse Camera — Entry Points (confirmed via jadx)

APK: `com.tw.reverse` — also `android:sharedUserId="android.uid.system"`.

| Entry point | How to reach | Notes |
|-------------|-------------|-------|
| `com.tw.reverse.StreamMediaActivity` | `getLaunchIntentForPackage("com.tw.reverse")` | Main app, LAUNCHER category |
| `cn.cardoor.desktop.window.floating.intent.action.LAUNCH` | `am start -a` or `sendBroadcast` | Floating window — may work from non-system app |
| `com.tw.reverse.ReverseActivity` | Not externally accessible | No action in IntentFilter |
| `com.tw.reverse.RadarService` | Exported service | Parking radar display |

The same floating window action pattern (`cn.cardoor.desktop.window.*`) appears in
the `com.tw.reverse` manifest, the `DesktopWindowService` in Dofun, and likely other
OEM apps — it is the platform-level multi-window overlay mechanism.

### CarMate Broadcasts (confirmed via jadx)

CarMate (`com.dofun.carassistant.car`) emits these broadcasts:
- `com.dofun.intent.action.TRACK_SWITCH` — media track change notification
- `com.dofun.location.DESTROY_LOCATIONSERVICE` — location service lifecycle

The MCU data distribution from CarMate to the launcher is likely via a second AIDL
interface or via direct IPC — not found in the broadcast search. Needs logcat or
further decompilation of the obfuscated CarMate classes (heavily renamed, a/b/c/...).

### ADB Access Attempts

| Method | Result |
|---|---|
| USB via green USB-A port | Failed — port is host-mode only |
| Bugjaeger (Android OTG) | Failed — same host-mode issue |
| `adb connect 192.168.1.79:5555` | Refused (port closed) |
| Ports 5037, 4444, 7777, 8888 | All timed out |
| LADB (Play Store) | Paid app — skipped |
| TermOne Plus (Play Store) | **Installed and working** — shell access without root |
| `setprop service.adb.tcp.port 5555` | Ran without error, value confirmed via getprop |
| `stop adbd` / `start adbd` | Failed — requires root |
| `kill $(pidof adbd)` | Failed — adbd invisible to non-root ps |
| `settings put global adb_wifi_enabled 1` | Failed — requires INTERACT_ACROSS_USERS |

Unit IP confirmed: **192.168.1.79** (DHCP — may change on reboot; verify in
Settings → Wi-Fi → tap connected network before each session)

**Current status:** Terminal shell available via TermOne Plus. ADB port cannot be
activated without root. `su` binary not present. No root path available from userspace.

**Next unblocking option:** FEL mode (AllWinner bootloader) — requires physical access
to the PCB to locate the FEL pad/button. Allows flashing a Magisk-patched boot image
from PC via PhoenixSuit without needing ADB.

### What is still unknown
- Root access — no `su` binary, ADB not enabled. FEL mode is next option.
- Raw MCU byte format — what bytes mean speed, ADAS events, etc. (needs logcat with root)
- How CarMate distributes parsed MCU data to the launcher (second AIDL? broadcast?)
- What the `ExportedProvider` KV keys contain (needs `adb shell content query`)
- What plugin APKs RePlugin loads at runtime (needs root to read `/data/data/com.dofun.variety/`)
- Function of `com.ms.ms2160` (MS912X.apk) — unknown hardware module
- `com.tw.rightview` entry points — APK not yet decompiled

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
directly — it always goes through the SDK. This keeps the platform portable: swapping
in support for a new head unit means updating `:sdk` only.

---

## Tech Stack

- Language: **Kotlin**
- UI: **Jetpack Compose**
- Architecture: **MVVM + Clean Architecture per module**
- Build: **Gradle (multi-module)**
- Min SDK: Android 10 (API 29) — confirmado por getprop y build number QP1A (Android Q)

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

1. ~~Is ADB accessible wirelessly?~~ **Answered** — port 5555 bloqueado. Terminal disponible vía TermOne Plus pero sin root no se puede activar ADB. Próximo paso: FEL mode.
2. ~~Which system APK delivers MCU data?~~ **Partially answered** — `com.tw.uart` es el canal UART de bajo nivel. CarMate (`com.dofun.carassistant.car`) no se conecta directamente a `com.tw.uart` — se conecta a **`com.tw.carinfoservice`** (acción `com.tw.carinfoservice.CarService.Bind`), un paquete intermediario no mapeado aún. Ver pregunta 12.
3. ~~How is the reverse camera signal delivered?~~ **Answered** — `com.tw.reverse.StreamMediaActivity` vía `getLaunchIntentForPackage`, o floating window action `cn.cardoor.desktop.window.floating.intent.action.LAUNCH`.
4. ~~Which CarPlay app does Dofun use?~~ **Answered** — `com.zjinnova.zlink` (ZLINK) es el único confirmado instalado.
5. **Can we obtain root?** — `su` no existe, ADB no disponible. FEL mode es el siguiente camino viable.
6. ~~How does `com.tw.uart` deliver data?~~ **Answered** — AIDL Binder. Interface `com.tw.uart.IUartController`, bind action `com.tw.uart.UartService.Bind`. Ver tabla completa arriba.
7. ~~What Intents does `com.tw.reverse` respond to?~~ **Answered** — ver tabla de entry points arriba. `com.tw.rightview` pendiente.
8. **What does the `ExportedProvider` KV store contain?** — requiere ADB: `adb shell content query --uri content://com.dofun.variety.ExportedProvider`
9. **What is `com.ms.ms2160` (MS912X.apk)?** — paquete desconocido en `/system_tw/`. Función sin identificar.
10. **What Intents does `com.dofun.variety` expose at runtime?** — manifiesto analizado estáticamente. Confirmar con `dumpsys package com.dofun.variety` una vez con root.
11. **Raw MCU byte format** — `writeUartData` y el callback `onUartDataUpdate` usan `byte[]` crudo. El protocolo (qué bytes significan velocidad, ADAS, etc.) es desconocido. Necesita logcat con root para capturar tráfico real.
12. **¿Qué es `com.tw.carinfoservice`?** — CarMate se conecta a este paquete (acción `com.tw.carinfoservice.CarService.Bind`), no directamente a `com.tw.uart`. No aparece en el mapa de paquetes inicial. Verificar: `pm list packages | grep carinfo` en TermOne Plus. Podría ser el verdadero intermediario entre `com.tw.uart` y las apps de usuario.
13. **¿Cómo llegan velocidad y ADAS al launcher de Dofun?** — CarMate usa GPS (`location.getSpeed() * 3.6f`) para velocidad en su propia UI. El canal hacia `com.dofun.variety` para datos MCU es desconocido — no es broadcasts, no es `Settings.Global`. Posiblemente vía `ExportedProvider` o AIDL directo desde `com.tw.carinfoservice`.
14. **Serial device path** — `openUart(baudRate, dev_ttyS)` recibe la ruta como parámetro, no está hardcodeada. El log de `UartService` la imprime: buscar `"dev_ttyS:"` en logcat con tag `UartService`. Ruta probable: `/dev/ttyS0`–`/dev/ttyS7` (AllWinner A133).
