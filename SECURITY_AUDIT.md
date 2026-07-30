# Auditoría de Seguridad y Calidad — Helm Android

**Fecha:** 2026-07-29 · **Cierre:** 2026-07-29  
**Cobertura:** 11 módulos Gradle, 2 workflows CI/CD, ~200 archivos analizados

---

## Resultado

| Severidad | Encontrados | Resueltos | Aceptados | Pendientes |
|-----------|-------------|-----------|-----------|------------|
| CRÍTICO   | 8           | 8         | 0         | 0          |
| ALTO      | 17          | 15        | 2         | 0          |
| MEDIO     | 8           | 8         | 0         | 0          |
| BAJO      | 7           | 6         | 1         | 0          |

---

## Hallazgos resueltos — Segunda auditoría 2026-07-29

### N-1 · `HelmMusicService` exportado sin permiso de bind ✅ resuelto en 9f947a3
**Archivos:** `app/src/main/AndroidManifest.xml:84-91` · `audio/src/main/kotlin/dev/helm/audio/HelmMusicService.kt:41-45`  
**Severidad:** Alta

`HelmMusicService` tiene `android:exported="true"` sin `android:permission` en el `<service>`. El guard en `onGetSession` compara `controllerInfo.packageName == packageName`, pero ese valor lo suministra el caller vía `connectionHints` — no está verificado por el sistema. Cualquier app puede hacer bind y controlar la reproducción o inyectar URIs al queue de ExoPlayer.

**Fix:**
```xml
<service
    android:name=".HelmMusicService"
    android:permission="android.permission.MEDIA_CONTENT_CONTROL"
    android:exported="true">
```
En `onGetSession`: confiar solo en `controllerInfo.isTrusted`, eliminar la comparación por `packageName`.

---

### N-2 · OTA — URL de descarga sin validación de host/scheme ✅ resuelto en 36e5b05
**Archivo:** `ota/src/main/kotlin/dev/helm/ota/OtaRepository.kt:45-56`  
**Severidad:** Alta

`browser_download_url` de la respuesta JSON de GitHub se pasa verbatim a `URL(apkUrl).openConnection()` sin verificar scheme ni host. `HttpURLConnection` sigue redirects por defecto. Con cuenta de GitHub comprometida o DNS poisoning se puede redirigir la descarga a un servidor arbitrario.

**Fix:**
```kotlin
val parsed = URL(apkUrl)
require(parsed.protocol == "https") { "Scheme inválido" }
require(
    parsed.host.endsWith(".github.com") ||
    parsed.host.endsWith(".githubusercontent.com")
) { "Host no permitido" }
conn.instanceFollowRedirects = false
```

---

### N-3 · OTA — Comparación de firma APK con lógica `any/any` ✅ resuelto en 752900b
**Archivo:** `ota/src/main/kotlin/dev/helm/ota/OtaRepository.kt:102-104`  
**Severidad:** Alta

La verificación usa `apkCerts.any { apk -> installedCerts.any { ... } }` — pasa si el APK descargado comparte **al menos un** certificado con el instalado. Si se rota la clave de firma y la anterior queda comprometida, un APK firmado con esa clave vieja seguiría pasando el check. Agravado por N-2.

**Fix:**
```kotlin
// Igualdad estricta de conjuntos
require(
    apkCerts.map { it.toCharsString() }.toSet() ==
    installedCerts.map { it.toCharsString() }.toSet()
)
```
Opción más robusta: comparar contra un fingerprint SHA-256 hardcodeado del cert de release, eliminando dependencia del runtime.

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
