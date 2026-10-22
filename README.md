# NSP Launcher 1.0

**NSP = NVIDIA Shield Portable.**

Launcher ligero para **NVIDIA SHIELD Portable** con Android 5.1.1 (API 22), inspirado en
Windows Phone. Java puro, sin Gradle y sin dependencias externas.

![Android 5.1](https://img.shields.io/badge/Android-5.1%20(API%2022)-green)
![minSdk](https://img.shields.io/badge/minSdk-22-blue)
![Java](https://img.shields.io/badge/Java-8-orange)

## Características

- **Rejilla de 5 columnas** con scroll vertical; las apps se reparten en filas y el resto baja.
- **Contadores de notificaciones** en cada mosaico (burbuja roja con el número).
- **Imagen de fondo personalizada** desde la galería, solo para el launcher, con ajuste
  automático de tema y velo según el brillo de la foto.
- **Panel de sistema** superior con Ajustes, fecha, reloj analógico, clima de Ciudad de
  Guatemala y memoria (RAM, interna y microSD).
- **RECIENTES**: los últimos 8 lanzamientos, actualizados al volver de una app.
- **Favoritos** y **categorías** (Juegos, Emuladores, Multimedia, Internet, Herramientas, Otros).
- Fondos incluidos para tema oscuro y claro, y opción de no usar fondo.
- Navegación completa con **cruceta, stick y botones**; también táctil.
- Foco visible con borde y escala; navegación 2D por filas y columnas.
- Tema oscuro/claro persistente.
- **Arranque con caché**: metadatos en disco + iconos en caché, pintado inmediato y escaneo
  en segundo plano.
- **Carga progresiva de iconos** y actualizaciones incrementales de favoritos/categorías.
- Recarga automática al instalar/desinstalar apps.
- **Interruptor para usar NSP como launcher**, apagado de fábrica. No borra el launcher original.

## Imagen de fondo desde la galería

En **Ajustes → APARIENCIA**:

- **Fondo**: Activado / Desactivado.
- **Imagen de fondo**: abre la galería del sistema. Al elegir una foto:
  - se copia al almacenamiento privado de NSP (no depende del archivo original);
  - se corrige la orientación EXIF;
  - se recorta al centro a 1280×720;
  - en modo **Automático**, se mide su brillo y se cambia el tema y el velo: foto oscura →
    tema oscuro; foto clara → tema claro.
- **Estilo del fondo**: Automático / Oscuro / Claro. Oscuro y Claro fuerzan ese tema y su velo.
- **Quitar imagen**: vuelve al fondo incluido o a ninguno.

La imagen **solo** se usa como fondo del launcher. No cambia el wallpaper del sistema, ni la
pantalla de bloqueo, ni la pantalla de Ajustes. Se guarda en la carpeta privada de la app y
se borra al desinstalar. No se sube a ningún sitio. No requiere permiso de almacenamiento:
se lee el URI que devuelve el selector y se copia al instante.

En la SHIELD, el selector del sistema se maneja con la pantalla táctil; la cruceta no siempre
mueve la selección dentro de la galería.

## Contadores de notificaciones

- Solo cuentan; **no** leen el texto ni guardan contenido.
- Requieren activar una vez **Acceso a notificaciones** en Ajustes del sistema
  (NSP → Ajustes → Notificaciones → Acceso a notificaciones).
- La burbuja aparece en todos los mosaicos de la app (RECIENTES, Favoritos y su categoría).
- Limitación de Android 5.1: `getActiveNotifications()` es API 23. Solo se cuentan las
  notificaciones recibidas mientras el servicio está conectado. Tras reiniciar, el contador
  empieza de cero hasta que llegue una nueva.
- Las notificaciones de la pantalla bloqueada siguen siendo de SystemUI.

## Nombres

- **Panel**: la franja de información superior.
- **Tarjeta**: cada módulo del panel (fecha, reloj, clima, memoria).
- **Mosaico**: cada app o acceso (incluye Ajustes).
- **Sección**: RECIENTES, Favoritos, Juegos, etc.

## Controles

| Botón | Acción |
|---|---|
| A / Enter | Abrir app o activar la fila enfocada |
| B | Volver en Ajustes; salir del launcher si no es el inicio |
| X | Añadir / quitar Favorito |
| Y | Mover la app a otra categoría |
| Cruceta / stick | Navegar por la rejilla (izquierda/derecha y arriba/abajo) |
| L1 / R1 | Sección anterior / siguiente |
| SELECT | Cambiar tema oscuro / claro |
| START | Recargar la lista de aplicaciones |

En Ajustes: cruceta para elegir, A para activar, B para volver.
En el diálogo de categoría: A confirma, B cancela.

## Clima y privacidad

- Ciudad fija: **Ciudad de Guatemala** (14.6349, -90.5069), zona `America/Guatemala`.
- Fuente: **Open-Meteo** por HTTP, sin clave de API y sin permiso de ubicación.
- Se actualiza al abrir, al volver si el dato tiene más de 30 minutos, al tocar la tarjeta y
  desde **Ajustes → Actualizar clima**.
- Caché local; sin red se sigue mostrando el último dato.
- Permisos: `INTERNET` y el permiso de binding del servicio de notificaciones
  (`BIND_NOTIFICATION_LISTENER_SERVICE`, exigido por el sistema para escuchar notificaciones).
- Sin analítica, sin telemetría y sin servicios de Google.
- El tráfico de clima va por HTTP (sin cifrar); es un dato público y no se envía nada del dispositivo.

## Usar NSP como launcher

El interruptor viene **apagado**. Al activarlo, NSP aparece como candidato a Inicio; Android
sigue decidiendo.

1. Abrir **Ajustes** (mosaico de engranaje).
2. En **Sistema**, pulsar **Usar NSP como inicio** y confirmar **Activar**.
3. Pulsar **Home**, elegir **NSP** y luego **Siempre**.
4. Para volver: desactivar el interruptor, usar **Ajustes de Inicio del sistema** o desinstalar NSP.
5. El launcher de NVIDIA sigue instalado; si NSP falla, Android vuelve a él.

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

Salida: `dist\NSPLauncher-1.0.apk` y `dist\NSPLauncher.apk` (versionCode 5).

El script autodetecta el SDK y el JDK 8; también acepta:

```powershell
.\build.ps1 -SdkRoot "C:\Android\Sdk" -JdkPath "C:\Java\jdk8"
```

Cadena: `aapt2` → `javac` → `d8` → `zipalign` → `apksigner`.
La primera vez genera `keystore\nsp.keystore` (firma de **depuración**, contraseña `android`).
Para publicar actualizaciones crea una clave de release y pásala con `-KeystorePath`,
`-KeyAlias`, `-StorePass` y `-KeyPass`.

Más detalle en [docs/BUILD.md](docs/BUILD.md). La numeración se explica en
[docs/VERSIONING.md](docs/VERSIONING.md) y los pendientes en
[docs/ROADMAP.md](docs/ROADMAP.md).

## Instalar por USB

```powershell
.\install.ps1
```

o:

```powershell
adb install -r .\dist\NSPLauncher-1.0.apk
adb shell am start -n com.nsp.launcher/.MainActivity
```

## Estructura

```
app/src/main/
  AndroidManifest.xml
  java/com/nsp/launcher/
    MainActivity.java       inicio, foco 2D, rejilla, controles, fondo
    BackgroundManager.java  selector, recorte, EXIF, brillo y preferencias
    Dashboard.java          tarjetas de fecha, reloj, clima y memoria
    ClockView.java          reloj analógico
    WeatherIconView.java    iconos de clima dibujados
    WeatherService.java     Open-Meteo + caché
    WeatherData.java
    SettingsActivity.java   Ajustes
    HomeUtil.java           alias de launcher e inicio actual
    AppRepository.java      apps, categorías, favoritos, recientes, caché
    AppEntry.java
    IconStore.java          caché de iconos en memoria y disco
    NotificationService.java listener de notificaciones (solo conteo)
    NotificationStore.java   contadores por paquete + notificación a la UI
  res/drawable-nodpi/        fondos incluidos
  res/                       temas, textos, iconos
build.ps1                   compilación sin Gradle
install.ps1                 instalación por adb
docs/                       compilación, arquitectura, versionado y hoja de ruta
```

Arquitectura y flujos en [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
Los pendientes y siguientes mejoras están en [docs/ROADMAP.md](docs/ROADMAP.md).

## Verificado

- Emulador **Android 5.1.1 / API 22, 1280×720 @ 320 dpi** (misma resolución y densidad que la
  SHIELD Portable): panel, clima, memoria, rejilla de 5 columnas, foco 2D, RECIENTES,
  favoritos, cambio de categoría en sitio, tema claro/oscuro, fondos incluidos y contadores.
- Selector de galería del sistema: imagen personalizada, recorte, persistencia, tema automático
  (foto clara → claro; foto oscura → oscuro), cambio manual de estilo y eliminación.
- Flujo completo de Inicio: activar → elegir NSP → Siempre; B/BACK no cierran; desactivar → vuelve NVIDIA.
- Sin crashes ni ANR en las pruebas.
- Pendiente: prueba prolongada en hardware real, fotos con EXIF de cámaras reales y varias tarjetas SD.

## Limitaciones

- El selector de galería necesita una app de galería/archivos que responda a `ACTION_GET_CONTENT`;
  en la SHIELD se usa la pantalla táctil.
- No hay editor de recorte: la foto se recorta al centro.
- Los contadores de notificaciones no recuperan la persiana completa en API 22.
- El clima es HTTP y la ciudad está fija; sin ubicación automática.
- Sin widgets de Google ni de terceros.
- Sin live tiles animados ni mosaicos de varios tamaños.
- Orden alfabético dentro de cada categoría.

## Publicar en GitHub

- `build/`, `dist/`, `keystore/`, `*.apk` y `*.idsig` están en `.gitignore`.
- Sube el APK como **Release**, no al repositorio.
- No subas tu keystore de release ni contraseñas.
- El APK de `dist` está firmado con la clave de depuración; sirve para probar, pero no para
  actualizaciones de larga duración.
- No hay claves de API, tokens ni datos personales en el código.

## Licencia

MIT. Ver [LICENSE](LICENSE).

NSP Launcher no está afiliado a NVIDIA. SHIELD es una marca de NVIDIA Corporation.
