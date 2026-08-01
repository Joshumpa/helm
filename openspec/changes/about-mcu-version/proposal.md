## Why

La pantalla About en `:settings` tiene un campo `mcuVersion` vacío — el firmware del MCU es información crítica para diagnóstico y soporte. Sin ella, no es posible saber qué versión de firmware corre el head unit desde la app.

## What Changes

- `McuService` expone `firmwareVersion(): Flow<String>` que lee la respuesta del MCU al comando de versión (código MCU existente en el protocolo).
- `CarSystem.getSystemInfo()` rellena `mcuVersion` desde `McuService.firmwareVersion()`.
- La pantalla About muestra la versión MCU junto a los datos de OS y modelo de dispositivo ya presentes.
- Mientras no haya root (Track B pendiente), la implementación stub devuelve `"—"` (guion largo) en lugar de cadena vacía.

## Capabilities

### New Capabilities

- `mcu-firmware-version`: El sistema expone la versión de firmware del MCU como dato accesible desde la capa SDK, y la pantalla About la muestra al usuario.

### Modified Capabilities

_(ninguna — no hay specs existentes de about-screen; `mcu-service` spec existe pero no cubre versión de firmware)_

## Impact

- **:sdk**: `McuService` gana `firmwareVersion(): Flow<String>`; `TwUtilAdapter` implementa la llamada real; stub devuelve `"—"`.
- **:settings**: `AboutSection` / `SettingsViewModel` consume `CarSystem.getSystemInfo().mcuVersion`.
- **Dependencias**: ninguna nueva.
- **Track**: B — requiere `com.tw.uart` (system app, pendiente FEL/root). Compila y muestra stub hasta obtener root.

## Non-goals

- No se implementa actualización OTA del firmware MCU.
- No se exponen otros metadatos del MCU (número de serie, fecha de fabricación).
- No se valida ni parsea la cadena de versión — se muestra tal cual la devuelve el MCU.
