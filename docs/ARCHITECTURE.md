# Arquitectura

## Resumen

Aplicación Android nativa en Java 8, sin AndroidX ni bibliotecas externas. Una sola
`Activity` principal (`MainActivity`), una pantalla de ajustes (`SettingsActivity`) y clases
de apoyo para datos, iconos, clima y dibujo.

```
MainActivity
├── AppRepository      paquetes, etiquetas, categorías, favoritos, recientes, caché
├── IconStore          iconos en memoria (LruCache) y disco (PNG)
├── BackgroundManager   imagen de la galería, recorte, EXIF, brillo y modos
├── NotificationStore   contadores por paquete y descarte (hilo principal)
├── Dashboard          tarjetas del panel
│   ├── BatteryRingView medidor circular de batería (Canvas)
│   ├── WeatherIconView iconos de clima (Canvas)
│   └── Bar            barras de memoria
├── SettingsActivity   ajustes y alias HOME
└── NotificationService listener del sistema (solo conteo)
```

## Arranque y caché

1. `onCreate` lee `files/apps_cache.json` con `AppRepository.readCache()`.
2. Si hay caché, construye la UI al instante con etiquetas y categorías; los iconos se piden
   a `IconStore` y aparecen progresivamente.
3. En paralelo, `scanApps()` consulta `PackageManager` (metadatos, sin iconos) y detecta
   altas, bajas, cambios de etiqueta o `lastUpdateTime`.
4. Si algo cambió, reconstruye en silencio conservando el foco por paquete; si no, no toca la UI.
5. El escaneo escribe de nuevo la caché.

Sin caché (primer arranque) se muestra "CARGANDO APLICACIONES" y se hace el escaneo inicial.

### Caché de iconos

- Clave: paquete + `lastUpdateTime`.
- Memoria: `LruCache` de 8 MB.
- Disco: `cache/icons/<pkg>_<updated>.png` (96×96).
- `IconStore.request()` deduplica peticiones; al llegar el icono se actualizan todos los
  mosaicos de ese paquete (puede estar en RECIENTES, Favoritos y su categoría).
- Si una app se actualiza (`lastUpdateTime` distinto), se invalida su icono y se recarga.

## Modelo de datos

`AppEntry`: paquete, actividad, etiqueta, `updated` y categoría.
`AppRepository`:

- `load()`: escaneo de `CATEGORY_LAUNCHER` y `LEANBACK_LAUNCHER`, sin iconos.
- `categoryOf()`: override guardado por paquete o heurística por palabras clave con
  normalización (minúsculas, sin acentos, tokens).
- Favoritos: `SharedPreferences` (`fav`).
- Recientes: cadena separada por `|` (`recent`), máximo 8.
- Overrides de categoría: `cat:<paquete>`.
- Caché: JSON en `files/apps_cache.json`, escritura atómica con `.tmp` + rename.

## UI y foco

- `Sistema` contiene los mosaicos **Ajustes** y **Avisos**, y el `Dashboard` con las tarjetas
  de batería, clima y memoria. La fecha vive en la cabecera.
- `RECIENTES`, `Favoritos` y categorías son secciones con encabezado y una **rejilla de 5 columnas**.
- Cada sección guarda su contenedor, rejilla y cabecera para poder insertar/eliminar en sitio.
- El foco es lógico (`focusSection`, `focusIndex`), no depende del `focusSearch` de Android.
- Navegación 2D: izquierda/derecha dentro de la fila; arriba/abajo ±5 posiciones y, en los
  bordes, a la sección anterior/siguiente conservando la columna.
- `dispatchKeyEvent` mapea cruceta, A, B, X, Y, L1/R1, START y SELECT.
- `onGenericMotionEvent` lee `AXIS_X/Y` y `AXIS_HAT_X/Y` con zona muerta 0.55 y repetición
  cada 140 ms.
- Tras cambios se restaura el foco por paquete (`focusByPackage`), no por índice.

### Actualizaciones en sitio

- Favorito: crea/elimina el mosaico de la sección Favoritos y su sección si queda vacía.
- Categoría: mueve el mosaico de la sección vieja a la nueva; crea la sección si hace falta
  en la posición correcta de `CATS`; elimina la vieja si queda vacía.
