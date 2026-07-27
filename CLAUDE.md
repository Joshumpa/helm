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

### Phase 2 — Infrastructure (complete)
Build the foundation:
- ✅ Helm SDK with OEM abstraction layer (`sdk/` module — CarSystem, AdasEvent, McuDataSource interface)
- ✅ Launcher skeleton with multi-module Gradle structure (11 modules, minSdk 29, armeabi-v7a)
- ✅ CI/CD pipeline (GitHub Actions — lint + assembleDebug + Detekt + JUnit 5)

### Phase 3 — User Experience (in progress)
Build the interface:
- ✅ Theme system (`:themes` module — 4 variants: Helm/Tesla/AndroidAuto/CarPlay, DataStore persistence, settings UI)
- ✅ Screen transition animations (directional slide: NowPlaying slides vertical, Settings slides horizontal)
- ✅ Boot screen (splash con wordmark animado, fade-in 700ms, auto-dismiss 1.6s → fade a Home)
- ✅ Speed badge pill (color animado: neutral < 50 km/h, primary 50–89, error ≥ 90)
- ✅ Button press animation — NoBouncy+StiffnessHigh shadow, MediumBouncy scale 0.96f pop-back
- ✅ Icon launch animation (Animatable 1.0→0.78→pop back en AppGrid y Hotseat, solo al ícono)
- ⬜ Stub modules wired up (`:audio`, `:radio`, `:bluetooth`, `:settings`, `:carplay`, `:navigation`)

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

---

## Phase 1 Findings

### System Package Map

Packages confirmed on-device via `pm list packages -f` from TermOne Plus (no root).
All OEM packages live in the `/system_tw/` partition, separate from `/system/`.

| Function | Package (confirmed) |
|---|---|
| MCU UART (low-level) | `com.tw.uart` ← **primary MCU channel** (system UID, AIDL) |
| MCU intermediary | `com.tw.carinfoservice` ← **confirmed NOT installed** (logcat boot error) — CarMate references it but it doesn't exist on this firmware |
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
| Steering wheel | `com.tw.keypad` — main activity: `com.tw.keypad.SteeringWheelActivity` (APK not extracted) |
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

**Serial ports on device:** `/dev/ttyS0`, `/dev/ttyS1` (=Bluetooth), `/dev/ttyS2`,
`/dev/ttyS3`, `/dev/ttyS4`, `/dev/ttyS7`. MCU port is one of ttyS0/2/3/4/7.
Confirmed via `tty_devices.txt` and `props_mcu.txt`.

**True MCU architecture (confirmed from logcat.bin and twservice-src decompilation):**

```
MCU (hardware) ──serial──▶ libtwutil2.so (native, PID 1941, tag "twutil2")
                                    │  JNI / android.tw.john.TWUtil
                         ┌──────────┴──────────────────────┐
                         ▼                                  ▼
              com.tw.service (TWService)          privileged apps (Dofun
              system events only:                 launcher plugin, etc.)
              volume, night/day, source           vehicle data: speed, AC,
              switch, screen rotation             ADAS (via TWUtil directly)
                         │
                  sendBroadcast (limited)
                  pm hide/unhide packages
```

**`android.tw.john.TWUtil`** — the real MCU subscription API.
A hidden system class provided by AllWinner in the platform SDK. Any process with
`android:sharedUserId="android.uid.system"` can use it. Helm requires root to access.

Usage pattern (from `twservice-src/com/p018tw/service/C0282Pa.java`):
```java
// Full subscription list — all 30 codes TWService listens to
twUtil.open(new short[]{
    258, 260, 516, 517, 528, 262, 263, 266, 274,      // 0x102–0x112
    513, 514, 515, 523, 524,                            // 0x201–0x20C
    769, 770, 772, 1028,                                // 0x301–0x404
    1281, 1285, 1286, 1287, 1288, 1289, 1304,           // 0x501–0x518
    -25088, -25071, -24816, -24805, -24804              // 0x9E00–0x9F1C (signed short)
});
twUtil.start();  // starts background listener thread
// Receive events in overridden handleMessage(Message msg)
// Send commands: twUtil.write(int code, int arg1, int arg2, byte[] data)
```

