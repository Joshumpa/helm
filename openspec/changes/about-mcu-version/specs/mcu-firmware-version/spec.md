## Purpose

Expone la versión de firmware del MCU del head unit como dato legible desde la capa SDK, y la muestra al usuario en la pantalla About de Settings para facilitar el diagnóstico y soporte técnico.

## ADDED Requirements

### Requirement: Versión de firmware MCU disponible en SDK

El sistema SHALL exponer la versión de firmware del MCU a través de la capa SDK como un valor observable.

#### Scenario: Root disponible (Track B activo)

- **WHEN** Helm está instalado como system app y puede comunicarse con `com.tw.uart`
- **THEN** `McuService` emite la cadena de versión de firmware real del MCU

#### Scenario: Root no disponible (stub activo)

- **WHEN** Helm no tiene privilegios de system app (Track B pendiente)
- **THEN** `McuService` emite `"—"` como valor stub, sin lanzar error

### Requirement: Versión MCU visible en pantalla About

La pantalla About en `:settings` SHALL mostrar la versión de firmware del MCU junto a los demás datos del dispositivo.

#### Scenario: Versión disponible

- **WHEN** el usuario abre la pantalla About
- **THEN** se muestra un campo "Firmware MCU" con la versión emitida por `McuService`

#### Scenario: Versión stub

- **WHEN** el sistema devuelve `"—"` como valor stub
- **THEN** el campo "Firmware MCU" muestra `"—"` (no se oculta el campo)

### Requirement: Sin degradación al fallar la consulta

El sistema MUST NO lanzar excepciones no capturadas ni crashear la pantalla About si la consulta de versión MCU falla.

#### Scenario: Error de comunicación MCU

- **WHEN** ocurre un error al consultar la versión del MCU
- **THEN** el sistema emite `"—"` como fallback y la pantalla About se muestra normalmente
