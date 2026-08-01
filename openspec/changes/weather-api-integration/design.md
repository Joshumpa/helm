## Context

Ver `proposal.md — Why`. `WeatherViewModel` ya existe en el launcher y consume un `WeatherDataSource`. La interfaz está definida; solo falta una implementación real. El permiso `ACCESS_FINE_LOCATION` ya está declarado en el manifest para el speed badge GPS.

## Goals / Non-Goals

**Goals:**
- Implementar `OpenMeteoWeatherDataSource` que reemplaza al stub
- Refresco automático cada 30 minutos con caché en memoria
- Manejo de ausencia de GPS o red sin crashear

**Non-Goals:**
- No se agrega pantalla de clima
- No se agrega pronóstico extendido
- No se persiste el caché en disco (solo en memoria mientras la app vive)

## Decisions

**API: Open-Meteo (openmeteo.com)**
- Sin clave API, sin límite de uso, responde temperatura actual + código WMO de condición
- Alternativa considerada: OpenWeatherMap — requiere registro y clave API, innecesaria complejidad para v1
- URL: `https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lng}&current=temperature_2m,weather_code`

**HTTP client: OkHttp**
- Ya disponible en el proyecto (Media3 lo trae transitivamente)
- Alternativa: `HttpURLConnection` — sin dependencias extra pero más boilerplate y sin timeout fácil

**Parsing: kotlinx.serialization**
- Ya presente en el proyecto
- Se parsea solo el subnodo `current` de la respuesta para minimizar código

**Refresco: `Flow` con `repeatOnLifecycle` en el ViewModel**
- `flow { while(true) { emit(fetch()); delay(30.min) } }` — simple, sin WorkManager
- Alternativa: WorkManager — overhead innecesario para un refresco que solo importa cuando la app está activa

**Mapeo WMO → WeatherCondition**
- Códigos 0 = CLEAR, 1-3 = PARTLY_CLOUDY/CLOUDY, 51-67 = RAIN, 71-77 = SNOW, 80-82 = RAIN, 95+ = STORM
- El enum `WeatherCondition` existente ya cubre estos estados

## Risks / Trade-offs

- **[Risk] Open-Meteo no está disponible** → Mitigation: timeout de 5s en OkHttp, devuelve último dato conocido o oculta widget
- **[Risk] Drift de GPS en garaje/estacionamiento** → Mitigation: aceptable — la coordenada será cercana y el clima de una ciudad no varía en km
- **[Risk] Consumo de batería por GPS** → Mitigation: se usa `getLastKnownLocation()` primero; solo pide ubicación activa si la última tiene más de 10 min

## Open Questions

- ¿El campo de temperatura en el home screen muestra solo número + `°C`, o también el ícono de condición? (Decisión de UX — no bloquea implementación, el campo ya existe)
