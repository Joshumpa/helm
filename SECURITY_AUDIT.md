# Auditoría de Seguridad y Calidad — Helm Android

**Fecha:** 2026-07-29 · **Cierre:** 2026-07-29  
**Cobertura:** 11 módulos Gradle, 2 workflows CI/CD, ~200 archivos analizados

---

## Resultado

| Severidad | Encontrados | Resueltos | Aceptados |
|-----------|-------------|-----------|-----------|
| CRÍTICO   | 8           | 8         | 0         |
| ALTO      | 14          | 12        | 2         |
| MEDIO     | 8           | 8         | 0         |
| BAJO      | 7           | 6         | 1         |

---

## Riesgos aceptados (pendiente futura iteración)

### A-10 · Sin Certificate Pinning para `lrclib.net`
**Archivo:** `launcher/media/LyricsRepository.kt`

Pinning de cert para un servicio de letras de terceros con rotación frecuente rompería la app en cada renovación. Mitigado parcialmente: respuesta limitada a 512 KB + validación de `Content-Type`. Se revisará cuando se adopte OkHttp en el proyecto.

### B-1 · `READ_LOGS` con `tools:ignore="ProtectedPermissions"`
**Archivo:** `app/src/main/AndroidManifest.xml`

La supresión está en el nodo `<uses-permission>` específico, no en el archivo — alcance ya mínimo. El permiso solo funciona instalado como app de sistema (Track B). Aceptado hasta ese momento.

---

## Hallazgos Positivos

- `android:allowBackup="false"` ✓
- `isMinifyEnabled = true` + `isShrinkResources = true` en release ✓
- `network_security_config.xml` con `cleartextTrafficPermitted="false"` ✓
- Credenciales de firma en variables de entorno, no hardcodeadas ✓
- Secrets de CI enmascarados con `::add-mask::` antes del build ✓
- `FileProvider` con ruta privada `files-path ota/` ✓
- Verificación de firma APK antes de instalar en OTA ✓
- Validación de `Content-Length` vs bytes descargados ✓
- `Uri.encode()` en parámetros de query a lrclib.net ✓
- Dependencias actualizadas a versiones recientes ✓
- Sin `Log.d/Log.e` con datos sensibles en el código ✓
- Permisos runtime GPS manejados correctamente en `MainActivity` ✓
- `hardware.*` con `required="false"` ✓
- Reglas ProGuard para reflexión A2DP, OTA y SDK ✓
- callbackFlow template con `awaitClose` documentado en McuDataSource ✓
- DataStore marcado como no cifrado para prevenir uso incorrecto futuro ✓
- `ACCESS_BACKGROUND_LOCATION` justificado en manifest ✓
