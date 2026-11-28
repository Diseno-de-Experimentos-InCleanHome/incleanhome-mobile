# Endpoints del Backend que usa la app Android (To-Be)

Base revisada: `incleanhome-backend`, rama `main`, commit `6bc393c` (etiqueta `v1.1.0` más la corrección de Swagger de US43), consultado el 2026-11-27. El detalle de cada contrato está en `docs/to-be-evidencia.md` de ese repositorio. Las propiedades de Gradle se describen en el [README](../README.md#propiedades-de-gradle).

## Integrados en la app

| Verbo y ruta | User Story | Uso en Android | Activación |
|---|---|---|---|
| `GET /api/experiments/config` | US43 | `ExperimentationApi.getConfig`; la configuración se guarda en caché y reemplaza la de la variante | `staging`: activo. `production`: `-Pincleanhome.remoteExperiments=true` |
| `POST /api/tracking/events` | US42 | Outbox persistente; envío en `onStart` y `onStop`, lotes de hasta 500 eventos, reintento con el mismo `eventId` | `staging`: activo. `production`: `-Pincleanhome.trackingUpload=true` |
| `GET /api/reviews/worker/{id}` | US34 | Campos `isVerified`, `serviceType` y `serviceDate` | Siempre |
| `GET /api/reviews/worker/{id}/summary` | US34 | Promedio, distribución y servicios completados del grupo de tratamiento de EC-BLQ02 | Según EC-BLQ02 |
| `POST /api/reviews` | US34/US35 | El comentario es obligatorio (la API responde `400` si falta) | Siempre |
| `POST /api/bookings/estimate` | US41 | Monto estimado con la tarifa vigente al cambiar servicio, fecha u horario | Según EC-BLQ06 |
| `POST /api/bookings` | US41 | Horas derivadas del horario, duración mínima de 30 minutos y fecha de Perú (`America/Lima`) | Siempre |
| `POST /api/auth/login`, `/2fa/verify`, `/2fa/enable` | US39 | `429` con `Retry-After` y `retryAfterSeconds`: la app muestra el tiempo de espera | Siempre |

Notas de la integración:
- Los eventos sin usuario (por ejemplo, las preguntas frecuentes antes de iniciar sesión) se quedan en el dispositivo. La API exige sesión y les asignaría la cuenta que los envía.
- Para contar operaciones hay que filtrar `properties ->> 'capturedBy' = 'server'`. La app también envía `booking_request_created`, `booking_request_failed` y `review_submitted` con `capturedBy: "client"`.
- El campo `district` se envía tal como lo escribe el usuario. La API solo acepta textos con forma de nombre. La solución de fondo es una lista cerrada de distritos, que depende de US32.

## Siguen faltando en el Backend

| # | Verbo y ruta | User Story | Propiedad Android | Estado Android |
|---|---|---|---|---|
| 1 | `GET` y `PUT /api/workers/me/service-area` | US32 | `-Pincleanhome.serviceAreaApi=true` | Pantalla y validación de rango implementadas (`ServiceAreaApi`). Sin el endpoint no se declara el área como guardada |
| 2 | `GET /api/workers/suggestions` | US33 | — | Hoy se usa `GET /workers` filtrado por servicio y zona, más `GET /workers/{id}/availability`. Sin distancia ni radio |
| 3 | Envío del recordatorio por push o correo | US35 | — | Solo existe el recordatorio dentro de la app (`channel=in_app`) |
| 4 | `GET /api/auth/access-activity` y `POST /api/auth/access-activity/{id}/report` | US40 | — | Sin pantalla hasta que exista el servicio. `access_attempts` (US39) guarda solo los intentos fallidos o rechazados, no los accesos correctos |
| 5 | Ambiente de pruebas desplegado (URL HTTPS) | Todas | `-Pincleanhome.stagingApiUrl=https://…/api/` | `staging` usa la API local (`http://10.0.2.2:5000/api/`) |
| 6 | Despliegue de producción | Todas | — | El 2026-11-27, `https://incleanhome-api-e4tv.onrender.com` respondía 404 con `x-render-routing: no-server` |

### US32. Área de atención del prestador

`GET /api/workers/me/service-area` → `200 { "district": "miraflores", "radiusKm": 5 }`, o `404` si no existe.
`PUT /api/workers/me/service-area` con `{ "district": "miraflores", "radiusKm": 5 }` → `200` con el recurso guardado. Si el radio está fuera de los límites, la respuesta es `400 { "error": "El radio debe estar entre 1 y 15 km" }`.
No se envían coordenadas. Los límites de 1 a 15 km son provisionales hasta que se confirme EXQ04.

### US33. Sugerencias

`GET /api/workers/suggestions?serviceType=limpieza_general&date=2026-11-27&startTime=09:00&endTime=11:00&district=miraflores`
→ `200 [ WorkerResource + "approxDistanceKm" ]`. Devuelve como máximo 5 prestadores disponibles en ese horario cuyo radio incluye el distrito, ordenados por distancia y calificación. La app no calcula distancias: las toma de la API.

### US35. Recordatorio

Un proceso que, 24 h después de que una reserva quede completada sin reseña, envíe una sola notificación (push FCM o correo) con un enlace a la calificación. Debe registrar `review_reminder_sent` con `channel=push|email`, no enviarse si ya existe la reseña y respetar EC-BLQ03.

### US40. Actividad de acceso

`GET /api/auth/access-activity` → `200 { "twoFactorEnabled": true, "accesses": [ { "id": 1, "occurredAt": "…", "platform": "mobile", "twoFactorVerified": true } ] }`, con los últimos 10 accesos de la cuenta de la sesión.
`POST /api/auth/access-activity/{id}/report` → `204`. Cierra las demás sesiones y obliga a cambiar la contraseña. Un `id` de otra cuenta responde `404`.
Requiere registrar también los accesos correctos, con la plataforma y el estado del segundo factor.
