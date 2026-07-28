# MCU Protocol Reference

Full documentation of the MCU ↔ Android communication stack, TWUtil message codes,
and the role of each system service. Derived from logcat.bin analysis and
`twservice-src` decompilation.

---

## Architecture

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

**`android.tw.john.TWUtil`** is the real MCU subscription API — a hidden system class
provided by AllWinner in the platform SDK. Any process with
`android:sharedUserId="android.uid.system"` can use it. Helm requires root to access.

Usage pattern (from `twservice-src/com/p018tw/service/C0282Pa.java`):
```java
twUtil.open(new short[]{
    258, 260, 516, 517, 528, 262, 263, 266, 274,
    513, 514, 515, 523, 524,
    769, 770, 772, 1028,
    1281, 1285, 1286, 1287, 1288, 1289, 1304,
    -25088, -25071, -24816, -24805, -24804
});
twUtil.start();
// Receive events in overridden handleMessage(Message msg)
// Send commands: twUtil.write(int code, int arg1, int arg2, byte[] data)
```

---

## TWUtil Message Codes

Direction: MCU→A = MCU to Android (inbound); A→MCU = Android to MCU (outbound only); ↔ = bidirectional.

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
| `0x9E11` | -25071 | MCU→A | Remote source-change request — arg2=target source ID, bit7="stop if current"; relays to code 769 |
| `0x9F10` | -24816 | ↔ | Power-off countdown — MCU→A: arg2=ms before shutdown, schedules `REQUEST_SHUTDOWN`; A→MCU: `write(40720, 0, ms)` (3000 or 7000 ms) |
| `0x9F1B` | -24805 | MCU→A | Hardware mute signal — arg1=0 unmuted, arg1≠0 muted; applies to `AudioManager` stream 3 (STREAM_MUSIC) |
| `0x9F1C` | -24804 | MCU→A | Reverse camera stream active — arg1=1 signal live, arg1=0 gone; propagated via AIDL `onReverseStatus()`; updates `f852ra` |

### Outbound-only commands (Android → MCU, not in subscription list)

| Code (hex) | Decimal | Purpose |
|---|---|---|
| `0x9F1A` | 40730 | System shell command — payload is ASCII string e.g. `"pm hide com.tw.ac"` |

### Additional codes (observed in `com.tw.reverse`, absent from TWService)

| Code (hex) | Dec | Description |
|---|---|---|
| `0x010B` | 267 | Unknown — subscribed by reverse camera app immediately after 266; possibly a secondary version field |
| `0x050E` | 1294 | Unknown — subscribed by reverse camera alongside radar/camera codes (1286, 1287); purpose unconfirmed |

Speed/ADAS/AC data codes are consumed by Dofun's privileged theme plugin directly
via TWUtil — not distributed through TWService. Code numbers unknown without root.

---

## Raw UART Frame Format

For `com.tw.uart` direct path via `writeUartData` / `onUartDataUpdate`:

```
byte[0]     = 0xF2          ← frame start marker
byte[1]     = dataType      (0x00=normal, 0xA0=firmware update)
byte[2]     = cmdType       ← event type identifier
byte[3]     = dataLen N     ← payload byte count
byte[4..N]  = payload
byte[4+N]   = checksum = (0xFF - sum(bytes[1..4+N-1])) & 0xFF
Baud rate: 115200
```

### AIDL interface for raw UART

Interface name: `com.tw.uart.IUartController`  
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

---

## Live MCU Messages Observed (logcat.bin, unit parked/idle)

```
I twutil2: Gui_ListenDataFromMcu what=202 arg1=6 arg2=0   ← heartbeat ~3 sec
I twutil2: libDataFromMcu what=10a, bytes: 54 31 33 2e... ← "T13.1.1-10" version
D TWService: mcuVersion:A6
D TWService: dspCode:3 isMagicEQ=true
D TWService: 0x0112 msg.arg1:8113
```

---

## Service Roles

### `com.tw.service` (TWService) — system event dispatcher, NOT vehicle data

- Subscribes to MCU volume/mode/rotation events via TWUtil
- Uses `pm hide`/`pm unhide` via `TWUtil.write(0x9F1A, ...)` to show/hide source apps
- Sends limited broadcasts: `com.unisound.intent.action.*` (voice), `com.zjinnova.zlink.action.OUT_DARK_*` (ZLINK sleep), `com.baony.tw360.action.rotate`
- Writes to `/sys/class/tw/misc/navi` sysfs
- `onBind()` returns null — not bindable, no AIDL interface

### `com.tw.service.xt` (CommandService) — steering wheel media button dispatcher

Exposes AIDL interface `ITWCommandAidl` (bind action `com.tw.service.xt`).  
`com.tw.keypad` receives physical button press from MCU and calls this AIDL.

| AIDL method | Radio source (mSource=1 or 9) | All other sources |
|---|---|---|
| `mediaNext()` (tx 55) | `TWUtil.write(513, 1, 19)` — AVRCP next | `sendKeyCode(87)` KEYCODE_MEDIA_NEXT |
| `mediaPre()` (tx 56) | `TWUtil.write(513, 1, 21)` — AVRCP prev | `sendKeyCode(88)` KEYCODE_MEDIA_PREVIOUS |
| `mediaPlay()` (tx 57) | n/a | `sendKeyCode(85)` KEYCODE_MEDIA_PLAY_PAUSE |
| `mediaPause()` (tx 58) | n/a | `sendKeyCode(127)` KEYCODE_MEDIA_STOP |

Full steering wheel button chain:
```
MCU (physical button press)
    → com.tw.keypad (signal path from MCU unconfirmed — APK not extracted)
    → ITWCommandAidl.mediaNext() / mediaPre() / mediaPlay() / mediaPause()
    → CommandService.sendSystemFunction()
    → sendKeyCode(87/88/85/127)    ← normal media sources
    OR TWUtil.write(513, 1, 19/21) ← radio source only (AVRCP-style)
```

### `com.tw.core` (TWCore) — platform services host, Helm can ignore

- Runs `MainService` + `DaemonService` watchdog + `MessengerService` (Messenger IPC)
- Contains `com.tw.core.track` — passive GPS trip logger
- Shows notification overlay via `NotificationReceiver`/`NotificationView`
- Hidden `MainActivity` can install `launcher.apk` or `TWCore.apk` from `/sdcard/DoFun/`
- Bundles Eclipse Paho MQTT library (no active usage found)
- No TWUtil subscription, no MCU codes, no AIDL

### `com.tw.coreservice` (TDService) — watchdog only

- Monitors if `com.tw.core` is running, restarts it
- Self-updates TDService.apk from remote server
- Captures logcat on crash and uploads to cloud
- Has nothing to do with MCU data

---

## Helm MCU Integration Strategy

**Without root** (current Phase 3 baseline):
- GPS speed via Android Location API
- Media metadata via `MediaSession` / `NotificationListenerService`
- Standard Android volume/audio APIs

**With root** (system app install — future):
- Use `android.tw.john.TWUtil` directly for all MCU events
- Speed delivered continuously, thresholds at 5, 10, 20, 30, 50, 70, 90 km/h
- ADAS events: `car_effect_adas_warning_stop_go`, `car_effect_adas_warning_car`,
  `car_effect_adas_warning_lane`, `car_effect_adas_warning_lane_left/right`
- AC/climate, door state, parking radar, day/night mode, power-off countdown

**No custom Binder services** — `service list` returns 155 standard Android services.
All `com.tw.*` IPC uses bound services or TWUtil, not registered Binder names.
Notable standard service available: `serial: [android.hardware.ISerialManager]` (index 40).
