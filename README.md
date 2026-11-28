# InCleanHome Mobile

App Android de InCleanHome (Kotlin, Jetpack Compose, Retrofit). Conecta a clientes con prestadores de limpieza: búsqueda, reservas, mensajería, reseñas, reclamos y autenticación con 2FA.

La versión `v1.0.0` corresponde al As-Is. La versión `1.1.0` (versionCode 2) incorpora las funcionalidades To-Be y los experimentos (US32–US43) y se conecta a la API desplegada. Los APK se publican en [GitHub Releases](https://github.com/Diseno-de-Experimentos-InCleanHome/incleanhome-mobile/releases).

## Requisitos

- Android Studio con el SDK de Android 17 (API 37) y build-tools 37.0.0.
- JDK 25 (el mismo que usa la CI).
- El wrapper de Gradle incluido (`gradlew`). No hace falta instalar Gradle.

## Variantes de compilación

La dimensión `environment` define dos flavors. Combinados con `debug` y `release` dan cuatro variantes.

| | `production` | `staging` |
|---|---|---|
| Application ID | `com.incleanhome.mobile` | `com.incleanhome.mobile.staging` (se instala junto a producción) |
| API | `https://incleanhomebackend-q8za.onrender.com/api/` (la misma de la web; solo HTTPS) o la URL de `incleanhome.productionApiUrl` | `http://10.0.2.2:5000/api/` (API local vista desde el emulador) o la URL de `incleanhome.stagingApiUrl` |
| Experimentos | Todos desactivados | Activados según `incleanhome.stagingExperiments` |
| Pantalla de diagnóstico y log de eventos | No | Sí |
| Recordatorio para calificar | 24 h | Inmediato, para poder probarlo |

`staging` es la variante por defecto en Android Studio. Así, una ejecución de depuración nunca escribe datos de prueba en producción. En `production` los experimentos solo podrán activarse mediante la configuración remota, cuando el Backend la publique.

## Compilar

```bash
./gradlew assembleStagingDebug        # APK de pruebas
./gradlew assembleProductionDebug     # APK de producción, depuración
./gradlew assembleProductionRelease   # APK de producción (firmado si hay datos de firma)
```

En Windows usa `gradlew.bat`. Los APK quedan en `app/build/outputs/apk/<flavor>/<buildType>/`.

### Firma del APK de producción

El repositorio no guarda keystores ni contraseñas. El APK release se firma solo si existen los cuatro valores siguientes, como propiedades en `~/.gradle/gradle.properties` (fuera del repositorio) o como variables de entorno. Sin ellos, el APK release sale sin firmar.

| Propiedad de Gradle | Variable de entorno |
|---|---|
| `incleanhome.signing.storeFile` (ruta absoluta del keystore) | `INCLEANHOME_SIGNING_STORE_FILE` |
| `incleanhome.signing.storePassword` | `INCLEANHOME_SIGNING_STORE_PASSWORD` |
| `incleanhome.signing.keyAlias` | `INCLEANHOME_SIGNING_KEY_ALIAS` |
| `incleanhome.signing.keyPassword` | `INCLEANHOME_SIGNING_KEY_PASSWORD` |

Para que un APK nuevo se instale como actualización de uno anterior, debe firmarse con la misma clave. El keystore y sus contraseñas deben respaldarse fuera del repositorio. Para comprobar la firma: `apksigner verify --print-certs app-production-release.apk`.

## Pruebas y Lint

```bash
./gradlew testStagingDebugUnitTest testProductionDebugUnitTest
./gradlew lintProductionRelease lintStagingDebug
```

Los reportes quedan en `app/build/reports/tests/` y `app/build/reports/lint-results-*.html`. Lint falla si encuentra errores, y `assembleProductionRelease` ejecuta además las comprobaciones críticas. La CI (`.github/workflows/mobile-ci.yml`) ejecuta las pruebas unitarias y compila los APK debug de ambos flavors en `main`, `develop` y `feature/**`.

## Propiedades de Gradle

Se pasan con `-P` o en `~/.gradle/gradle.properties`. Ninguna contiene secretos.

| Propiedad | Uso | Por defecto |
|---|---|---|
| `incleanhome.productionApiUrl` | URL de la API para `production`; debe usar HTTPS | `https://incleanhomebackend-q8za.onrender.com/api/` |
| `incleanhome.stagingApiUrl` | URL de la API de pruebas para `staging` | `http://10.0.2.2:5000/api/` |
| `incleanhome.stagingExperiments` | Experimentos de `staging` con el formato `ID=on\|off:porcentaje;…`. La configuración remota los reemplaza cuando se descarga | BLQ03 desactivado y el resto activos |
| `incleanhome.remoteExperiments` | Descarga la configuración de experimentos de `GET /api/experiments/config` (US43) | `staging`: `true`; `production`: `false` |
| `incleanhome.trackingUpload` | Envía los eventos registrados a `POST /api/tracking/events` (US42) | `staging`: `true`; `production`: `false` |

En `production`, la configuración remota y el envío de eventos se activan solo con autorización del equipo. En `staging`, la configuración descargada reemplaza a la de la variante. La API usa por defecto todos los experimentos desactivados, así que la API de pruebas debe publicar los que se quieran probar (por ejemplo, `Experiments__EC_BLQ06__Enabled=true`). Si se compila con `-Pincleanhome.remoteExperiments=false`, la app ignora la configuración remota que haya guardado antes. El área de atención (US32) y las sugerencias (US33) dependen de `EC-BLQ01-SA` y `EC-BLQ01` en la API: si la API responde `403`, la app conserva el comportamiento de control. La actividad de acceso (US40) no depende de un experimento. Los contratos están en [docs/backend-endpoints.md](docs/backend-endpoints.md).

## Datos locales y respaldo

La sesión cifrada y los datos de experimentos (variantes asignadas, eventos por enviar y recordatorios) se excluyen del respaldo en la nube y de la transferencia entre dispositivos (`app/src/main/res/xml/`). La asignación experimental y los eventos pertenecen al dispositivo en el que se generaron.
