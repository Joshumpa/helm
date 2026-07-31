## Purpose

Provides the core binding layer between Helm and the vehicle's MCU hardware via `android.tw.john.TWUtil`, including lifecycle management, subscription, error propagation, and a stub/real toggle for pre-root development.

## Requirements

### Requirement: MCU binding lifecycle
The SDK SHALL establish and maintain a connection to `TWUtil` on startup when running as a system app, subscribing to the full set of known message codes. On connection loss or unexpected termination the SDK SHALL attempt reconnection with exponential back-off and surface a `DataError.Unavailable` on all dependent flows until the connection is restored.

#### Scenario: Successful bind on system app start
- **WHEN** Helm runs with `android.uid.system` and `TWUtil` is available
- **THEN** the MCU service SHALL reach `Connected` state and begin emitting events within 2 seconds of process start

#### Scenario: Connection lost mid-session
- **WHEN** the TWUtil connection drops unexpectedly
- **THEN** all MCU-sourced flows SHALL emit `DataError.Unavailable` and the service SHALL schedule a reconnection attempt

#### Scenario: Running without root (Track A / stub mode)
- **WHEN** Helm runs without `android.uid.system`
- **THEN** all MCU flows SHALL emit stub/default values and no crash or ANR SHALL occur

### Requirement: Outbound command API
The SDK SHALL expose a command API that allows feature modules to send structured messages to the MCU without depending on `TWUtil` directly. Commands that fail to send SHALL propagate a `DataError.WriteFailure`.

#### Scenario: Feature module sends a command
- **WHEN** a feature module calls the SDK's outbound command API
- **THEN** the SDK SHALL encode and transmit the UART frame to the MCU and return success or a typed error

#### Scenario: MCU not connected when command sent
- **WHEN** a command is issued while the MCU service is not connected
- **THEN** the SDK SHALL return `DataError.Unavailable` immediately without queuing

### Requirement: Stub/real data source switching
The SDK SHALL support compile-time or runtime selection between a real TWUtil data source and a stub data source that emits deterministic test values. Feature modules SHALL be unaware of which source is active.

#### Scenario: Stub mode emits predictable values
- **WHEN** stub mode is active
- **THEN** speed SHALL emit 0 km/h, day/night SHALL emit Day, and all other flows SHALL emit safe neutral defaults

### Requirement: Sealed error propagation
All SDK flows backed by MCU data SHALL propagate errors as sealed `DataError` subtypes (`Unavailable`, `ParseFailure`, `WriteFailure`) rather than throwing exceptions.

#### Scenario: Malformed MCU frame received
- **WHEN** a TWUtil message with an unexpected payload length or invalid checksum is received
- **THEN** the affected flow SHALL emit `DataError.ParseFailure` for that event and continue processing subsequent messages
