## Purpose

Keeps Helm's audio state synchronized with steering-wheel volume controls and MCU-initiated audio source switches, ensuring user actions on physical controls are reflected immediately in the Android audio subsystem.

## ADDED Requirements

### Requirement: Steering-wheel volume synchronisation
The SDK SHALL observe MCU code `0x0203` (steering-wheel volume button) and apply the reported volume level to Android `AudioManager` stream 5 (STREAM_MUSIC). If the mute flag (`arg1 & 0x80000000`) is set, the stream SHALL be muted.

#### Scenario: Steering-wheel volume-up pressed
- **WHEN** MCU code `0x0203` with an increased level value is received
- **THEN** the Android media volume SHALL update to match the reported level within one UI frame

#### Scenario: Steering-wheel mute pressed
- **WHEN** MCU code `0x0203` with the mute flag set is received
- **THEN** `AudioManager` stream 5 SHALL be muted

#### Scenario: Steering-wheel unmute
- **WHEN** MCU code `0x0203` with the mute flag cleared is received
- **THEN** `AudioManager` stream 5 SHALL be unmuted and volume restored to the reported level

### Requirement: MCU-initiated volume levels from code 0x0106
The SDK SHALL observe MCU code `0x0106` (obj[0]=media volume) and apply the media volume level, keeping the Android volume state consistent with the MCU's view.

#### Scenario: MCU pushes a volume level
- **WHEN** MCU code `0x0106` is received
- **THEN** `AudioManager` stream 5 volume SHALL be set to obj[0] within one UI frame

### Requirement: Audio source change notification
The SDK SHALL observe MCU code `0x0301` (audio source change) and surface the new source ID as a `Flow<AudioSource>` for feature modules to react to (e.g., pausing music when radio source is selected).

#### Scenario: MCU switches to radio source
- **WHEN** MCU code `0x0301` with source ID 7 (radio) is received
- **THEN** `McuService.audioSource()` SHALL emit `AudioSource.Radio` and the music player SHALL pause

#### Scenario: MCU switches to USB music source
- **WHEN** MCU code `0x0301` with source ID 3 is received
- **THEN** `McuService.audioSource()` SHALL emit `AudioSource.UsbMusic`

### Requirement: Phone call audio routing
The SDK SHALL observe MCU code `0x0302` (BT phone call state): arg1≠0 means call active (mute media + switch to call audio); arg1=0 means call ended (unmute + restore source).

#### Scenario: Incoming call starts
- **WHEN** MCU code `0x0302` with arg1=1 is received
- **THEN** media playback SHALL pause, STREAM_MUSIC SHALL be muted, and call audio SHALL be active

#### Scenario: Call ends
- **WHEN** MCU code `0x0302` with arg1=0 is received
- **THEN** STREAM_MUSIC SHALL be unmuted and the previous audio source SHALL be restored

### Requirement: Hardware mute signal
The SDK SHALL observe MCU code `0x9F1B` (hardware mute signal) and apply or release mute on `AudioManager` stream 3 accordingly.

#### Scenario: Hardware mute activated
- **WHEN** MCU code `0x9F1B` with arg1≠0 is received
- **THEN** `AudioManager` stream 3 SHALL be muted

#### Scenario: Hardware mute released
- **WHEN** MCU code `0x9F1B` with arg1=0 is received
- **THEN** `AudioManager` stream 3 SHALL be unmuted