**TWUtil message codes** — full subscription list decoded (from `HandlerC0341ka.java` + logcat):

Direction key: MCU→A = MCU to Android (inbound); A→MCU = Android to MCU (outbound only); ↔ = bidirectional.

| Code (hex) | Dec | Dir | Description |
|---|---|---|---|
| `0x0102` | 258 | MCU→A | Config / ACK |
| `0x0104` | 260 | MCU→A | Climate data byte array (temperature zones) |
| `0x0106` | 262 | MCU→A | Volume levels — obj[0]=media vol → `setStreamVolume(stream5, vol)`; obj[1]=secondary level |
| `0x0107` | 263 | ↔ | Time sync — Android pushes time+date+12/24h to MCU RTC on `TIME_TICK`/`TIME_SET`; subscribed for echo/ACK |
| `0x010A` | 266 | MCU→A | MCU firmware version string (ASCII, len=30) |
| `0x0112` | 274 | MCU→A | Status word — bit 15=extra flag; queries `Settings.System["ISSHOW_FAN"]`; bit field encodes AC/fan/source state |
| `0x0201` | 513 | MCU→A | Source/mode selection — arg2: 1=src1 2=src2 3=src3 4=src4 38=AUX; also fired as **33281** (0x8201, bit15 set) |
| `0x0202` | 514 | MCU→A | Heartbeat (every ~3 sec, arg1=state) |
| `0x0203` | 515 | MCU→A | Volume change (steering wheel button) — `arg1 & 0x7FFFFFFF`=current level; `arg1 & 0x80000000`≠0=muted; `arg2 & 0x7FFFFFFF`=max level → triggers volume OSD overlay |
| `0x0204` | 516 | MCU→A | Day/Night mode (arg1: 1=night, 0=day) |
| `0x0205` | 517 | MCU→A | CarPlay/Mirror session active — arg1≠0=active, arg1=0=ended; stored in `f793Vc`; blocks YouTube PiP |
| `0x020B` | 523 | MCU→A | Reverse gear engaged — arg1=1 in reverse, arg1=0 not; stored in `C0282Pa.f611Rh` |
| `0x020C` | 524 | MCU→A | Lateral camera trigger — arg1=1 launch rightView, arg1=2 launch leftView, arg1=0 dismiss |
| `0x0210` | 528 | MCU→A | Screen rotation (arg1: 1=90°, 2=180°, 3=270°) → `IWindowManager.freezeRotation()` |
| `0x0301` | 769 | ↔ | Audio source — MCU→A on change; A→MCU `write(769, 192, srcId)`; IDs: 1=BT 2=AUX 3=USB-music 4=USB-video 5=CarPlay 7=radio 192=idle |
| `0x0302` | 770 | MCU→A | BT phone call — arg1=0 ended (unmute+restore source); arg1≠0 active (mute+switch to call audio) |
| `0x0304` | 772 | A→MCU | Reverse camera active ack — `write(772,1)`=camera showing, `write(772,0)`=closed; MCU stores in `f642Fd` |
| `0x0404` | 1028 | MCU→A | Touch zone bitmask — when bits 0x22 set, shows touch-area calibration overlay (`ta_layout`) |
| `0x0501` | 1281 | ↔ | Touch injection — MCU injects touch via `InteractionController.touchDown()`; A→MCU `write(1281, x, 2, byte[]{x,y})` |
| `0x0505` | 1285 | MCU→A | Door state — obj[0]&0xF8: bit6=driver, bit7=passenger, bit4-5=rear doors, bit3=trunk; drives door overlay |
| `0x0506` | 1286 | MCU→A | Reverse camera resolution — obj[0..1]=width, obj[2..3]=height (LE shorts); used to compute display scaling |
| `0x0507` | 1287 | MCU→A | Parking radar 8-sensor — bytes 0-3=front distances, byte4=front max, bytes 5-8=rear distances, byte9=rear max; drives `RadarView` |
| `0x0508` | 1288 | MCU→A | Outdoor ambient temperature — obj[1]=raw °C (127=invalid); displayed as `"Ext N°C"` in AC popup |
| `0x0509` | 1289 | MCU→A | Full AC/climate state — multi-byte: AC on/off, fan speed+direction, left/right seat setpoints, flags (MAX/rear-defrost/recirc/ECO), blower zone |
| `0x0518` | 1304 | MCU→A | MCU RTC time sync on boot — 7-byte packet (hour/min/sec/year/month/day); Android sets system clock; one-shot |
| `0x9E00` | -25088 | MCU→A | Foreground activity ID — arg1 stored in `mActivity`; used to decide audio source auto-restore |
| `0x9E11` | -25071 | MCU→A | Remote source-change request — arg2=target source ID, bit7="stop if current"; relays to code 769; from secondary controller |
| `0x9F10` | -24816 | ↔ | Power-off countdown — MCU→A: arg2=ms before shutdown, schedules `REQUEST_SHUTDOWN`; A→MCU: `write(40720, 0, ms)` (3000 or 7000 ms) |
| `0x9F1B` | -24805 | MCU→A | Hardware mute signal — arg1=0 unmuted, arg1≠0 muted; applies to `AudioManager` stream 3 (STREAM_MUSIC) |
| `0x9F1C` | -24804 | MCU→A | Reverse camera stream active — arg1=1 signal live, arg1=0 gone; propagated via AIDL `onReverseStatus()`; updates `f852ra` |

