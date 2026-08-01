## 1. Preparar laptop

- [ ] 1.1 Descargar e instalar Android SDK Platform Tools en la laptop (`adb.exe`)
- [ ] 1.2 Descargar `sunxi-tools` para Windows (binarios precompilados de sunxi-tools releases) — incluye `sunxi-fel.exe`
- [ ] 1.3 Descargar e instalar Zadig (zadig.akeo.ie) para asignar driver WinUSB al dispositivo en FEL mode
- [ ] 1.4 Conseguir cable USB-A macho a USB-A macho (o adaptador USB-A hembra a USB-A hembra)
- [ ] 1.5 Descargar Magisk APK (v26+) desde GitHub releases — se necesita en el head unit para parchear boot.img

## 2. Entrar en FEL mode

- [ ] 2.1 Instalar Magisk APK en el head unit vía TermOne Plus: `pm install /sdcard/Magisk-*.apk` (copiarlo antes a USB y luego a /sdcard/)
- [ ] 2.2 En TermOne Plus, intentar: `reboot efex`
- [ ] 2.3 Si 2.2 falla con "Permission denied", intentar alternativa: `echo 'efex' > /sys/class/misc/sunxi-efex/efex_flag && reboot`
- [ ] 2.4 Conectar el cable USB-A a USB-A entre laptop y head unit inmediatamente tras ejecutar el comando

## 3. Instalar driver y verificar FEL

- [ ] 3.1 Abrir Zadig en la laptop → el dispositivo debe aparecer como "SUNXI USB Device" (VID `1f3a`, PID `efe8`)
- [ ] 3.2 En Zadig seleccionar el dispositivo y asignarle driver **WinUSB** → "Install Driver"
- [ ] 3.3 Verificar con: `sunxi-fel.exe version` → debe mostrar `AWUSBFEX soc=00001800` o similar

## 4. Dump del boot partition

- [ ] 4.1 Leer los primeros 512 bytes del dispositivo para localizar tabla de particiones: `sunxi-fel.exe read 0x0 512 mbr.bin`
- [ ] 4.2 Identificar el offset del boot partition en la tabla (buscar `boot` o `bootimg` en la salida o con un hex editor)
- [ ] 4.3 Hacer dump del boot partition completo: `sunxi-fel.exe read <offset> <size> boot.img` — guardar como backup
- [ ] 4.4 Verificar que el dump es válido: los primeros 8 bytes deben ser `ANDROID!`

## 5. Parchear con Magisk y flashear

- [ ] 5.1 Copiar `boot.img` al head unit (USB o via sunxi-fel memory): copiarlo a `/sdcard/` para que Magisk lo pueda leer
- [ ] 5.2 En la app Magisk del head unit → "Install" → "Select and Patch a File" → seleccionar `boot.img` → guardar `magisk_patched_*.img`
- [ ] 5.3 Recuperar el `magisk_patched_*.img` del head unit a la laptop
- [ ] 5.4 Poner el head unit en FEL mode nuevamente (repetir tarea 2)
- [ ] 5.5 Flashear el boot parchado: `sunxi-fel.exe write <offset> magisk_patched.img`
- [ ] 5.6 Reiniciar el head unit: `sunxi-fel.exe exec 0x0` o desconectar USB y reconectar la batería/energía

## 6. Verificar root y habilitar ADB WiFi

- [ ] 6.1 En TermOne Plus, verificar root: `su -c "id"` → debe mostrar `uid=0(root)`
- [ ] 6.2 Completar setup de Magisk en la app (puede pedir segundo reinicio)
- [ ] 6.3 Habilitar ADB WiFi persistente: `su -c "setprop persist.adb.tcp.port 5555 && stop adbd && start adbd"`
- [ ] 6.4 En la laptop: `adb connect <ip-del-head-unit>:5555` → confirmar `connected`
- [ ] 6.5 Verificar ADB con root: `adb shell su -c "id"` → `uid=0(root)`
- [ ] 6.6 Ejecutar Track B on-device test 8.4: confirmar que `TwUtilMcuDataSource` se bindea y llega heartbeat `0x0202`
