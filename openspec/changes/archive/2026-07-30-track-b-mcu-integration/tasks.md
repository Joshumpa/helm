## 1. SDK — MCU Service Foundation

- [x] 1.1 Define `McuEvent` sealed class in `:sdk` covering all subscribed TWUtil message codes from `docs/mcu-protocol.md`
- [x] 1.2 Define `DataError` sealed class (`Unavailable`, `ParseFailure`, `WriteFailure`) in `:sdk`
- [x] 1.3 Create `McuDataSource` interface in `:sdk` with `events(): Flow<McuEvent>` and `send(code: Int, arg1: Int, arg2: Int, data: ByteArray)` 
- [x] 1.4 Implement `StubMcuDataSource` emitting safe neutral defaults (speed=0, day=Day, etc.)
- [x] 1.5 [Track B] Implement `TwUtilAdapter` in `:sdk` — reflective wrapper over `android.tw.john.TWUtil` with `open(shortArray)`, `start()`, `write()`, and `handleMessage` bridge into `callbackFlow<McuEvent>`
- [x] 1.6 [Track B] Implement `TwUtilMcuDataSource` backed by `TwUtilAdapter`; subscribe to full code list from `mcu-protocol.md`
- [x] 1.7 Add Hilt module in `:sdk` that binds `TwUtilMcuDataSource` when `Process.myUid() == Process.SYSTEM_UID`, else `StubMcuDataSource`
- [x] 1.8 Implement `McuService` in `:sdk` consuming `McuDataSource` events and exposing typed flows: `dayNight()`, `audioSource()`, `doorState()`, `ambientTemperature()`
- [x] 1.9 Implement `McuService.powerOffCountdown()` — handles code `0x9F10`, schedules `REQUEST_SHUTDOWN`, sends MCU ack `write(40720, 0, 3000)`
- [x] 1.10 Implement time sync in `McuService` — handles code `0x0518` (set system clock on boot) and pushes time to MCU on `TIME_TICK`/`TIME_SET` via code `0x0107`
- [x] 1.11 Add exponential back-off reconnection logic to `TwUtilMcuDataSource`; all flows emit `DataError.Unavailable` while disconnected
- [x] 1.12 Write unit tests for `McuEvent` parsing (code decoding, payload extraction) using `StubMcuDataSource`

## 2. SDK — Audio Routing

- [x] 2.1 Add `McuAudioRouter` to `:sdk` observing `McuDataSource` events for codes `0x0106`, `0x0203`, `0x0301`, `0x0302`, `0x9F1B`
- [x] 2.2 Implement steering-wheel volume sync: code `0x0203` → `AudioManager.setStreamVolume(STREAM_MUSIC, level)`; mute flag → `AudioManager.adjustStreamVolume(MUTE)`
- [x] 2.3 Implement MCU volume level sync: code `0x0106` obj[0] → `setStreamVolume(STREAM_MUSIC, level)`
- [x] 2.4 Implement audio source flow `McuService.audioSource(): Flow<AudioSource>` driven by code `0x0301`
- [x] 2.5 Implement BT call routing: code `0x0302` arg1≠0 → mute STREAM_MUSIC + switch to call audio; arg1=0 → unmute + restore source
- [x] 2.6 Implement hardware mute: code `0x9F1B` → `setStreamMute(STREAM_MUSIC, arg1 != 0)` for stream 3
- [x] 2.7 Connect `:audio` music player to `McuService.audioSource()` so it pauses when source changes away from USB/music

## 3. SDK — Vehicle Data Flows

- [x] 3.1 Add `McuService.speed(): Flow<Int>` — stub emits 0; [Track B] wire to privileged TWUtil vehicle data code (TBD post-FEL)
- [x] 3.2 Confirm `McuService.dayNight(): Flow<DayNight>` is driven by code `0x0204` (arg1=1→Night, arg1=0→Day)
- [x] 3.3 Implement `McuService.doorState(): Flow<DoorState>` decoding code `0x0505` bitmask (driver/passenger/rear/trunk)
- [x] 3.4 Implement `McuService.ambientTemperature(): Flow<Int?>` from code `0x0508` obj[1]; emit `null` when value=127

