## Why

Todo Track B (radio, cámara reversa, datos reales del MCU, CarPlay) está implementado pero bloqueado: el head unit corre como app normal sin acceso a `com.tw.uart` ni a las APIs `@hide` del sistema. FEL mode es el camino confirmado para obtener root en este dispositivo AllWinner A133 (secure boot desactivado, VID:PID `1f3a:efe8` confirmado). Con root se puede instalar Helm como app de sistema y habilitar ADB permanente sobre WiFi.

## What Changes

- Se define y documenta el procedimiento para poner el head unit en FEL mode vía comando en TermOne Plus (`reboot efex`)
- Se establece el cable requerido: USB-A macho a USB-A macho (laptop host → head unit en modo device durante FEL)
- Se especifican las herramientas de laptop necesarias: `sunxi-tools` (sunxi-fel) + ADB Platform Tools + driver Zadig (Windows)
- Se documenta el proceso de flashing de Magisk al boot partition para obtener root permanente
- Se documenta cómo habilitar ADB WiFi persistente post-root para desarrollo continuo

## Capabilities

### New Capabilities

- `fel-mode-trigger`: Procedimiento para poner el head unit en FEL mode desde TermOne Plus y verificar la conexión desde la laptop

### Modified Capabilities

_(ninguna — no cambia ningún spec de software existente)_

## Impact

- No modifica código de Helm
- Habilita todo Track B post-procedimiento: on-device tests 8.4, 8.5, 8.6
- Habilita ADB permanente para deploy directo del APK desde Android Studio / Gradle
- Prerequisito para instalar Helm como system app (OTA silenciosa futura)

## Non-goals

- No incluye modificar particiones del sistema más allá del boot partition (Magisk)
- No incluye desbloquear el bootloader de forma permanente (el head unit no tiene fastboot expuesto)
- No incluye restaurar el firmware OEM (reversible vía FEL si algo falla)
