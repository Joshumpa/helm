## Context

Ver `proposal.md — Why`. `CarSystem.getSystemInfo()` ya existe y devuelve un `SystemInfo` data class con campo `mcuVersion: String` que actualmente es vacío o tiene un TODO. El stack MCU está documentado en `docs/mcu-protocol.md`. Track B: el bind a `com.tw.uart` requiere system app (pendiente FEL/root).

## Goals / Non-Goals

**Goals:**
- Definir `McuService.firmwareVersion(): Flow<String>` en el SDK
- Implementar stub que devuelve `"—"` (compila y corre sin root)
- Implementar `TwUtilAdapter` real que consulta versión de firmware vía UART (se activa post-FEL)
- Mostrar el valor en `AboutSection` de `:settings`

**Non-Goals:**
- No se parsea ni valida el formato de la cadena de versión
- No se actualiza la versión en tiempo real durante la sesión (solo al arrancar)

## Decisions

**Stub devuelve `"—"` (guion largo), no cadena vacía**
- Una cadena vacía podría interpretarse como error; `"—"` comunica explícitamente "no disponible"
- Alternativa considerada: ocultar el campo si es stub — descartado porque el campo debe existir para no confundir al usuario post-FEL cuando aparezca

**Consulta de versión: una sola vez al arrancar**
- La versión del firmware MCU no cambia en tiempo de ejecución; no tiene sentido un Flow continuo
- Se expone como `Flow<String>` para consistencia con el resto del SDK, pero emite un solo valor y completa
- Alternativa: `suspend fun firmwareVersion(): String` — más honesto, pero rompe el patrón del SDK

**Ubicación de la consulta real: `TwUtilAdapter`**
- `TwUtilAdapter` ya gestiona el bind a `com.tw.uart` y el envío de comandos UART
- Agregar aquí la consulta de versión mantiene toda la comunicación MCU en un solo lugar
- El comando exacto de versión de firmware se determinará al leer el protocolo MCU con acceso root

## Risks / Trade-offs

- **[Risk] Comando de versión MCU desconocido** → Mitigation: se deja TODO en `TwUtilAdapter` real; el stub cubre la UI hasta tener acceso para investigar
- **[Risk] El MCU no responde al comando de versión** → Mitigation: timeout + catch → emite `"—"` como fallback

## Open Questions

- ¿Cuál es el byte command exacto para solicitar la versión de firmware al MCU? (No bloquea implementación del stub ni la UI; se resuelve post-FEL consultando `docs/mcu-protocol.md` o capturando tráfico UART)
