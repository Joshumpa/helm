# Auditoría de Seguridad y Calidad — Helm Android

**Fecha:** 2026-07-29  
**Cobertura:** 11 módulos Gradle, 2 workflows CI/CD, ~200 archivos analizados  
**Metodología:** Revisión estática de código, manifiestos, configuraciones de build y workflows

---

## Resumen Ejecutivo

| Severidad | Cantidad |
|-----------|----------|
| ~~CRÍTICO~~ | ~~8~~ → **0 (todos resueltos)** |
| ALTO      | 10       |
| MEDIO     | 7        |
| BAJO      | 5        |

---

## ALTOS

### A-10 · Sin Certificate Pinning para `lrclib.net`
**Archivo:** `launcher/media/LyricsRepository.kt:20`

`HttpURLConnection` usa el almacén de certificados del sistema sin pinning. Una CA comprometida puede servir letras con contenido inesperado.

---

### A-11 · `ACCESS_BACKGROUND_LOCATION` sin restricción temporal ni justificación en UI
**Archivo:** `app/src/main/AndroidManifest.xml:22`

El permiso permite rastreo de ubicación continuo en background. No hay onboarding que explique por qué es necesario ni lógica que lo desactive cuando no se use navegación.

---

## MEDIOS

### M-8 · Stubs de `:radio` y `:carplay` exponen arquitectura interna en la UI
**Archivos:** `radio/src/main/kotlin/dev/helm/radio/RadioScreen.kt:68–78`  
`carplay/src/main/kotlin/dev/helm/carplay/CarPlayScreen.kt:68–78`

Los strings mencionan "FEL mode", "root" y "acceso al hardware MCU". Un auditor externo puede inferir el vector de root, el SoC (Allwinner) y el roadmap de implementación.

---

## BAJOS

### B-1 · `READ_LOGS` con `tools:ignore="ProtectedPermissions"`
**Archivo:** `app/src/main/AndroidManifest.xml:41–43`

La supresión global oculta futuras advertencias legítimas en el mismo archivo. El permiso requiere instalación como app de sistema — está documentado pero el ignore es demasiado amplio.

---

### B-2 · `callbackFlow` futuro para MCU sin `awaitClose` documentado
**Archivo:** `sdk/src/main/kotlin/dev/helm/sdk/McuDataSource.kt:7–10`

La interfaz está vacía pero la documentación anticipa `callbackFlow` para callbacks UART. Sin un template con `awaitClose { unregisterCallback(...) }`, implementaciones futuras probablemente omitirán la limpieza.

---

### B-5 · DataStore de tema sin cifrado
**Archivo:** `themes/src/main/kotlin/dev/helm/themes/ThemeRepository.kt:14–18`

Solo almacena el nombre del tema (enum), no es sensible ahora. Documentar explícitamente que esta store es sin cifrado para evitar que futuros datos sensibles se guarden en ella.

---

### B-6 · GitHub Actions: contraseñas de keystore sin enmascarar explícitamente
**Archivo:** `.github/workflows/release.yml:43–47`

`KEYSTORE_PASSWORD` y `KEY_PASSWORD` se pasan como env vars a Gradle. Si Gradle imprime el entorno en un error, los valores podrían quedar en los logs del runner.

**Fix:** Añadir `echo "::add-mask::$KEYSTORE_PASSWORD"` y `echo "::add-mask::$KEY_PASSWORD"` antes del paso de build.

---

### B-7 · Sin validación de `Content-Length` vs bytes leídos en descarga OTA
**Archivo:** `ota/src/main/kotlin/dev/helm/ota/OtaRepository.kt:64–70`

Si la conexión se interrumpe antes de completar, el archivo parcial queda en disco. `verifyApkSignature()` lo rechazará, pero sería más limpio validar también que `destFile.length() == Content-Length`.

---

## Hallazgos Positivos (para referencia)

- `android:allowBackup="false"` — previene restauración no autorizada ✓
- `isMinifyEnabled = true` en release ✓
- `network_security_config.xml` con `cleartextTrafficPermitted="false"` ✓
- Credenciales de firma en variables de entorno, no hardcodeadas ✓
- `FileProvider` con ruta privada `files-path ota/` ✓
- Verificación de firma APK antes de instalar en OTA ✓
- `Uri.encode()` en parámetros de query a lrclib.net ✓
- Todas las dependencias actualizadas a versiones recientes ✓
- Sin `Log.d/Log.e` con datos sensibles en el código ✓
- Timeouts configurados en `HttpURLConnection` (10s connect, 30s read) ✓
- Permisos runtime para GPS manejados correctamente en `MainActivity` ✓
- `hardware.camera`, `hardware.gps`, `hardware.bluetooth` con `required="false"` ✓

---

## Plan de Acción

**Corto plazo (v1 estable):**
- A-14: Añadir reglas ProGuard para reflexión A2DP y entrypoints SDK
- B-6: Enmascarar secrets de keystore en CI con `::add-mask::`

**Mediano plazo (v2):**
- A-10: Certificate Pinning para `lrclib.net`
- A-12: Validación runtime de permisos Bluetooth en todos los paths
- A-13: Loguear excepciones en `GpsSpeedRepository`
- M-6: `shrinkResources = true` en release build
- M-8: Neutralizar strings de arquitectura interna en stubs radio/carplay
