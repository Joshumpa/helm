## Purpose

Manages the CarPlay / ZLINK session lifecycle in `:carplay`, replacing the stub by reacting to MCU session-state events and audio source switches.

## ADDED Requirements

### Requirement: Session activation from MCU
The system SHALL transition the CarPlay screen to an active session state when MCU code `0x0205` (arg1≠0) is received, and return to idle when arg1=0 is received.

#### Scenario: MCU signals session start
- **WHEN** MCU code `0x0205` with arg1=1 is received
- **THEN** the CarPlay UI SHALL display the active session view and route audio to the CarPlay source

#### Scenario: MCU signals session end
- **WHEN** MCU code `0x0205` with arg1=0 is received
- **THEN** the CarPlay UI SHALL return to the idle/waiting screen and audio source SHALL revert to the previous source

### Requirement: Audio source coordination
When CarPlay is active, the system SHALL set the MCU audio source to CarPlay (source ID 5 via code `0x0301`) and restore the previous source when the session ends.

#### Scenario: Audio source set on CarPlay start
- **WHEN** a CarPlay session starts
- **THEN** the SDK SHALL send `write(769, 192, 5)` to the MCU within 500 ms

#### Scenario: Audio restored on CarPlay end
- **WHEN** a CarPlay session ends
- **THEN** the SDK SHALL restore the previous MCU audio source

### Requirement: Idle state UI
When no CarPlay session is active, the `:carplay` screen SHALL display a "Connect iPhone" prompt and the connection state (Disconnected / Waiting / Active).

#### Scenario: No USB device connected
- **WHEN** the CarPlay screen is opened with no active MCU session
- **THEN** the UI SHALL show "Connect iPhone via USB" and a Disconnected indicator

### Requirement: Stub mode without MCU
Without MCU connection the CarPlay module SHALL compile, open without crash, and display a "Unavailable (Track B)" screen.

#### Scenario: CarPlay screen opened in stub mode
- **WHEN** the screen is opened without root/MCU
- **THEN** the unavailability screen is shown and the module does not crash
