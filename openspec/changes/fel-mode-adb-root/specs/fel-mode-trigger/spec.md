## Purpose

Define los requisitos del procedimiento para poner el head unit AllWinner A133 en FEL mode desde TermOne Plus, verificar la conexión desde la laptop y obtener root permanente vía Magisk.

## ADDED Requirements

### Requirement: Trigger FEL mode desde TermOne Plus
El head unit SHALL entrar en FEL mode (USB Download mode de AllWinner) al ejecutar `reboot efex` en TermOne Plus, sin requerir root previo.

#### Scenario: Comando exitoso
- **WHEN** el usuario ejecuta `reboot efex` en TermOne Plus
- **THEN** el head unit se reinicia y presenta VID:PID `1f3a:efe8` en el bus USB de la laptop

#### Scenario: Comando falla por permisos
- **WHEN** `reboot efex` responde "Permission denied"
- **THEN** se intenta la alternativa: `echo 'efex' > /sys/class/misc/sunxi-efex/efex_flag && reboot`

### Requirement: Detección USB desde laptop
La laptop SHALL detectar el head unit como dispositivo USB con VID:PID `1f3a:efe8` en modo FEL.

#### Scenario: Detección en Windows
- **WHEN** el head unit está en FEL mode y conectado vía USB-A a USB-A
- **THEN** el Device Manager de Windows muestra el dispositivo como "SUNXI USB Device" o similar, y Zadig puede asignarle el driver WinUSB

#### Scenario: Verificación con sunxi-fel
- **WHEN** el driver está instalado y se ejecuta `sunxi-fel version`
- **THEN** la salida incluye el SoC identificado (e.g., `AWUSBFEX soc=00001800`)

### Requirement: Root permanente vía Magisk
El head unit SHALL tener acceso root permanente tras flashear un boot.img parchado con Magisk vía sunxi-fel.

#### Scenario: Dump del boot partition
- **WHEN** el dispositivo está en FEL mode y se ejecuta `sunxi-fel read` con el offset del boot partition
- **THEN** se obtiene un archivo `boot.img` válido del dispositivo

#### Scenario: Flash de boot parchado
- **WHEN** Magisk ha parchado `boot.img` y se ejecuta `sunxi-fel write` con el offset correcto
- **THEN** el head unit arranca normalmente y la app Magisk reporta root activo

### Requirement: ADB WiFi persistente post-root
Con root activo, el head unit SHALL exponer ADB sobre WiFi de forma persistente entre reinicios.

#### Scenario: Habilitación post-root
- **WHEN** se ejecuta `su -c "setprop service.adb.tcp.port 5555 && stop adbd && start adbd"` en TermOne Plus
- **THEN** la laptop puede conectarse con `adb connect <ip>:5555`

#### Scenario: Persistencia entre reinicios
- **WHEN** el head unit se reinicia
- **THEN** ADB WiFi sigue activo sin ejecutar comandos manuales (vía `persist.adb.tcp.port` o init.d script)
