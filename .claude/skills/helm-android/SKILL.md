---
name: helm-android
description: Helm-specific Android supplement. Use for AOSP/system API source lookup (@hide classes, IWindowManager, TWUtil patterns), sealed DataError propagation in the SDK layer, Flow callbackFlow bridging for MCU callbacks, modularization visibility discipline, and hardware debugging (real device, no emulator). Complements claude-android-ninja — do not duplicate Compose, Gradle, theming, navigation, or Material 3 guidance already covered there.
metadata:
  type: project
---

# Helm Android Supplement

Complements `claude-android-ninja` (installed globally). Only covers what that skill omits.
For Compose, Gradle, theming, navigation, Material 3, testing → use android-ninja.

---

## 1. AOSP & System API Source Lookup

Critical for Helm: TWUtil, `IWindowManager`, `AudioManager` internals, `@hide` APIs, and
system service implementations are not in public docs — they must be read from source.

**`cs.android.com` blocks automated fetching** (JS SPA). Use it as a human search UI to
find the file path, then fetch the actual source via one of the strategies below.

### AOSP (framework internals, `@hide` APIs, system services) → Gitiles

```
https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android10-release/{path}?format=TEXT
```

Use `android10-release` for API 29 (Helm's target). Response is base64-encoded raw text.
Decode with `base64 -d`. Key repos and paths:

| What | Repo | Path prefix |
|---|---|---|
| View, Activity, Context | `platform/frameworks/base` | `core/java/android/` |
| System services implementation | `platform/frameworks/base` | `services/core/java/` |
| Default attrs / styles | `platform/frameworks/base` | `core/res/res/values/` |
| Java core (IO, collections) | `platform/libcore` | `ojluni/src/main/java/` |
| Audio policy | `platform/frameworks/av` | `services/audiopolicy/` |

### AndroidX → GitHub

```bash
# Read a file
curl https://raw.githubusercontent.com/androidx/androidx/androidx-main/{path}

# List a directory
gh api repos/androidx/androidx/contents/{path} --jq '.[].name'
```

### When the path is unknown

1. Search `cs.android.com` as a human to find the class/file
2. Copy the path shown
3. Fetch via Gitiles (AOSP) or `raw.githubusercontent.com` (AndroidX)

Both trees change — always fetch current source, never trust remembered line numbers.

---

## 2. Repository Error Model (`:sdk` and data modules)

Every module in Helm that crosses a system boundary (MCU, OEM Intent, UART) must use
this pattern. Exceptions must not leak into ViewModels as raw types.

### Sealed error hierarchy

```kotlin
// In :sdk or :core:model
sealed interface DataError {
    sealed interface Mcu : DataError {
        data object NotAvailable : Mcu          // TWUtil returned null / not bound
        data object PermissionDenied : Mcu      // requires root / system UID
        data class Protocol(val code: Int) : Mcu // unexpected MCU response code
    }
    sealed interface System : DataError {
        data object ServiceNotFound : System    // OEM service not installed (carinfoservice)
        data class Intent(val action: String) : System
    }
    data class Unknown(val throwable: Throwable) : DataError
}
```

### Propagation rules

```
Data source         throws platform exceptions (SecurityException, IOException, DeadObjectException)
     ↓
Repository          catches all, remaps to DataError → returns Result<T, DataError>
     ↓
Use case (optional) receives Result<T, DataError>, may add domain logic
     ↓
ViewModel           maps Result to UiState — never handles raw exceptions
```

```kotlin
// Repository example
class McuRepository(private val twUtil: TWUtil?) {
    suspend fun getSpeed(): Result<Float, DataError> = runCatching {
        twUtil?.readSpeed() ?: throw IllegalStateException("not bound")
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e ->
            when (e) {
                is SecurityException -> Result.Error(DataError.Mcu.PermissionDenied)
                is IllegalStateException -> Result.Error(DataError.Mcu.NotAvailable)
                else -> Result.Error(DataError.Unknown(e))
            }
        }
    )
}
```

---

## 3. Flow Patterns for MCU & OEM Callbacks

### callbackFlow for callback-based OEM APIs

Use `callbackFlow` whenever wrapping a callback-based system API (TWUtil listeners,
AIDL callbacks, `IUartReceiver`).

```kotlin
fun observeMcuEvents(twUtil: TWUtil): Flow<McuEvent> = callbackFlow {
    val callback = object : TWUtil.Callback {
        override fun handleMessage(msg: Message) {
            trySend(McuEvent.from(msg))   // non-blocking; drops if full
        }
    }
    twUtil.registerCallback(callback)
    // awaitClose is mandatory — runs when the collector cancels
    awaitClose { twUtil.unregisterCallback(callback) }
}
```

`awaitClose` is not optional — omitting it leaks the callback if the collector is cancelled.

### Channel(BUFFERED) for one-shot UI effects

One-shot imperatives (navigate, show snackbar, play sound) must use `Channel`, not `SharedFlow`.
`SharedFlow(replay=0)` drops events when there is no active collector (e.g. during recomposition).
`Channel(BUFFERED)` queues them.

```kotlin
// In ViewModel
private val _effects = Channel<UiEffect>(Channel.BUFFERED)
val effects = _effects.receiveAsFlow()

fun onReverseGear() {
    viewModelScope.launch { _effects.send(UiEffect.OpenCamera) }
}
```

```kotlin
// In Screen composable
LaunchedEffect(Unit) {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.effects.collect { effect ->
            when (effect) {
                UiEffect.OpenCamera -> navigator.openReverseCamera()
            }
        }
    }
}
```

### State vs effect decision

> Does the value survive a configuration change and need to be re-rendered?
> - Yes → `UiState` field (StateFlow)
> - No → `UiEffect` (Channel)

### Flow traps to avoid

| Trap | Rule |
|---|---|
| `Channel.receiveAsFlow()` shared across collectors | Fan-out, not broadcast. Each event goes to exactly one collector. Use `SharedFlow` for multicast. |
| `tryEmit` on unbuffered `SharedFlow` | Silently drops if no active collector. Use `launch { emit() }` instead. |
| `.catch {}` swallowing `CancellationException` | `.catch` intercepts upstream errors but does not catch `CancellationException` — do not rethrow it. |
| Side effects inside `map`/`combine` lambdas | Put side effects in `onEach`, not inside transform lambdas. |
| `combine` waiting on a source that never emits | If one input never emits, the combined flow never emits. Seed with `MutableStateFlow` if needed. |
| Uncapped `retry`/`retryWhen` | Always guard the attempt count: `retryWhen { cause, attempt -> attempt < 3 }`. |

---

## 4. Modularization Visibility Rule

**Declare everything at the lowest visibility that still compiles.**

```
private  →  used only in the same file
internal →  used by another file in the same module
public   →  used by a different module (confirm a real consumer exists before widening)
```

- Public interface = module API → `public`. Its implementation class → `internal`.
- Mappers, helpers, extensions, constants, UI-state holders scoped to a module → `internal`.
- `public` is a decision, not a default. Require a real cross-module consumer.

```kotlin
// :sdk module
public interface McuDataSource {          // public: :launcher needs it
    suspend fun observeSpeed(): Flow<Float>
}

internal class McuDataSourceImpl(         // internal: :launcher never instantiates this
    private val twUtil: TWUtil
) : McuDataSource { ... }
```

---

## 5. Debugging on Real Hardware

Helm runs on a fixed physical unit (AllWinner A133, Android 10). No emulator. ADB over USB or Wi-Fi.

### Logcat scoped to Helm

```bash
# Filter to Helm's process only
adb logcat --pid=$(adb shell pidof -s com.helm.launcher)

# Filter by tag (e.g. MCU events)
adb logcat -s McuRepository:D TWUtil:D

# Combine: Helm process + twutil2 system tag
adb logcat --pid=$(adb shell pidof -s com.helm.launcher) twutil2:I *:S
```

### Crash investigation

Read the full `Caused by:` chain **bottom-up** — the root cause is the deepest `Caused by:`, not the top exception.

```bash
adb logcat -b crash    # crashes only, less noise
```

### ANR traces

```bash
adb pull /data/anr/traces.txt
```

Look for `MONITOR` state on the main thread (blocked on a lock) or blocking calls
(`read`, `write`, `Binder.transact`) on the UI thread.

### R8 / ProGuard stack decode

```bash
./gradlew :app:retrace   # if retrace task is configured
# or manually:
java -jar retrace.jar build/outputs/mapping/release/mapping.txt crash.txt
```

### Compose recomposition debugging

```kotlin
// Temporary: log which lambda/state triggers recomposition
SideEffect { Log.d("Recompose", "SpeedBadge recomposed: speed=$speed") }
```

Use Layout Inspector → Recomposition Counts tab in Android Studio for visual confirmation.
Remove all `SideEffect` debug logs before committing.

### Multi-tag session tracking

When debugging across Helm + OEM services simultaneously, tag logs with a short suffix
so you can grep and clean up:

```kotlin
private const val TAG = "HelmMcu_a4f2"   // change suffix each debug session
```
