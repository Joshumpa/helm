## 1. SDK — Fuente de datos real

- [x] 1.1 Definir interfaz `WeatherDataSource` en `:sdk` si no existe aún (temperatura: Double, condición: WeatherCondition)
- [x] 1.2 Crear `OpenMeteoWeatherDataSource` en `:sdk` — consulta `api.open-meteo.com` con OkHttp, parsea temperatura + código WMO con kotlinx.serialization
- [x] 1.3 Implementar mapeo de códigos WMO → `WeatherCondition` enum (0=CLEAR, 1-3=CLOUDY, 51-67=RAIN, 71-77=SNOW, 80-82=RAIN, 95+=STORM)
- [x] 1.4 Agregar timeout de 5s en OkHttp y manejo de IOException → devuelve null para activar fallback

## 2. SDK — Integración GPS

- [x] 2.1 En `OpenMeteoWeatherDataSource`, obtener coordenadas via `LocationManager.getLastKnownLocation()` primero; si es null o tiene más de 10 min, solicitar ubicación activa con timeout de 3s
- [x] 2.2 Si no hay coordenadas disponibles, emitir null para que el ViewModel oculte el widget

## 3. ViewModel — Refresco periódico

- [x] 3.1 En `WeatherViewModel` (`:app` / launcher), reemplazar inyección de `WeatherDataSourceStub` por `OpenMeteoWeatherDataSource`
- [x] 3.2 Implementar bucle de refresco: `flow { while(true) { emit(fetch()); delay(30.minutes) } }` con `collectAsState`
- [x] 3.3 Manejar resultado null (sin GPS o sin red) → estado `WeatherUiState.Unavailable` que oculta el widget en el home screen
- [x] 3.4 Al arrancar, emitir último dato conocido (caché en memoria) antes de que llegue la respuesta de red

## 4. Permisos y manifest

- [x] 4.1 Verificar que `INTERNET` esté declarado en el manifest de `:app`; agregar si falta
- [x] 4.2 Verificar que `ACCESS_FINE_LOCATION` esté declarado (ya debería estar para el speed badge GPS)

## 5. Verificación

- [x] 5.1 Compilar con `./gradlew assembleDebug` sin errores
- [ ] 5.2 Instalar en el head unit y confirmar que el home screen muestra temperatura real (distinta de 22°C stub)
- [ ] 5.3 Confirmar que al poner el dispositivo en modo avión el widget desaparece o muestra el último dato cacheado
