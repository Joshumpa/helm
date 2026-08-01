## 1. SDK — Definir contrato

- [ ] 1.1 Agregar `firmwareVersion(): Flow<String>` a la interfaz `McuService` en `:sdk`
- [ ] 1.2 Implementar stub en `McuServiceStub` que emite `"—"` y completa

## 2. SDK — Implementación real [Track B]

- [ ] 2.1 En `TwUtilAdapter`, implementar `firmwareVersion()` real: enviar el comando UART de versión de firmware (revisar `docs/mcu-protocol.md` post-FEL para el byte exacto)
- [ ] 2.2 Agregar timeout + catch en `TwUtilAdapter.firmwareVersion()` → emitir `"—"` como fallback si el MCU no responde
- [ ] 2.3 Marcar con TODO el byte de comando hasta tener acceso root para verificarlo

## 3. SDK — CarSystem

- [ ] 3.1 En `CarSystem.getSystemInfo()`, poblar el campo `mcuVersion` consumiendo `McuService.firmwareVersion().first()`

## 4. UI — Pantalla About

- [ ] 4.1 En `AboutSection` de `:settings`, agregar fila "Firmware MCU" que muestra `systemInfo.mcuVersion`
- [ ] 4.2 Verificar que la fila se muestre con valor `"—"` cuando el stub está activo (no se oculta el campo)

## 5. Verificación

- [ ] 5.1 Compilar con `./gradlew assembleDebug` sin errores
- [ ] 5.2 Instalar en el head unit y confirmar que la pantalla About muestra "Firmware MCU: —" (stub)
- [ ] 5.3 [Track B] Post-FEL: verificar que muestra la versión real del firmware MCU
