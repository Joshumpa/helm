## Why

El home screen de Helm muestra temperatura y condición climática pero `WeatherViewModel` usa `WeatherDataSourceStub()` con datos hardcodeados (22°C, CLEAR). Para v1 daily-driver, el clima debe reflejar la condición real del conductor.

## What Changes

- Reemplazar `WeatherDataSourceStub` con una implementación real que consulte una API de clima pública (Open-Meteo — sin clave API, sin límite de uso).
- `WeatherRepository` obtiene lat/lng actual (GPS ya disponible en Track A) y consulta temperatura + código WMO de condición.
- Mapeo de códigos WMO → `WeatherCondition` enum existente (CLEAR, CLOUDY, RAIN, etc.).
- Refresco cada 30 minutos o al arrancar la app; caché en memoria para no hacer requests frecuentes.
- No hay pantalla nueva — el home screen ya muestra los datos.

## Capabilities

### New Capabilities

- `weather-current-conditions`: El sistema obtiene y muestra temperatura y condición climática real a partir de la ubicación GPS del vehículo, mediante la API pública Open-Meteo.

### Modified Capabilities

_(ninguna — no hay specs existentes de clima)_

## Impact

- **:app / launcher**: `WeatherViewModel` deja de usar el stub; inyecta `WeatherRepository` real.
- **:sdk**: Nuevo `WeatherDataSource` (interfaz) + `OpenMeteoWeatherDataSource` (implementación HTTP).
- **Dependencias**: `kotlinx.serialization` (ya presente) para parsear JSON; `kotlinx.coroutines` para el refresco periódico. No se agrega Retrofit — se usa `HttpURLConnection` o `OkHttp` ya disponible en el proyecto.
- **Permisos**: `ACCESS_FINE_LOCATION` (ya declarado para GPS). Se agrega `INTERNET` si no está presente.
- **Track**: A (no requiere root).

## Non-goals

- No se implementa una pantalla de clima dedicada.
- No se guarda historial ni pronóstico extendido.
- No se agrega una clave API — Open-Meteo es gratuito sin autenticación.
- No se localiza la descripción textual del clima (solo se muestra el ícono/condición).