**TWUtil outbound-only commands** (not in subscription list — Android → MCU):

| Code (hex) | Decimal | Purpose |
|---|---|---|
| `0x9F1A` | 40730 | System shell command — payload is ASCII string e.g. `"pm hide com.tw.ac"` |

**Additional TWUtil codes** (observed in `com.tw.reverse` subscription, absent from TWService):

| Code (hex) | Dec | Description |
|---|---|---|
| `0x010B` | 267 | Unknown — subscribed by reverse camera app immediately after 266 (firmware version); possibly a secondary version field |
| `0x050E` | 1294 | Unknown — subscribed by reverse camera alongside radar/camera codes (1286, 1287); purpose unconfirmed |

Speed/ADAS/AC data codes are consumed by Dofun's privileged theme plugin directly
via TWUtil — not distributed through TWService.

**Native library stack** (confirmed from `/system_tw/lib/`):
- `libtwutil2.so` — MCU communication (primary, used by TWUtil)
- `libtwutilcan.so` — CAN bus variant
- `libSerialPort_jni.so` — low-level serial port JNI (android-serialport-api)
- `libadas.so` — ADAS processing
- `tuner_8035.so` — QN8035 FM/AM tuner (matches hardware spec)
- `libimobiledevice.so` + `libplist.so` + `libusbmuxd.so` — iOS/CarPlay via USB

**No custom Binder services** — `service list` returns 155 standard Android services only.
All com.tw.* IPC uses bound services (bindService) or TWUtil, not registered Binder names.
Notable standard service available: `serial: [android.hardware.ISerialManager]` (index 40).

**`com.tw.service` (TWService) role** — system event dispatcher, NOT vehicle data:
- Subscribes to MCU volume/mode/rotation events via TWUtil
- Uses `pm hide`/`pm unhide` via `TWUtil.write(0x9F1A, ...)` to show/hide source apps (com.tw.ac, com.tw.radio, com.tw.color, com.tw.dvd, etc.)
- Sends limited broadcasts: `com.unisound.intent.action.*` (voice assistant),
  `com.zjinnova.zlink.action.OUT_DARK_*` (ZLINK sleep), `com.baony.tw360.action.rotate`
- Writes to `/sys/class/tw/misc/navi` sysfs (exact purpose unknown)
- `onBind()` returns null — not bindable, no AIDL interface

**`com.tw.service.xt` (CommandService) role** — steering wheel media button dispatcher:
- Exposes AIDL interface `ITWCommandAidl` (bind action `com.tw.service.xt`) for media button commands
- `com.tw.keypad` (APK not yet extracted) receives physical button press from MCU and calls this AIDL
- `CommandService.sendSystemFunction()` dispatch logic:

