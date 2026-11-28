# Endpoints del Backend que usa la app Android (To-Be)

Base revisada: `incleanhome-backend`, rama `main`, commit `94bc130` (merge del PR #5, etiqueta `v1.2.0`), consultado el 2026-11-27. El detalle de cada contrato está en `docs/to-be-evidencia.md` de ese repositorio. Las propiedades de Gradle se describen en el [README](../README.md#propiedades-de-gradle).

## Integrados en la app

| Verbo y ruta | User Story | Uso en Android | Activación |
|---|---|---|---|
| `GET /api/experiments/config` | US43 | `ExperimentationApi.getConfig`; la configuración se guarda en caché y reemplaza la de la variante | `staging`: activo. `production`: `-Pincleanhome.remoteExperiments=true` |
| `POST /api/tracking/events` | US42 | Outbox persistente; envío en `onStart` y `onStop`, lotes de hasta 500 eventos, reintento con el mismo `eventId` | `staging`: activo. `production`: `-Pincleanhome.trackingUpload=true` |
| `GET` y `PUT /api/workers/me/service-area` | US32 | `ServiceAreaApi`: distrito de una lista cerrada y radio de 1 a 15 km. `404` en el `GET` = sin área; `400` muestra el mensaje de la API y, si trae `validDistricts`, actualiza la lista; `403` = funcionalidad no activa | Según `EC-BLQ01-SA` en la API |
| `GET /api/workers/{id}?district=` | US32 | El perfil muestra `serviceDistrict` y `approxDistanceKm` (solo distrito y distancia aproximada); `district` se envía al venir de una sugerencia | Siempre (los campos llegan en `null` si no aplica) |
| `GET /api/workers/suggestions` | US33 | `WorkerSuggestionService`: hasta 5 prestadores en el orden de la API con `approxDistanceKm`. `[]` = sin sugerencias; `403` = grupo de control, solo búsqueda por filtros | Según `EC-BLQ01` |
| `GET /api/reviews/worker/{id}` | US34 | Campos `isVerified`, `serviceType` y `serviceDate` | Siempre |
| `GET /api/reviews/worker/{id}/summary` | US34 | Promedio, distribución y servicios completados del grupo de tratamiento de EC-BLQ02 | Según EC-BLQ02 |
| `POST /api/reviews` | US34/US35 | Comentario obligatorio y `source` (`reminder` desde un recordatorio, `direct` en otro caso) | Siempre |
| `POST /api/bookings/estimate` | US41 | Monto estimado con la tarifa vigente al cambiar servicio, fecha u horario | Según EC-BLQ06 |
| `POST /api/bookings` | US33/US41 | Horas derivadas del horario, duración mínima de 30 minutos, fecha de Perú y `origin` (`suggestion`, `filters` o `direct`) | Siempre |
| `POST /api/auth/login`, `/2fa/verify`, `/2fa/enable` | US39 | `429` con `Retry-After` y `retryAfterSeconds`: la app muestra el tiempo de espera | Siempre |
| `GET /api/auth/access-activity` | US40 | Últimos 10 accesos de la cuenta de la sesión: fecha y hora (UTC, mostrada en hora local), plataforma, resultado y segundo factor | Siempre |
| `POST /api/auth/access-activity/{id}/report` | US40 | Reportar un acceso no reconocido; `404` si no es de la cuenta. La API cierra las demás sesiones y pide cambiar la contraseña | Siempre |
| `PUT /api/auth/password` | US40 | Pantalla de cambio de contraseña con la sesión abierta; la política la valida la API | Siempre |

Notas de la integración:
- Los eventos sin usuario (por ejemplo, las preguntas frecuentes antes de iniciar sesión) se quedan en el dispositivo. La API exige sesión y les asignaría la cuenta que los envía.
- Para contar operaciones hay que filtrar `properties ->> 'capturedBy' = 'server'`. La app también envía `booking_request_created`, `booking_request_failed` y `review_submitted` con `capturedBy: "client"`.
- Las rutas de US40 están bajo `/api/auth/`, pero usan la sesión: `AuthInterceptor` les agrega el token. Un `401` sobre el token en uso (por ejemplo, una sesión cerrada al reportar un acceso desde otro dispositivo) cierra la sesión local.
- La app no calcula distancias ni guarda coordenadas: el catálogo de distritos (`core/location/Districts.kt`) solo contiene claves y nombres.
- El recordatorio por correo de US35 lo envía un proceso de la API (`EC-BLQ03`). La app conserva el recordatorio dentro de la app (`channel=in_app`) y envía `source=reminder` al calificar desde él.

## Fuera del alcance de Android o por resolver en el Backend

| # | Tema | User Story | Estado |
|---|---|---|---|
| 1 | Recordatorio por push (FCM) | US35 | La API solo envía correo (Brevo); sin proveedor configurado, el envío queda simulado. El enlace del correo abre la web: la app no tiene enlace profundo |
| 2 | `Security:EnforcePasswordChange` | US40 | Desactivado por defecto en la API. La app ya tiene la pantalla de cambio con sesión; el flujo con `challengeToken` de cambio obligatorio no está implementado en la app |
| 3 | Ambiente de pruebas desplegado (URL HTTPS) | Todas | `staging` usa la API local (`http://10.0.2.2:5000/api/`); se cambia con `-Pincleanhome.stagingApiUrl=https://…/api/` |
| 4 | Despliegue de producción | Todas | Resuelto: la API está desplegada en `https://incleanhomebackend-q8za.onrender.com` (la misma URL de la web) y `production` la usa desde la versión 1.1.0. La URL anterior, `https://incleanhome-api-e4tv.onrender.com`, ya no tiene servidor. En el plan gratuito de Render el servicio se suspende sin tráfico y la primera solicitud puede tardar en responder |
