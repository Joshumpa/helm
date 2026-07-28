# OEM Platform Reference

Phase 1 RE findings: package map, manifest analysis, entry points, and peripheral integrations.
Packages confirmed via `pm list packages -f` from TermOne Plus (no root).

---

## System Package Map

| Function | Package (confirmed) |
|---|---|
| MCU UART (low-level) | `com.tw.uart` ← **primary MCU channel** (system UID, AIDL) |
| MCU intermediary | `com.tw.carinfoservice` ← **confirmed NOT installed** (logcat boot error) |
| MCU bridge (app layer) | `com.dofun.carassistant.car` (CarMate) ← binds to carinfoservice, doesn't exist |
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
| Steering wheel | `com.tw.keypad` — main activity: `com.tw.keypad.SteeringWheelActivity` |
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

---

## AndroidManifest Analysis (static, from downloaded APK v168)

### Permissions Dofun declares (Helm will need most of these)

`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`,
`ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, `INTERNET`,
`READ_PHONE_STATE`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`,
`FOREGROUND_SERVICE`, `RECEIVE_BOOT_COMPLETED`, `BROADCAST_STICKY`,
`READ_LOGS`, `SYSTEM_ALERT_WINDOW`, `EXPAND_STATUS_BAR`, `BLUETOOTH`,
`CAMERA`, `REQUEST_INSTALL_PACKAGES`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`,
`MEDIA_CONTENT_CONTROL` *(signature-level — may require system app),*
`com.tencent.wecarflow.PLAY_CONTROL` *(Tencent CarPlay/CarLife integration)*

### Custom permissions Dofun defines

| Permission | Purpose |
|---|---|
| `cn.cardoor.variety.permission.ENTRANCE_ICON_FILTER` | Gates which apps appear in the launcher icon grid |
| `com.dofun.variety.permission.provider.exported.WRITE_KV_CONFIG` | Required to write to the ExportedProvider content provider |

### Exported activities

| Activity | How to reach |
|---|---|
| `com.dofun.overseasvariety.Launcher` | Home screen — launched on boot via `MAIN`+`HOME` |
| `com.dofun.variety.online.DomesticOnlineActivity` | `MAIN` action; also `launcher://variety/theme/category/one`, `.../two`, `.../online/theme` |
| `com.dofun.variety.theme.details.ThemeDetailsActivity` | `launcher://variety/theme/category/details` |
| `com.dofun.overseasvariety.activity.ScreenSaverActivity` | `cn.cardoor.intent.action.DAY_DREAM` or direct `am start` |
| `com.dofun.variety.router.EnterSettingActivity` | `launcher://variety/setting` (autoVerify=true) |

ADB test commands:
```bash
adb shell am start -a android.intent.action.VIEW -d "launcher://variety/setting"
adb shell am start -n com.dofun.variety/.overseasvariety.activity.ScreenSaverActivity
adb shell am broadcast -a cn.cardoor.intent.action.DAY_DREAM
```

### Services of interest

| Service | Action | Notes |
|---|---|---|
| `cn.cardoor.basic.media.NotifyService` | `android.service.notification.NotificationListenerService` | Reads all notifications — how Dofun shows music metadata on the home screen widget. Helm `:widgets` needs its own NotificationListenerService. |
| `cn.cardoor.desktop.window.DesktopWindowService` | `cn.cardoor.desktop.window.DESKTOP_WINDOW_SERVICE` | Floating multi-window overlay service. Test: `adb shell am startservice -a cn.cardoor.desktop.window.DESKTOP_WINDOW_SERVICE` |

### Broadcast receivers

Only **one** exported receiver: `com.dofun.variety.VarietyReceiver`  
Listens to: `BOOT_COMPLETED`, `LOCALE_CHANGED`.

No MCU or reverse-camera receiver is exposed in this APK. MCU signals come from
a separate system APK or HAL layer, not from `com.dofun.variety`.

### Content providers

| Authority | Exported | Notes |
|---|---|---|
| `com.dofun.variety.ExportedProvider` | **yes** | The only external hook. Reads are open; writes require `WRITE_KV_CONFIG` permission. |

```bash
adb shell content query --uri content://com.dofun.variety.ExportedProvider
```

---

## Reverse Camera Entry Points (confirmed via jadx)

APK: `com.tw.reverse` — `android:sharedUserId="android.uid.system"`.

| Entry point | How to reach | Notes |
|-------------|-------------|-------|
| `com.tw.reverse.StreamMediaActivity` | `getLaunchIntentForPackage("com.tw.reverse")` | Main app, LAUNCHER category |
| `cn.cardoor.desktop.window.floating.intent.action.LAUNCH` | `am start -a` or `sendBroadcast` | Floating window — may work from non-system app |
| `com.tw.reverse.ReverseActivity` | Not externally accessible | Listens for broadcast `Constant.REVERSE_EXIT_FRONT` to auto-finish when gear leaves reverse |
| `com.tw.reverse.RadarService` | Exported service | Parking radar display — `RadarView2` subscribes to code 1287 and calls `showView()`/`hideView()` autonomously |

`RadarService` creates a system overlay window (type 2038 = `TYPE_APPLICATION_OVERLAY`) at 920×460dp or 600×300dp depending on aspect ratio.

`StreamMediaActivity` uses `TWUtil.write(40730, ...)` (`pm enable/disable`) to show/hide itself — same `0x9F1A` shell command mechanism as TWService.

The floating window pattern (`cn.cardoor.desktop.window.*`) appears in `com.tw.reverse`, `DesktopWindowService` in Dofun, and other OEM apps — it is the platform-level multi-window overlay mechanism.

---

## CarMate Broadcasts (confirmed via jadx)

CarMate (`com.dofun.carassistant.car`) emits:
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