- RECIENTES: se reconstruye solo esa sección al volver de una app (`recentsDirty`).
- La rejilla afectada se reconstruye (`buildGrid`) sin tocar el resto de la pantalla;
  los iconos se reutilizan y el foco se restaura por paquete.

## Panel

- Fecha/reloj/memoria se actualizan cada 10 s (memoria cada 30 s) solo con la Activity visible.
- `ClockView` se redibuja cada segundo si el segundero está activo; si no, cada 20 s.
- RAM: `ActivityManager.MemoryInfo`.
- Interna: `StatFs` sobre `Environment.getDataDirectory()`.
- SD: `getExternalFilesDirs()`; si hay un segundo volumen se usa; si no, la fila se oculta.
- El panel se detiene en `onPause` y arranca en `onResume`.

## Clima

- `WeatherService` consulta Open-Meteo por HTTP con latitud/longitud fijas de Ciudad de Guatemala.
- Sin clave ni permiso de ubicación; solo `INTERNET`.
- Caché en `SharedPreferences` (`weather_temp`, `weather_code`, `weather_updated`).
- Se refresca si el dato tiene más de 30 minutos; el endpoint HTTP evita el problema de
  raíces modernas (Let's Encrypt/ISRG X1) que Android 5.1 no confía.
- Actualización en hilo aparte; parseo con `org.json`; errores silenciosos conservando caché.

## Notificaciones

- `NotificationService` extiende `NotificationListenerService` y cuenta claves únicas por paquete.
- Ignora notificaciones `isOngoing()` y resúmenes de grupo para no duplicar.
- `NotificationStore` guarda los contadores, los expone con `count(pkg)` y notifica a la UI
  en el **hilo principal** mediante un `Handler`.
- Los mosaicos tienen una burbuja roja (20 dp) que se muestra si el contador es > 0.
- El permiso de acceso a notificaciones se concede en Ajustes del sistema; la app solo
  abre esa pantalla. No se lee ni se guarda el contenido de las notificaciones.
- API 22 no tiene `getActiveNotifications()`: los contadores empiezan vacíos al conectar y
  solo reflejan lo que llega mientras el servicio está vivo.

## Fondo

- Dos JPEG en `res/drawable-nodpi`: `bg_dark.jpg` y `bg_light.jpg` (1280×720).
- `buildUi` añade un `ImageView` CENTER_CROP y un velo (`View` con color semitransparente)
  detrás de la página. Los mosaicos y tarjetas son opacos.
- Preferencia `background` (por defecto `true`); Ajustes permite desactivarlo y `MainActivity`
  se recrea al detectar el cambio en `onResume`.
- Sin parallax, blur en tiempo real ni live wallpaper.

## Cabecera y pista de controles

- La cabecera lleva el título `NSP` y, a la derecha, la fecha larga y un reloj cuyo estilo se
  elige en Ajustes: **analógico** (`ClockView` de 44 dp), **digital** (`HH:mm`, o `HH:mm:ss` con el
  segundero) u **oculto** (entonces la fecha crece: día grande + día de la semana y mes).
- Un ticker de 1 s actualiza los textos solo cuando cambian; el reloj analógico se redibuja solo.
- La pista de controles (`A abrir | X favorito | …`) es una vista flotante sobre la rejilla:
  aparece al construir la UI por primera vez, se atenúa a los 6 s y vuelve a mostrarse cuando
  llega una tecla sin asignar (volumen, NVIDIA, medios…).

## Anillo de batería

- `BatteryRingView` dibuja un arco de 270° con el nivel y el porcentaje en el centro.
- Los datos llegan por el broadcast `ACTION_BATTERY_CHANGED` (nivel, carga, voltaje) y por un
  refresco periódico; no hay permisos ni sondeos.
- El tiempo restante no existe en API 22 (`BATTERY_PROPERTY_TIME_REMAINNING` es API 28): se
  estima con la tasa de descarga observada (media móvil del % por minuto guardada en
  `SharedPreferences`) y se omite si el dato no es fiable.
- Si la batería llega antes de que exista el panel, el estado se guarda y se aplica al
  construirlo.

## Mosaico de Avisos

- `NotificationService` cuenta las claves de notificación vivas por paquete, ignorando las
  `ongoing` y los resúmenes de grupo.
- `NotificationStore` guarda esas claves y **notifica siempre en el hilo principal**: las
  callbacks del listener llegan por un hilo de binder y tocar la UI desde ahí falla.
- El mosaico **Avisos** muestra el total; al pulsarlo se abre la lista por app. Con el foco en
  una fila, **Y** descarta los avisos de esa app y **X** los descarta todos
  (`NotificationListenerService.cancelNotification`, API 21).

## Menú por app

- Long press sobre un mosaico: abrir, añadir/quitar de Favoritos, mover de categoría, info de
  la app (`ACTION_APPLICATION_DETAILS_SETTINGS`) y desinstalar (con confirmación y
  `ACTION_DELETE`).
- Cada acción actualiza la rejilla en sitio, sin reconstruir la pantalla.

## Imagen de fondo personalizada

- Ajustes lanza `ACTION_GET_CONTENT` (`image/*`) y recibe el URI en `onActivityResult`.
- `BackgroundManager.importImage` copia el URI a un temporal, decodifica con muestreo,
  corrige la orientación EXIF, recorta al centro a 1280×720 y guarda
  `files/custom_background.jpg`.
- Calcula el brillo medio y lo guarda en `bg_luminance`.
- `bg_mode` define el fondo: `MODE_BUNDLED` (incluido), `MODE_CUSTOM` (imagen de la galería) o
  `MODE_NONE` (sin fondo). `bg_theme_override` guarda el tema forzado sobre la foto
  (`THEME_AUTO` por defecto).
- `effectiveLight` decide el tema antes de `setTheme`: con imagen y override automático usa el
  brillo (≥ 0.55 → tema claro); si el fondo no se está viendo manda el tema del usuario.
- El velo sigue al tema: 70 % blanco en claro y 60 % negro en oscuro.
- `bg_changed` avisa a `MainActivity` para recrearse y recargar la imagen.
- La imagen solo se usa en el launcher; Ajustes y el lockscreen no la usan.
- Sin permiso de almacenamiento: se lee el URI concedido por el selector y se copia al instante.
- Si se elimina, vuelve el fondo incluido según el tema.

## Ajustes

- `SettingsActivity` muestra la sección APARIENCIA con dos filas: **Tema** y **Fondo de pantalla**.
- El diálogo de fondo ofrece *Incluido (tema actual)*, *Imagen de la galería*, *Sin fondo* y
  *Quitar imagen* (solo si hay una propia).
- Cambiar el tema con una imagen puesta fija el `bg_theme_override`, de modo que el automático
  no lo pise al arrancar.

## Launcher (HOME)

- `activity-alias` `HomeAlias` con `CATEGORY_HOME` y `enabled=false` en el manifiesto.
- `HomeUtil.setAliasEnabled()` usa `PackageManager.setComponentEnabledSetting` con
  `DONT_KILL_APP`.
- El interruptor solo hace que NSP sea candidato; el usuario elige "Siempre" en el sistema.
- `HomeUtil.isDefaultHome()` resuelve `ACTION_MAIN + CATEGORY_HOME` para decidir si B/BACK
  pueden cerrar la app.
- Desinstalar NSP revierte al launcher de NVIDIA.

## Compilación

Ver [BUILD.md](BUILD.md). Sin Gradle: `aapt2` → `javac` → `d8` → `jar` → `zipalign` → `apksigner`.

## Rendimiento

- Sin bibliotecas, sin servicios en segundo plano y sin wakelocks.
- Caché de metadatos/iconos para evitar trabajo en el arranque.
- Iconos progresivos y actualizaciones incrementales de la UI.
- Temporizadores solo con la app visible.
- En Tegra 4 la CPU/GPU es el límite; el launcher evita animaciones costosas y overdraw.

## Compatibilidad

- `minSdk 22`, `targetSdk 28`, compilado con `compileSdk 31`.
- API usadas: `getExternalFilesDirs` (19), `setLetterSpacing` (21), `Theme.Material` (21),
  `ImmersiveSticky` (19), `AppWidgetHost` no usado.
- Sin permisos de almacenamiento, ubicación ni servicios de Google.
