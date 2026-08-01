## Purpose

Proporciona temperatura y condición climática actuales basadas en la ubicación GPS del vehículo, obtenidas de una fuente externa en tiempo real, para que el conductor pueda verlas en el home screen sin salir de Helm.

## ADDED Requirements

### Requirement: Condición climática real

El sistema SHALL obtener temperatura y condición climática actuales desde una API externa usando las coordenadas GPS actuales del vehículo.

#### Scenario: Datos disponibles

- **WHEN** la aplicación está corriendo y hay señal GPS y conectividad de red disponibles
- **THEN** el home screen muestra temperatura en °C y condición climática actualizada (máximo 30 minutos de antigüedad)

#### Scenario: Sin conectividad de red

- **WHEN** no hay conexión de red disponible
- **THEN** el home screen muestra el último dato conocido si tiene menos de 30 minutos de antigüedad, o bien oculta el widget de clima

#### Scenario: Sin señal GPS

- **WHEN** no se puede obtener la ubicación GPS del vehículo
- **THEN** el sistema no realiza solicitudes a la API de clima y el widget de clima no se muestra

### Requirement: Refresco periódico

El sistema SHALL refrescar los datos de clima automáticamente sin intervención del usuario.

#### Scenario: Refresco en background

- **WHEN** han pasado 30 minutos desde la última actualización exitosa y la app está en foreground
- **THEN** el sistema realiza una nueva consulta a la API de clima y actualiza los datos mostrados

#### Scenario: Refresco al arrancar

- **WHEN** la aplicación se inicia y hay GPS y red disponibles
- **THEN** el sistema solicita datos de clima antes de mostrar el home screen (o en paralelo sin bloquear el arranque)

### Requirement: Privacidad de ubicación

El sistema MUST usar la ubicación únicamente para consultar clima — no la almacena ni la envía a servicios distintos a la API de clima.

#### Scenario: Uso exclusivo para clima

- **WHEN** el sistema obtiene coordenadas GPS para la consulta de clima
- **THEN** las coordenadas se usan solo para formar la solicitud HTTP a la API de clima y se descartan después
