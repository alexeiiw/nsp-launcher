# NSP Launcher 1.3

**NSP = NVIDIA Shield Portable.**

Launcher ligero para **NVIDIA SHIELD Portable** con Android 5.1.1 (API 22), inspirado en
Windows Phone. Java puro, sin Gradle y sin dependencias externas.

![Android 5.1](https://img.shields.io/badge/Android-5.1%20(API%2022)-green)
![minSdk](https://img.shields.io/badge/minSdk-22-blue)
![Java](https://img.shields.io/badge/Java-8-orange)

## Qué hay ahora

- **Rejilla de 5 columnas** con foco 2D: izquierda/derecha dentro de la fila, arriba/abajo
  entre filas y secciones. Nada queda escondido a la derecha.
- **Cotización de BTC en la cabecera**: `BTC $68,214` con la edad del dato (por ejemplo
  *hace 3 h*). Se consulta en CoinGecko por HTTPS al abrir y al tocar el chip; se cachea
  el último valor. Incluye las raíces de Let's Encrypt empaquetadas para que funcione en
  Android 5.1 aunque el almacén del sistema sea del 2015.
- **Tarjeta AGENDA** en el panel: las 2 próximas actividades del calendario del sistema,
  hora + título; táctil y con mando: abre la app Calendario.
- **Ajustes en la cabecera**: icono ⚙ arriba a la derecha (llega con **ARRIBA** desde la
  fila SISTEMA; **B** vuelve a los mosaicos).
- **Panel de sistema**: `Agenda | Avisos | Batería | Clima | Memoria`.
- **Cabecera**: título NSP + chip BTC + fecha larga + **un solo reloj**, elegible en
  Ajustes: **Analógico / Digital / Oculto**. Con el reloj oculto, la fecha crece a la
  derecha (día grande).
- **Pista de controles**: aparece al arrancar, se desvanece a los ~6 s y vuelve a salir si
  pulsas una tecla sin asignar (volumen, NVIDIA, medios…).
- **Anillo de batería** en el panel: nivel como arco de 270 %, porcentaje en el centro, color
  por nivel (verde/ámbar/rojo) y tiempo restante estimado cuando la consola lo permite.
- **Mosaico de Avisos**: contador de notificaciones del sistema, lista por app, **Y** descarta
  los avisos de esa app y **X** los descarta todos, sin abrir las apps.
- **Menú por app** (long press): abrir, añadir/quitar de Favoritos, mover de categoría, info de
  la app y desinstalar.
- **Imagen de fondo desde la galería**, solo del launcher, con recorte, EXIF y tema/velo
  automáticos.
- **Favoritos, RECIENTES y categorías** con actualizaciones en sitio (sin reconstruir todo).
- **Arranque con caché**: metadatos e iconos en disco, pintado inmediato y escaneo en segundo
  plano. Iconos progresivos.
- **Interruptor para usar NSP como launcher**, apagado de fábrica. No borra el launcher original.

## Controles

| Botón | Acción |
|---|---|
| A / Enter | Abrir app o activar la fila enfocada |
| B | Volver en Ajustes o cerrar un diálogo; salir del launcher si no es el inicio |
| X | Añadir / quitar Favorito |
| Y | Mover la app a otra categoría; en Avisos, descartar los de esa app |
| Long press | Menú de la app |
| Cruceta / stick | Navegar por la rejilla |
| ARRIBA (desde la fila SISTEMA) | Entrar a la cabecera (BTC y Ajustes) |
| L1 / R1 | Sección anterior / siguiente |
| SELECT | Cambiar tema oscuro / claro |
| START | Recargar la lista de aplicaciones |

## Ajustes

**Apariencia**:

- **Tema**: Oscuro / Claro. Con una imagen de fondo, el tema elegido manda también sobre el velo.
- **Reloj de la cabecera**: Analógico / Digital / Oculto. El segundero aplica al analógico (aguja)
  y al digital (`HH:mm:ss`).
- **Fondo de pantalla**: abre un diálogo con *Incluido (tema actual)*, *Imagen de la galería*,
  *Sin fondo* y, si hay una propia, *Quitar imagen*.

Reglas:

- El tema automático por brillo de la foto solo actúa **si el fondo se está viendo**.
- Si fijas el tema a mano con una imagen puesta, se respeta aunque la foto sea más clara u oscura.
- El fondo **solo** afecta al launcher: ni el lockscreen ni el wallpaper del sistema.

**Notificaciones**: *Acceso a notificaciones* abre los ajustes del sistema. Al activarlo, cada
mosaico muestra un contador y el mosaico **Avisos** permite ver y descartar los avisos.

**Sistema**: *Usar NSP como inicio*, *Ajustes de Inicio del sistema*, *Recargar
aplicaciones*, **Apagar consola…**.

### Apagar consola (una vez)

"Apagar consola…" abre el **diálogo del sistema** (Apagar / Reiniciar) usando un servicio
de accesibilidad mínimo. Para que funcione, actívalo una vez:

1. **Ajustes → Apagar consola…** → **Abrir Acceso** (o directamente Ajustes → Accesibilidad).
2. Activa **NSP · Menú de energía**.
3. Vuelve y pulsa **Apagar consola…**: sale el diálogo nativo con Apagar / Reiniciar.

El servicio no lee el contenido de la pantalla; solo llama a la acción global de energía
cuando se lo piden.

## Clima, BTC y privacidad

## Clima, BTC y privacidad

- Ciudad fija: **Ciudad de Guatemala** (14.6349, -90.5069), zona `America/Guatemala`.
- Clima: **Open-Meteo** por HTTP, sin clave de API y sin permiso de ubicación.
- Cotización fija: precio del **BTC en USD** de **CoinGecko** por HTTPS.
- Se actualizan al abrir el launcher, al volver (si el dato está viejo: 30 min el clima,
  1 h el BTC), al tocar sus tarjetas y desde **Ajustes**.
- Caché local de ambos; sin red se sigue mostrando el último dato (BTC muestra su edad).
- **HTTPS en Android 5.1**: si el servidor exige una raíz que el almacén del sistema no
  tiene (hoy, Let's Encrypt/ISRG), la app reintenta con la cotización usando las **raíces
  ISRG X1/X2 empaquetadas** en el APK (`res/raw`). Es confianza **adicional**, solo para
  esta app; no se toca el almacén del sistema ni las demás apps.
- Permisos: `INTERNET`, `READ_CALENDAR` (lista de actividades en la tarjeta Agenda) y el
  permiso de binding del servicio de notificaciones
  (`BIND_NOTIFICATION_LISTENER_SERVICE`, que el sistema exige para escuchar notificaciones).
- Sin analítica, sin telemetría y sin servicios de Google.
- El clima va por HTTP (sin cifrar): es un dato público y no se envía nada del dispositivo.

### Contadores de notificaciones: límite de Android 5.1

`getActiveNotifications()` es API 23. En 5.1 el servicio solo ve lo que llega **mientras está
conectado**, así que al arrancar la consola los contadores empiezan en 0 hasta que llegue el
primer aviso. En el uso diario el servicio queda vivo en cuanto se concede el acceso, y por eso
el contador es fiable. Los avisos que ya estaban en la persiana antes de conceder el acceso no
se pueden recuperar.

## Usar NSP como launcher

El interruptor viene **apagado**. Al activarlo, NSP aparece como candidato a Inicio; Android
sigue decidiendo.

1. Abrir **Ajustes** (mosaico de engranaje).
2. En **Sistema**, pulsar **Usar NSP como inicio** y confirmar **Activar**.
3. Pulsar **Home**, elegir **NSP** y luego **Siempre**.
4. Para volver: desactivar el interruptor, usar **Ajustes de Inicio del sistema** o desinstalar
   NSP. El launcher de NVIDIA sigue instalado; si NSP falla, Android vuelve a él.

Cuando NSP es el inicio, **B y BACK no cierran la app**.

## Instalación rápida

1. Descarga el APK desde **Releases** (o compílalo, ver abajo).
2. Cópialo a una microSD/USB y ábrelo en la consola con un gestor de archivos.
3. Activa **Ajustes → Seguridad → Orígenes desconocidos** si Android lo pide.
4. Abre **NSP** desde el cajón del launcher de NVIDIA.

La primera vez Android 5.1 muestra su aviso de pantalla completa; pulsa OK una vez.

## Compilar

Requisitos:

- Windows 10/11 con PowerShell 5.1.
- Android SDK con **platform 31+**, **build-tools 32.0.0** y `platform-tools`.
- **JDK 8** (d8 de build-tools 32 usa `java.ext.dirs`, eliminado en Java 9+).

```powershell
git clone <TU_REPO>
cd NSP
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

Salida: `dist\NSPLauncher-1.3.apk` y `dist\NSPLauncher.apk` (versionCode 9).

El script autodetecta el SDK y el JDK 8; también acepta:

```powershell
.\build.ps1 -SdkRoot "C:\Android\Sdk" -JdkPath "C:\Java\jdk8"
```

Cadena: `aapt2` → `javac` → `d8` → `zipalign` → `apksigner`.
La primera vez genera `keystore\nsp.keystore` (firma de **depuración**, contraseña `android`).
Para publicar actualizaciones crea una clave de release y pásala con `-KeystorePath`,
`-KeyAlias`, `-StorePass` y `-KeyPass`.

Más detalle en [docs/BUILD.md](docs/BUILD.md). La numeración se explica en
[docs/VERSIONING.md](docs/VERSIONING.md) y los pendientes en [docs/ROADMAP.md](docs/ROADMAP.md).

## Instalar por USB

```powershell
.\install.ps1
```

o:

```powershell
adb install -r .\dist\NSPLauncher-1.3.apk
adb shell am start -n com.nsp.launcher/.MainActivity
```

## Estructura

```
app/src/main/
  AndroidManifest.xml
  java/com/nsp/launcher/
    MainActivity.java        inicio, foco 2D, rejilla, cabecera y chip BTC, menús
    BackgroundManager.java   imagen de la galería, recorte, EXIF, brillo y modos
    BatteryRingView.java     medidor circular de batería
    ClockView.java           reloj analógico de la cabecera
    Dashboard.java           tarjetas de batería, clima y memoria
    WeatherIconView.java     iconos de clima dibujados
    WeatherService.java      Open-Meteo + caché
    WeatherData.java
    BtcService.java          precio BTC (CoinGecko HTTPS) + reintento con raíces emp. + caché
    BtcData.java
    TrustedRoots.java        fábrica TLS con las raíces ISRG empaquetadas
    AgendaService.java       próximas actividades del calendario del sistema
    PowerMenuService.java    servicio de accesibilidad para el menú de energía
    SettingsActivity.java    Ajustes (incluye "Apagar consola…")
    HomeUtil.java            alias de launcher e inicio actual
    AppRepository.java      apps, categorías, favoritos, recientes, caché
    AppEntry.java
    IconStore.java          caché de iconos en memoria y disco
    NotificationService.java listener de notificaciones (solo conteo)
    NotificationStore.java   contadores por paquete y descarte
  res/drawable-nodpi/        fondos incluidos
  res/                       temas, textos, iconos
build.ps1                   compilación sin Gradle
install.ps1                 instalación por adb
docs/                       compilación, arquitectura, versionado y pendientes
```

Arquitectura y flujos en [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Verificado

- Emulador **Android 5.1.1 / API 22, 1280×720 @ 320 dpi** (misma resolución y densidad que la
  SHIELD Portable): rejilla de 5 columnas, foco 2D, panel de 5 tarjetas, anillo de batería,
  reloj de cabecera en sus tres estilos (analógico, digital con segundero y oculto con fecha
  grande), pista de controles que se desvanece, mosaico de avisos con lista y descarte, menú por
  app, ajustes de fondo unificados, clima, memoria, favoritos, recientes y uso como Inicio.
- Flujo completo de Inicio: activar → elegir NSP → Siempre; B/BACK no cierran; desactivar → vuelve
  NVIDIA.
- Instalación limpia, arranque en frío y con caché, sin crashes ni ANR.
- Pendiente: prueba prolongada en hardware real, fotos de cámara con EXIF y varias tarjetas SD.

## Limitaciones

- Los contadores de notificaciones no recuperan la persiana completa en API 22 (ver arriba).
- El clima es HTTP y la ciudad está fija; sin ubicación automática.
- Solo hay fondos incluidos; la foto propia es una copia local de la galería.
- Sin widgets de Google ni de terceros.
- Sin live tiles animados ni mosaicos de varios tamaños.
- Orden alfabético dentro de cada categoría.

## Publicar en GitHub

- `build/`, `dist/`, `backups/`, `keystore/`, `*.apk` y `*.idsig` están en `.gitignore`.
- Sube el APK como **Release**, no al repositorio.
- No subas tu keystore de release ni contraseñas.
- El APK de `dist` está firmado con la clave de depuración; sirve para probar, pero no para
  actualizaciones de larga duración.
- No hay claves de API, tokens ni datos personales en el código.

## Licencia

MIT. Ver [LICENSE](LICENSE).

NSP Launcher no está afiliado a NVIDIA. SHIELD es una marca de NVIDIA Corporation.