| AIDL method | Radio source (mSource=1 or 9) | All other sources |
|---|---|---|
| `mediaNext()` (tx 55) | `TWUtil.write(513, 1, 19)` — AVRCP next track | `sendKeyCode(87)` KEYCODE_MEDIA_NEXT |
| `mediaPre()` (tx 56) | `TWUtil.write(513, 1, 21)` — AVRCP prev track | `sendKeyCode(88)` KEYCODE_MEDIA_PREVIOUS |
| `mediaPlay()` (tx 57) | n/a | `sendKeyCode(85)` KEYCODE_MEDIA_PLAY_PAUSE |
| `mediaPause()` (tx 58) | n/a | `sendKeyCode(127)` KEYCODE_MEDIA_STOP |

- Full steering wheel media button chain:
```
MCU (physical button press)
    → com.tw.keypad (APK not extracted — MCU→keypad signal path unconfirmed)
    → ITWCommandAidl.mediaNext() / mediaPre() / mediaPlay() / mediaPause()
    → CommandService.sendSystemFunction()
    → sendKeyCode(87/88/85/127)    ← normal media sources
    OR TWUtil.write(513, 1, 19/21) ← radio source only (AVRCP-style)
```

**`com.tw.core` (TWCore) role** — platform services host, NOT vehicle data:
- Runs `MainService` + `DaemonService` watchdog + `MessengerService` (Messenger IPC)
- Contains `com.tw.core.track` package — passive GPS trip logger (`GpsModel`, `CoordType`)
- Shows notification overlay via `NotificationReceiver`/`NotificationView`
- Hidden `MainActivity` can install `launcher.apk` or `TWCore.apk` from `/sdcard/DoFun/`
- Bundles Eclipse Paho MQTT library (no active usage sites found in source)
- No TWUtil subscription, no MCU codes, no AIDL. Helm can ignore this entirely.

**`com.tw.coreservice` (TDService) role** — watchdog only:
- Monitors if `com.tw.core` is running, restarts it via `Intent("com.tw.core.model.MainService")`
- Self-updates TDService.apk from remote server
- Captures logcat on crash and uploads to cloud
- Has NOTHING to do with MCU data

**Raw UART frame format** (for `com.tw.uart` direct path via `writeUartData` / `onUartDataUpdate`):
```
byte[0]     = 0xF2          ← frame start marker
byte[1]     = dataType      (0x00=normal, 0xA0=firmware update)
byte[2]     = cmdType       ← event type identifier
byte[3]     = dataLen N     ← payload byte count
byte[4..N]  = payload
byte[4+N]   = checksum = (0xFF - sum(bytes[1..4+N-1])) & 0xFF
Baud rate: 115200
```

**AIDL interface for raw UART** (still valid, for root path):

AIDL interface name: `com.tw.uart.IUartController`
Bind action: `com.tw.uart.UartService.Bind`

| Transaction | Method | Purpose |
|---|---|---|
| 1 | `registerCallback(IUartReceiver cb)` | Subscribe to raw MCU bytes |
| 2 | `unregisterCallback(IUartReceiver cb)` | Unsubscribe |
| 3 | `openUart(int baudRate, String dev_ttyS)` | Open serial port |
| 4 | `closeUart()` | Close serial port |
| 5 | `writeUartData(byte[] data)` | Send raw command bytes to MCU |
| 6 | `updateDataTime(Bundle b)` | Set polling interval (default 50 ms, min 10 ms) |

Callback: `void onUartDataUpdate(byte[] bytes)` and `void onExtendedInterface(Bundle bundle)`.
Log tags: `"UartService"` (prints dev_ttyS path on open), `"SerialPort"`.

**Live MCU messages observed** (from logcat.bin, unit parked/idle):
```
I twutil2: Gui_ListenDataFromMcu what=202 arg1=6 arg2=0   ← heartbeat ~3 sec
I twutil2: libDataFromMcu what=10a, bytes: 54 31 33 2e... ← "T13.1.1-10" version
D TWService: mcuVersion:A6
D TWService: dspCode:3 isMagicEQ=true
D TWService: 0x0112 msg.arg1:8113
```

**Helm MCU strategy:**
- **Without root** (Phase 3 start): GPS speed + MediaSession media + standard Android APIs
- **With root** (system app install): use `android.tw.john.TWUtil` directly for all MCU events

