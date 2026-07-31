## Purpose

Delivers FM and AM radio playback in `:radio` by driving the QN8035 tuner chip via MCU UART commands, replacing the existing stub with a real implementation.

## Requirements

### Requirement: FM band playback
The system SHALL tune to any FM frequency between 87.5 MHz and 108.0 MHz in 0.1 MHz steps and play audio through the vehicle's audio output.

#### Scenario: User selects FM frequency
- **WHEN** the user selects a frequency within the FM band
- **THEN** the tuner SHALL lock to that frequency and audio playback SHALL begin within 1 second

#### Scenario: Frequency out of FM range
- **WHEN** the user requests a frequency outside 87.5–108.0 MHz
- **THEN** the system SHALL reject the request and retain the current frequency

### Requirement: AM band playback
The system SHALL tune to any AM frequency between 522 kHz and 1620 kHz in 9 kHz steps.

#### Scenario: User switches to AM
- **WHEN** the user activates AM mode
- **THEN** the tuner SHALL switch band and resume from the last saved AM frequency

### Requirement: Station seek
The system SHALL provide forward and backward seek that steps through frequencies until a station with sufficient signal strength is found or the band wraps.

#### Scenario: Seek finds a station
- **WHEN** the user initiates seek
- **THEN** the tuner SHALL stop at the first frequency with a decodable signal and display the new frequency

#### Scenario: No station found on seek
- **WHEN** seek completes a full band scan without finding a signal
- **THEN** the tuner SHALL return to the starting frequency and indicate no station was found

### Requirement: RDS station name display
When the tuned FM station broadcasts RDS data, the system SHALL display the Programme Service (PS) name.

#### Scenario: RDS PS name received
- **WHEN** the tuner locks onto an FM station broadcasting RDS
- **THEN** the station name (up to 8 characters) SHALL be displayed in the radio UI

#### Scenario: No RDS data
- **WHEN** the tuned station does not broadcast RDS
- **THEN** the UI SHALL display the frequency and no station name field

### Requirement: Preset stations
The system SHALL allow the user to save up to 12 preset stations (across both bands) and recall them with a single tap.

#### Scenario: User saves a preset
- **WHEN** the user long-presses a preset slot while a station is tuned
- **THEN** that station and band SHALL be stored in the slot and survive app restart

### Requirement: Stub mode when MCU unavailable
When running without root or MCU connection, the radio SHALL operate in stub mode with a fixed simulated station, no audio, and a visible "Unavailable (Track B)" indicator.

#### Scenario: Radio UI opened without root
- **WHEN** the radio screen is opened in stub mode
- **THEN** the UI SHALL render correctly, show the unavailability indicator, and not crash
