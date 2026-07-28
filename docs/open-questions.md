# Open Questions (Phase 1 RE)

Items still unknown after Phase 1 reverse engineering. Root access would resolve most of these.

---

## Vehicle Data Codes

**TWUtil codes for speed, AC setpoints, and ADAS** are consumed directly by Dofun's
privileged theme plugin via TWUtil. Code numbers are unknown without root, since the
relevant APK is protected by 360 Jiagu and jadx cannot decompile it.

## ExportedProvider Contents

`content://com.dofun.variety.ExportedProvider` — may be a usable data channel for Helm.
Reads are open (no permission required). Query has not been executed yet.

```bash
adb shell content query --uri content://com.dofun.variety.ExportedProvider
```

## Unknown Hardware Module

`com.ms.ms2160` (MS912X.apk) — confirmed installed in `/system_tw/`. Function is unknown.
No Intent analysis done yet.

## Lateral Camera Entry Points

`com.tw.rightview` — APK not yet analyzed. MCU code `0x020C` triggers it (arg1=1 rightView,
arg1=2 leftView, arg1=0 dismiss) but the actual entry point into the app is unconfirmed.

## Steering Wheel → Keypad Signal Path

`com.tw.keypad` main activity `com.tw.keypad.SteeringWheelActivity` is confirmed.
Which TWUtil code or raw UART frame the MCU sends to trigger it is unknown — APK not extracted.

## Unknown TWUtil Codes

| Code | Dec | Status |
|---|---|---|
| `0x010B` | 267 | Subscribed by `com.tw.reverse` immediately after 266 (firmware version); purpose unconfirmed |
| `0x050E` | 1294 | Subscribed by `com.tw.reverse` alongside radar/camera codes; purpose unconfirmed |
