## Purpose

Exposes real-time vehicle data flows — speed, day/night, door state, ambient temperature, and AC/climate — sourced from MCU events and made available to all Helm feature modules via the SDK.

## ADDED Requirements

### Requirement: Vehicle speed flow
The SDK SHALL provide a `Flow<Int>` that emits the current vehicle speed in km/h, sourced from the privileged TWUtil vehicle data path. In stub mode it SHALL emit 0.

#### Scenario: Speed data received
- **WHEN** a speed update arrives from the MCU
- **THEN** `McuService.speed()` SHALL emit the new value in km/h within one MCU polling cycle

#### Scenario: Stub mode speed
- **WHEN** stub mode is active
- **THEN** `McuService.speed()` SHALL emit a constant value of 0

### Requirement: Day/night mode flow
The SDK SHALL provide a `Flow<DayNight>` driven by MCU code `0x0204` (arg1=1 Night, arg1=0 Day).

#### Scenario: MCU signals night mode
- **WHEN** MCU code `0x0204` with arg1=1 is received
- **THEN** `McuService.dayNight()` SHALL emit `DayNight.Night`

#### Scenario: MCU signals day mode
- **WHEN** MCU code `0x0204` with arg1=0 is received
- **THEN** `McuService.dayNight()` SHALL emit `DayNight.Day`

### Requirement: Automatic theme switching from day/night
The theme system SHALL observe `McuService.dayNight()` and switch between the theme's day and night variants automatically, without requiring user interaction.

#### Scenario: Night mode triggers dark theme
- **WHEN** `DayNight.Night` is emitted
- **THEN** the active theme SHALL switch to its night variant within one recomposition frame

### Requirement: Door state flow
The SDK SHALL provide a `Flow<DoorState>` driven by MCU code `0x0505`, encoding open/closed status for driver, passenger, rear doors, and trunk.

#### Scenario: Door open event received
- **WHEN** MCU code `0x0505` indicates one or more doors are open
- **THEN** `McuService.doorState()` SHALL emit a `DoorState` with the correct door flags set

### Requirement: Ambient temperature flow
The SDK SHALL provide a `Flow<Int?>` driven by MCU code `0x0508` (obj[1] raw °C, 127 = invalid). When value is 127, the flow SHALL emit `null`.

#### Scenario: Valid temperature received
- **WHEN** MCU code `0x0508` with obj[1]=22 is received
- **THEN** `McuService.ambientTemperature()` SHALL emit 22

#### Scenario: Invalid temperature sentinel
- **WHEN** MCU code `0x0508` with obj[1]=127 is received
- **THEN** `McuService.ambientTemperature()` SHALL emit `null`

### Requirement: Power-off countdown
The SDK SHALL handle MCU code `0x9F10` (power-off countdown) by requesting a graceful Android shutdown when the countdown expires.

#### Scenario: MCU power-off countdown received
- **WHEN** MCU code `0x9F10` with arg2=ms before shutdown is received
- **THEN** the system SHALL schedule `REQUEST_SHUTDOWN` and send the MCU acknowledgement (`write(40720, 0, 3000)`) within that interval

### Requirement: MCU time sync on boot
The SDK SHALL handle MCU code `0x0518` (7-byte RTC packet) by setting the Android system clock and SHALL push time back to the MCU RTC on every `TIME_TICK`/`TIME_SET` event via code `0x0107`.

#### Scenario: RTC sync on boot
- **WHEN** MCU code `0x0518` is received during startup
- **THEN** the system clock SHALL be set to the MCU's time within 1 second
