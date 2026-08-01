## Context

El head unit AllWinner A133 (ceres-b6) tiene secure boot desactivado (`sunxi_secure: normal`) y SELinux en modo permissive. El puerto USB-A verde es host-only en modo Android normal, pero en FEL mode el SoC fuerza el USB a device mode, presentando VID:PID `1f3a:efe8`. Esto permite conectar laptop (host) → head unit (device) con un cable USB-A macho a USB-A macho. La partición boot no está firmada (no hay verified boot activo).

## Goals / Non-Goals

**Goals:**
- Poner el head unit en FEL mode con un comando desde TermOne Plus
- Instalar el driver correcto en Windows para que sunxi-fel reconozca el dispositivo
- Hacer dump del boot partition, parcharlo con Magisk, y flashearlo de vuelta
- Habilitar ADB WiFi persistente para desarrollo continuo

**Non-Goals:**
- Desbloqueo de bootloader (no hay fastboot en este firmware)
- Modificar particiones del sistema más allá de boot
- Automatizar el proceso (es un procedimiento manual de una sola vez)

## Decisions

### Cable: USB-A macho a USB-A macho
En FEL mode el SoC fuerza device mode en el puerto USB sin importar su configuración normal. Un cable USB-A a USB-A es suficiente y es el único tipo de cable que puede conectar dos puertos USB-A.

### Trigger: `reboot efex`
Comando estándar de AllWinner para entrar en FEL mode desde Android shell. Funciona sin root en builds con SELinux permissive. Alternativa si falla: escribir directamente al sysfs `sunxi-efex`.

### Driver Windows: Zadig + WinUSB
`sunxi-fel` en Windows requiere WinUSB en lugar del driver genérico de Windows. Zadig es la herramienta estándar para asignar WinUSB a VID:PID `1f3a:efe8`. Alternativa descartada: LibUSB-win32 (más lento, más complejo de instalar).

### Root: Magisk via sunxi-fel write
Opciones consideradas:
- **Magisk + sunxi-fel** ✓ — flashea solo el boot partition, no toca el sistema. Reversible.
- **PhoenixSuit / LiveSuit** ✗ — reflashea firmware completo, riesgo alto, requiere imagen completa del OEM.
- **TWRP** ✗ — no hay recovery compilado para ceres-b6; compilarlo requiere código fuente del kernel AllWinner que no está disponible públicamente para este board.

### ADB WiFi persistente: `persist.adb.tcp.port`
Escribir `persist.adb.tcp.port=5555` vía `setprop` con root hace el cambio permanente entre reinicios sin necesidad de scripts init.d.

## Risks / Trade-offs

- **Offset incorrecto del boot partition** → `sunxi-fel` podría escribir en la partición equivocada y brickear el dispositivo. Mitigación: leer la tabla de particiones con `sunxi-fel read` primero y verificar el magic del boot.img (`ANDROID!`) antes de flashear.
- **`reboot efex` no disponible** → Algunos builds AllWinner no incluyen el handler. Mitigación: alternativa vía sysfs documentada en el spec.
- **Magisk incompatible con Android 10 / API 29** → Magisk v26+ soporta Android 10. Verificar versión antes de parchear.
- **Cable USB-A a USB-A** → No es un cable estándar; el usuario debe conseguirlo o usar un adaptador USB-A hembra a USB-A hembra con dos cables normales.

## Migration Plan

1. Ejecutar procedimiento una sola vez en el head unit
2. Verificar root con `su -c "id"` en TermOne Plus → debe mostrar `uid=0`
3. Habilitar ADB WiFi
4. Conectar via `adb connect` desde laptop para confirmar
5. Proceder con Track B on-device tests (8.4, 8.5, 8.6)

**Rollback:** Si el boot parchado falla, re-entrar en FEL mode y flashear el `boot.img` original (guardado en el paso de dump).

## Open Questions

- ¿Cuál es el offset exacto del boot partition en este firmware? Se determina en el momento con `sunxi-fel read` leyendo la tabla de particiones (GPT/MBR en los primeros sectores).
- ¿`reboot efex` requiere algún permiso especial en este build específico? Se verifica en el primer intento; si falla, se usa la alternativa sysfs.
