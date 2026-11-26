# Endpoints del Backend que necesita la app Android (To-Be)

Base revisada: `incleanhome-backend`, rama `main`, commit `6af2c7f` (consultado el 2026-11-25). Ninguno de los endpoints siguientes existe en esa versión. La app ya tiene la integración preparada y desactivada mediante propiedades de Gradle; se activa sin cambiar código cuando el Backend publique cada endpoint (ver [README](../README.md#propiedades-de-gradle)).

| # | Verbo y ruta | User Story | Propiedad Android que lo activa | Estado Android |
|---|---|---|---|---|
| 1 | `GET /api/experiments/config` | US43 | `-Pincleanhome.remoteExperiments=true` | Cliente Retrofit y caché implementados (`ExperimentationApi`) |
| 2 | `POST /api/tracking/events` | US42 | `-Pincleanhome.trackingUpload=true` | Outbox persistente y envío en `onStart` implementados |
| 3 | `GET` y `PUT /api/workers/me/service-area` | US32 | `-Pincleanhome.serviceAreaApi=true` | Pantalla y validación de rango implementadas (`ServiceAreaApi`) |
| 4 | `GET /api/workers/suggestions` | US33 | (reemplaza la composición actual) | Hoy se usa `GET /workers` + `GET /workers/{id}/availability` |
| 5 | Campos `serviceType` y `serviceDate` en `ReviewResource` | US34 | ninguna (campos opcionales) | La app los muestra si llegan |
| 6 | Job de recordatorio (push o correo) | US35 | — | Recordatorio dentro de la app implementado; falta el envío fuera de la app |
| 7 | `GET /api/auth/access-activity` y `POST /api/auth/access-activity/{id}/report` | US40 | — | Sin UI hasta que exista el servicio real |
| 8 | Registro de intentos fallidos (escenario 3) | US39 | — | La app ya maneja el 429 del límite existente |
| 9 | Ambiente de pruebas desplegado (URL HTTPS) | Todas | `-Pincleanhome.stagingApiUrl=https://…/api/` | Variante `staging` lista; no se usa ninguna URL inventada |
| 10 | Redespliegue de producción | Todas | — | El 2026-11-25 `https://incleanhome-api-e4tv.onrender.com` respondía 404 `x-render-routing: no-server` |

## 1. Configuración de experimentos (US43)

`GET /api/experiments/config` (autenticado o anónimo)

```json
{ "experiments": [
  { "id": "EC-BLQ01", "enabled": true,  "treatmentPercent": 50 },
  { "id": "EC-BLQ02", "enabled": true,  "treatmentPercent": 50 },
  { "id": "EC-BLQ03", "enabled": false, "treatmentPercent": 50 },
  { "id": "EC-BLQ06", "enabled": true,  "treatmentPercent": 50 },
  { "id": "EC-BLQ04", "enabled": true,  "treatmentPercent": 100 },
  { "id": "EC-BLQ01-SA", "enabled": true, "treatmentPercent": 100 } ] }
```

La asignación de variante es determinista y debe reproducirse igual en la web y en la API:
`bucket = (primeros 4 bytes, big-endian, sin signo, de SHA-256("<experimentId>:<userId>")) mod 100`; tratamiento si `bucket < treatmentPercent`. La app guarda la primera variante calculada para cada usuario.

## 2. Recepción de eventos (US42)

`POST /api/tracking/events` (Bearer del usuario). Respuesta esperada: `202` o `204`. El Backend debe ignorar un `eventId` repetido (idempotencia).

```json
{ "events": [ {
  "eventId": "b0a3e203-fa1b-3541-b16c-3cbd73683b73",
  "eventName": "worker_profile_viewed",
  "occurredAt": "2026-11-26T03:31:27.195834Z",
  "platform": "mobile", "os": "android",
  "appVersion": "1.0-staging", "environment": "staging",
  "userRole": "client", "userRef": "668702718af62b58",
  "sessionId": "1fd15e90-1096-4e4d-a68b-d74f7acd19eb",
  "experimentId": "EC-BLQ02", "variant": "control", "featureActive": false,
  "properties": { "workerId": 3, "source": "filters", "searchId": "1e84f0ee-…" } } ] }
```

Privacidad: `userRef` es un SHA-256 de `incleanhome-user:<id>`. Como los id son secuenciales, el Backend debería reemplazarlo al recibirlo por un HMAC con una sal privada del servidor antes de almacenarlo o enviarlo a analítica.

Los eventos que el plan asigna al servidor (`booking_request_created`, `review_submitted`, `booking_status_changed`, `review_reminder_sent` por push) deberían registrarse en la API al confirmar la operación. La app también los registra con `capturedBy=client` para poder conciliarlos.

## 3. Área de atención del prestador (US32)

`GET /api/workers/me/service-area` → `200 { "district": "miraflores", "radiusKm": 5 }` o `404` si no existe.
`PUT /api/workers/me/service-area` con el cuerpo `{ "district": "miraflores", "radiusKm": 5 }` → `200` con el recurso guardado. Si el radio está fuera de los límites, responde `400 { "error": "El radio debe estar entre 1 y 15 km" }`.
No se envían coordenadas. Los límites de 1 a 15 km son provisionales, a la espera de la confirmación de EXQ04.

## 4. Sugerencias (US33)

`GET /api/workers/suggestions?serviceType=limpieza_general&date=2026-11-27&startTime=09:00&endTime=11:00&district=miraflores`
→ `200 [ WorkerResource + "approxDistanceKm" ]`. Devuelve como máximo 5 prestadores, ordenados por distancia y calificación, y solo aquellos cuyo radio incluye el distrito.

## 5. Reseñas verificadas (US34)

Agregar a `ReviewResource` los campos `serviceType` (string) y `serviceDate` (`yyyy-MM-dd`) de la reserva asociada.

## 6. Recordatorio (US35)

Un job que, pasadas 24 h desde que la reserva quedó completada y sin reseña, envíe una sola notificación (push FCM o correo) con un enlace a la calificación. Debe registrar `review_reminder_sent` con `channel=push|email`.

## 7. Actividad de acceso (US40)

`GET /api/auth/access-activity` → `200 { "twoFactorEnabled": true, "accesses": [ { "id": 1, "occurredAt": "…", "platform": "mobile" } ] }` (últimos 10 accesos).
`POST /api/auth/access-activity/{id}/report` → `204`. Cierra las demás sesiones y obliga a cambiar la contraseña.
