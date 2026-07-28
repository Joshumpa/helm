# Hardware & Platform Reference

## SoC & Firmware

| Field | Value |
|-------|-------|
| SoC | AllWinner A133 |
| Android | 10 (API 29) — Dofun UI reports "14" incorrectly |
| CPU mode | 32-bit (armeabi-v7a) — A133 is 64-bit physically but firmware is 32-bit |
| RAM | 4 GB |
| Storage | 64 GB |
| Kernel | 4.9.170 |
| Build | QP1A.191105.004 test-keys |
| SELinux | permissive — does not block unauthorized operations |
| Security patch | 2021-01-05 |
| Audio IC | PT2313 |
| Radio IC | QN8035 |
| Bluetooth | 5.4 |
| MCU version | T13.1.1-100-10-A6939D-241125(6) |
| Boot version | JBDxGFH_M8_WD097SHM30AA-D6_90_240522 |
| System version | V13.1.1_20260408.141817_OS-EN |
| App/Theme version | \13_20260408.140850_APP-THEME1 |

**Serial ports on device:** `/dev/ttyS0`, `/dev/ttyS1` (=Bluetooth), `/dev/ttyS2`,
`/dev/ttyS3`, `/dev/ttyS4`, `/dev/ttyS7`. MCU port is one of ttyS0/2/3/4/7.

**Native library stack** (confirmed from `/system_tw/lib/`):
- `libtwutil2.so` — MCU communication (primary, used by TWUtil)
- `libtwutilcan.so` — CAN bus variant
- `libSerialPort_jni.so` — low-level serial port JNI (android-serialport-api)
- `libadas.so` — ADAS processing
- `tuner_8035.so` — QN8035 FM/AM tuner (matches hardware spec)
- `libimobiledevice.so` + `libplist.so` + `libusbmuxd.so` — iOS/CarPlay via USB

---

## OEM App (Dofun)

| Field | Value |
|-------|-------|
| Package | `com.dofun.variety` |
| Main activity | `com.dofun.variety.online.DomesticOnlineActivity` |
| APK version | V7.10.28.168.240313 |
| APK protection | 360 Jiagu (code encrypted — jadx will not decompile main dex) |
| Plugin framework | RePlugin by Qihoo 360 |
| Internal URI scheme | `launcher://variety/theme/category/one` |

All OEM packages live in the `/system_tw/` partition, separate from `/system/`.