**Speed:** delivered continuously, thresholds at 5, 10, 20, 30, 50, 70, 90 km/h.
Displayed as large numeric text (`tv_carspeed_text`, 60sp) + unit (`tv_carspeed_unit`, 22sp).

**ADAS events** received by the launcher:
- `car_effect_adas_warning_stop_go` — stop/go warning
- `car_effect_adas_warning_car` — close-distance warning
- `car_effect_adas_warning_lane` — lane departure
- `car_effect_adas_warning_lane_left` / `lane_right`

**Media widget** displays: song name, 170×170 round album art, progress bar,
prev/play/next controls. Supports: local music, FM radio, Bluetooth, Spotify,
Apple Music, YouTube Music, Kugou, Kuwo, Ximalaya.

**Weather widget** shows animated effects for: fine, cloudy, rain, thunderstorm,
snow, haze.

### Reverse Camera — Entry Points (confirmed via jadx)

APK: `com.tw.reverse` — also `android:sharedUserId="android.uid.system"`.

| Entry point | How to reach | Notes |
|-------------|-------------|-------|
| `com.tw.reverse.StreamMediaActivity` | `getLaunchIntentForPackage("com.tw.reverse")` | Main app, LAUNCHER category |
| `cn.cardoor.desktop.window.floating.intent.action.LAUNCH` | `am start -a` or `sendBroadcast` | Floating window — may work from non-system app |
| `com.tw.reverse.ReverseActivity` | Not externally accessible | No action in IntentFilter; listens for broadcast `Constant.REVERSE_EXIT_FRONT` to auto-finish when gear leaves reverse |
| `com.tw.reverse.RadarService` | Exported service | Parking radar display — `RadarView2` internally subscribes to code 1287 and calls `showView()`/`hideView()` autonomously |

`RadarService` creates a system overlay window (type 2038 = `TYPE_APPLICATION_OVERLAY`) at 920×460dp or 600×300dp depending on aspect ratio.

`StreamMediaActivity` uses `TWUtil.write(40730, ...)` (`pm enable/disable com.tw.reverse/.StreamMediaActivity`) to show/hide itself — same `0x9F1A` shell command mechanism as TWService.

The same floating window action pattern (`cn.cardoor.desktop.window.*`) appears in
the `com.tw.reverse` manifest, the `DesktopWindowService` in Dofun, and likely other
OEM apps — it is the platform-level multi-window overlay mechanism.

### CarMate Broadcasts (confirmed via jadx)

CarMate (`com.dofun.carassistant.car`) emits these broadcasts:
- `com.dofun.intent.action.TRACK_SWITCH` — media track change notification
- `com.dofun.location.DESTROY_LOCATIONSERVICE` — location service lifecycle

CarMate's `CarService.java` immediately binds to `com.tw.carinfoservice` on create
(action `com.tw.carinfoservice.CarService.Bind`) — confirming CarMate was designed
as a UI layer over that missing intermediary. The vehicle data interface that
`carinfoservice` would have provided included callbacks for:
- Speed: `D(float speed, String unit)`
- AC setpoints: `E(float temp, String side)`, `h(float voltage, String)`
- Door state: `c(boolean open)`
- Tire pressure: `TirePressureBean` object
- Driving time: `i(int seconds)`

Since `carinfoservice` is not installed on this firmware, **CarMate is a dead end
for MCU data**. All vehicle signals must come directly via TWUtil.

### What is still unknown
- TWUtil codes for speed, AC setpoints, ADAS — consumed directly by Dofun's privileged plugin; code numbers unknown without root.
- `ExportedProvider` KV contents — may be a usable data channel for Helm; needs `adb shell content query --uri content://com.dofun.variety.ExportedProvider`.
- Function of `com.ms.ms2160` (MS912X.apk) — unknown hardware module in `/system_tw/`.
- `com.tw.rightview` entry points — APK not yet analyzed.
- `com.tw.keypad` MCU signal path — main activity `com.tw.keypad.SteeringWheelActivity` confirmed; which TWUtil code or UART frame triggers it is unknown.
- TWUtil codes `0x010B` (267) and `0x050E` (1294) — observed in `com.tw.reverse` subscription, purpose unknown.

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

