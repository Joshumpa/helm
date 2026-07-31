## Purpose

Displays the vehicle's reverse camera feed and parking radar overlay when the driver engages reverse gear, driven entirely by MCU signal codes.

## ADDED Requirements

### Requirement: Automatic camera activation on reverse gear
The system SHALL display the full-screen reverse camera view automatically within 500 ms of receiving the MCU reverse-gear signal (`0x020B` arg1=1), without any user interaction.

#### Scenario: Driver engages reverse
- **WHEN** MCU code `0x020B` with arg1=1 is received
- **THEN** the reverse camera screen SHALL appear in full-screen portrait, covering all other content

#### Scenario: Driver exits reverse
- **WHEN** MCU code `0x020B` with arg1=0 is received
- **THEN** the camera view SHALL dismiss and the previous screen SHALL be restored

### Requirement: Camera stream display
The system SHALL render the live camera stream at the resolution reported by the MCU (`0x0506`) scaled to fill the portrait display without distortion.

#### Scenario: Stream starts at correct resolution
- **WHEN** the camera view is active and MCU code `0x0506` provides resolution data
- **THEN** the `SurfaceView` SHALL scale to match the reported width × height proportions

#### Scenario: Stream signal goes away unexpectedly
- **WHEN** MCU code `0x9F1C` with arg1=0 is received while the camera view is shown
- **THEN** the system SHALL display a "No signal" placeholder and keep the camera screen visible until the gear changes

### Requirement: Parking radar overlay
The system SHALL display an 8-sensor radar overlay on top of the camera feed using distance data from MCU code `0x0507` (bytes 0–3 front, bytes 5–8 rear).

#### Scenario: Sensor distances received
- **WHEN** MCU code `0x0507` data arrives while the camera view is active
- **THEN** the overlay SHALL update each sensor arc/indicator within one rendering frame to reflect current distances

#### Scenario: Sensor indicates collision risk
- **WHEN** any sensor distance falls below a critical threshold
- **THEN** that sensor indicator SHALL turn red and an audible alert SHALL play

### Requirement: MCU camera acknowledgement
When the reverse camera view becomes visible, the system SHALL send acknowledgement code `0x0304` (write(772,1)) to the MCU. When the view is dismissed, it SHALL send write(772,0).

#### Scenario: Ack sent on camera display
- **WHEN** the reverse camera view is shown
- **THEN** the SDK SHALL transmit `write(772,1)` to the MCU within 200 ms

### Requirement: Stub mode without MCU
Without MCU connection the reverse camera feature SHALL not crash and SHALL display a static placeholder image with a "Unavailable (Track B)" label.

#### Scenario: Reverse camera viewed in stub mode
- **WHEN** the feature is triggered in stub mode (e.g., via debug shortcut)
- **THEN** a static placeholder is shown and no exception is thrown
