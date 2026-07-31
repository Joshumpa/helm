## Context

See `proposal.md — Why` for motivation. The current state relevant to this design:

- `:sdk` contains stub implementations of `McuService`, `RadioTuner`, `ReverseCamera`, and `CarPlay` that emit static values; they compile and are wired to feature modules.
- `:radio` and `:carplay` render these stubs as UI placeholders with no real data path.
- `android.tw.john.TWUtil` is a `@hide` platform class available only to processes sharing `android.uid.system`; Helm does not have this privilege until FEL root is obtained.
- `libtwutil2.so` and `tuner_8035.so` are present in `/system_tw/lib/` (confirmed via hardware docs).
- The UART frame format and all message codes are documented in `docs/mcu-protocol.md`.

---

## Goals / Non-Goals

**Goals:**
- Replace stub `McuService` with a real TWUtil-backed implementation that compiles in both stub and real modes.
- Provide typed, Flow-based SDK surface for all Track B data (speed, day/night, audio source, doors, temperature, power-off, time sync).
- Replace `:radio` stub with a real `RadioTuner` backed by the MCU/QN8035.
- Replace `:carplay` stub with a session lifecycle driven by MCU codes.
- Implement reverse camera as a full-screen portrait view with parking radar overlay.
- Wire day/night MCU events to the existing theme system.
- Synchronise Android audio state with steering-wheel controls and MCU source switches.
- All modules MUST compile and show safe stub UI without root.

**Non-Goals:**
- ADAS overlay (speed/distance data from the privileged Dofun plugin path — codes TBD; deferred to v3).
- AC/climate UI (codes `0x0104`, `0x0509` are decoded but no Helm screen is planned for v2).
- OBD-II integration.
- CarPlay wireless (USB ZLINK only for v2).

---

## Decisions

### 1. TWUtil binding via reflection + `callbackFlow`

**Decision:** Wrap `android.tw.john.TWUtil` with a thin reflective adapter in `:sdk`. The adapter calls `open(shortArray)`, `start()`, and `write()` via reflection so the feature module code compiles without the platform SDK on developer machines. The callback (`handleMessage`) is bridged into a `callbackFlow<McuEvent>` and shared with `shareIn(SharingStarted.WhileSubscribed)`.

**Alternatives considered:**
- *Direct platform SDK dependency (compileOnly)*: Cleaner but requires every dev machine and CI to have the AllWinner platform SDK installed. Reflection costs only a few ms at startup and is isolated to one class.
- *AIDL to `com.tw.uart` (raw UART)*: Bypasses TWUtil entirely. Viable for Track B experimentation but requires implementing frame parsing in Kotlin on top of the raw `IUartController` AIDL. TWUtil already parses frames; using it avoids re-implementing the protocol.

**Rationale:** Reflection keeps the build self-contained; the platform SDK is not in a Maven repo and the path on-device is not reproducible in CI.

---

### 2. Stub/real switching via Hilt multi-binding

**Decision:** Define a `McuDataSource` interface. Provide two implementations — `StubMcuDataSource` (emits hardcoded safe values) and `TwUtilMcuDataSource` (real). Select at runtime: if `Process.myUid() == Process.SYSTEM_UID`, use real; otherwise use stub. Inject via Hilt `@Binds` + a module that conditionally binds the correct impl.

**Alternatives considered:**
- *Build flavor (`stubDebug` / `realRelease`)*: Prevents running stub UI on the device without changing the build. A runtime check lets a sideloaded debug APK fall back to stubs gracefully if FEL mode isn't yet active.
- *`BuildConfig.TRACK_B_ENABLED` flag*: Simpler but requires a rebuild to toggle; the UID check is zero-configuration.

---

### 3. Reverse camera as a dedicated `Activity` (not Composable)

**Decision:** Implement the reverse camera as a separate `Activity` with a `SurfaceView`. MCU gear signal triggers `startActivity` from the SDK foreground service; the Activity overrides `onBackPressed` to be a no-op while in reverse.

**Alternatives considered:**
- *Composable overlay via `DialogWindow`*: `SurfaceView` inside a Compose dialog has known Z-ordering bugs on API 29 where the `Surface` can render beneath system overlays.
- *Full Compose `AndroidView` wrapping `SurfaceView`*: Acceptable but adds a recomposition boundary for a view that needs zero-latency rendering. A plain Activity avoids any Compose overhead during the safety-critical reverse manoeuvre.

**Rationale:** Safety-critical UI should have the simplest possible rendering path.

---

### 4. Radio tuner via MCU UART, not direct `tuner_8035.so` JNI

**Decision:** Send all radio commands to the MCU over TWUtil (`twUtil.write`) rather than calling `tuner_8035.so` directly via JNI. The MCU firmware owns the QN8035 SPI bus; Android-side JNI to the tuner would race with MCU access and is not supported by the OEM architecture.

**Alternatives considered:**
- *Direct JNI to `tuner_8035.so`*: The native lib is present but the MCU already holds the tuner. Concurrent access would produce undefined behaviour (observed in TWService source: all tuner commands go through MCU codes).

---

### 5. Day/night MCU → `ThemeRepository` via `McuService` flow observation

**Decision:** `ThemeRepository` (already in `:themes`) observes `McuService.dayNight()`. When `DayNight.Night` arrives it writes `DataStore` key `helm_day_night = NIGHT`; when `DayNight.Day` arrives it writes `DAY`. This reuses the existing DataStore→Compose theme binding with zero new API surface.

**Alternatives considered:**
- *Direct MCU observer in the theme Composable*: Would put hardware awareness in UI layer — violates Clean Architecture.

---

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Root / FEL not yet obtained — no on-device validation of Track B code | All real paths are behind UID check; stub paths are validated on Track A. End-to-end tests deferred to post-FEL. |
| Reflection against `@hide` TWUtil may break on a future firmware update | Reflection is isolated to `TwUtilAdapter.kt`. If the class changes, only that file needs updating. |
| `SurfaceView` reverse camera latency > 500 ms spec requirement | Measure on real hardware post-FEL. If latency is too high, switch to `TextureView`; the Activity shell stays the same. |
| Speed data codes unknown (privileged path, not in TWService logcat) | `McuService.speed()` stubs 0 until codes are identified post-root. Speed badge on home screen already handles 0 gracefully. |
| QN8035 seek algorithm not documented | Implement seek by stepping frequency in software loop until SNR crosses threshold — matches pattern seen in OEM `com.tw.radio` decompilation. |
| CarPlay USB stack (`libimobiledevice`) may require additional system permissions | Investigate at FEL time; stub UI is unaffected. |

---

## Migration Plan

1. **Phase 1 (now, pre-root):** Wire all new SDK interfaces to `StubMcuDataSource`. Feature modules compile and show stub UI. CI remains green.
2. **Phase 2 (post-FEL):** Add `TwUtilMcuDataSource`; enable on-device testing with the real TWUtil. Fix any issues found.
3. **Phase 3:** Enable CarPlay + reverse camera; validate safety-critical flows (camera latency, radar overlay accuracy) on hardware.
4. **Rollback:** Revert `TwUtilMcuDataSource` injection binding to point back to `StubMcuDataSource`. No schema or data migration needed.

---

## Open Questions

- Which exact TWUtil message codes carry vehicle speed and ADAS data? (Privileged path, not visible in logcat without root — answer post-FEL.)
- Does `tuner_8035.so` expose a JNI API that could supplement MCU tuner commands for RDS? (Low priority — MCU path should be sufficient for v2.)