## 4. Theme — Day/Night Auto-Switch

- [x] 4.1 In `:themes`, add a `McuDayNightObserver` (ViewModel or Application-scope coroutine) that observes `McuService.dayNight()` and writes `helm_day_night` to DataStore
- [x] 4.2 Verify existing `ThemeRepository` DataStore → Compose theme binding responds to the new programmatic writes without user interaction

## 5. Radio — Real Implementation

- [x] 5.1 [Track B] Implement `RadioTuner` in `:sdk` — exposes `tune(frequency: Float, band: Band)`, `seek(direction: SeekDirection)`, `currentStation(): Flow<RadioStation>`
- [x] 5.2 [Track B] Wire radio MCU commands via `TwUtilAdapter.write()` matching OEM `com.tw.radio` command patterns (derive from decompile analysis)
- [x] 5.3 [Track B] Implement RDS PS name extraction from MCU radio data; surface as `RadioStation.name: String?`
- [x] 5.4 Implement preset storage in `:radio` — up to 12 slots, `Room` or `DataStore`, persisted across restart
- [x] 5.5 Replace `:radio` stub UI with real `RadioScreen` — frequency display, band toggle, seek buttons, preset grid (portrait, max 3 cols)
- [x] 5.6 Show "Unavailable (Track B)" banner in `:radio` when `StubMcuDataSource` is active
- [x] 5.7 Apply `.systemBarsPadding()` on root `Column` of `RadioScreen`

## 6. Reverse Camera

- [x] 6.1 Create `:camera` module (or feature package inside `:sdk`) with `ReverseCameraManager`
- [x] 6.2 [Track B] Wire `ReverseCameraManager` to MCU codes `0x020B` (gear), `0x9F1C` (stream live), `0x0506` (resolution)
- [x] 6.3 Implement `ReverseCameraActivity` — full-screen portrait `SurfaceView`; launched by a foreground service when `0x020B` arg1=1 is received; back press is no-op in reverse
- [x] 6.4 Implement MCU ack: send `write(772, 1)` when camera view becomes visible; `write(772, 0)` on dismiss
- [x] 6.5 Implement parking radar overlay `RadarOverlayView` driven by code `0x0507` — 8-sensor arc display; sensor turns red below critical distance; audible alert via `AudioManager`
- [x] 6.6 Handle `0x9F1C` arg1=0 (stream lost) while camera view is open → show "No signal" placeholder without dismissing
- [x] 6.7 Show static placeholder + "Unavailable (Track B)" in stub mode
- [x] 6.8 Apply `.systemBarsPadding()` to `ReverseCameraActivity` root layout

## 7. CarPlay — Real Implementation

- [x] 7.1 [Track B] Implement CarPlay session lifecycle in `:carplay` SDK layer — observes `McuDataSource` code `0x0205`; exposes `CarPlay.state(): Flow<CarPlayState>` (Disconnected / Active)
- [x] 7.2 [Track B] On session start, send `write(769, 192, 5)` (audio source = CarPlay); on session end, restore previous source
- [x] 7.3 Replace `:carplay` stub UI with `CarPlayScreen` — idle "Connect iPhone via USB" state + active session view
- [x] 7.4 Show "Unavailable (Track B)" banner in stub mode
- [x] 7.5 Apply `.systemBarsPadding()` on root `Column` of `CarPlayScreen`

## 8. Integration & Validation

- [x] 8.1 Run `./gradlew assembleDebug` — confirm all modules compile with both stub and [Track B] code paths
- [x] 8.2 Run `./gradlew detekt` — confirm no new violations
- [x] 8.3 Run `./gradlew :sdk:test` — confirm `McuEvent` parsing unit tests pass
- [ ] 8.4 [Track B] On-device smoke test post-FEL: confirm `TwUtilMcuDataSource` binds, heartbeat code `0x0202` is received, day/night `0x0204` switches theme
- [ ] 8.5 [Track B] On-device radio test: tune FM, seek, confirm RDS PS name appears
- [ ] 8.6 [Track B] On-device reverse camera test: engage reverse, confirm camera appears within 500 ms, radar overlay updates, camera dismisses on forward gear
