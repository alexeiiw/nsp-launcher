# Versionado

## Versión 1.6

- `versionName`: **1.6**.
- `versionCode`: **12**.
- Módulo CASE completo: cabecera `▮▮▮▮ CASE` como botón, panel con el reporte íntegro,
  configuración de URL/token en Ajustes e importación de `case.txt`, refresco 08:00/20:00 y
  caché. Se retira la voz y el overlay de la 1.5.
- Primera versión destinada a llegar por el actualizador; misma firma que la 1.5.
- La URL y el token del script **no** forman parte de la versión: se configuran en el aparato.

## Versión 1.5

- `versionName`: **1.5**.
- `versionCode`: **11**.
- RSS funcional, previa en la tarjeta, agenda legible, certificados GTS/DigiCert/Amazon,
  actualizador desde GitHub, CASE (LED, voz y overlay) y búsqueda de Google.
- Última instalación manual: a partir de la 1.6 las versiones llegan por el actualizador.

## Versión 1.4

- versionName: **1.4**.
- versionCode: **10**.
- Panel de dos lineas, lector RSS y correccion del servicio de energia.

## Versión 1.3

- `versionName`: **1.3** (BTC en la cabecera, tarjeta de Agenda, Ajustes como ⚙ en la
  cabecera y Apagar consola en Ajustes).
- `versionCode`: **9**.
- Se instala encima de la 1.2 (versionCode 7) con la misma firma, sin desinstalar.
- Nota: se salta el 8 a propósito; solo importa que el número crezca.

## Versión 1.2

- `versionName`: **1.2** (reloj conmutable en la cabecera y panel sin la tarjeta de fecha).
- `versionCode`: **7**.
- Se instala encima de la 1.1 (versionCode 6) con la misma firma, sin desinstalar.

## Versión 1.1

- `versionName`: **1.1** (anillo de batería, reloj en la cabecera, mosaico de Avisos, menú por
  app y ajustes de fondo unificados).
- `versionCode`: **6**.
- Se instala encima de la 1.0 (versionCode 5) con la misma firma, sin desinstalar.

## Versión 1.0

- `versionName`: **1.0** (primer alcance completo).
- `versionCode`: **5**.
- Se instala encima de las compilaciones internas anteriores (versionCode 1–4) con la misma
  firma, sin desinstalar.

El nombre de versión no reinicia el contador de actualizaciones: Android usa `versionCode` para
decidir si un APK es más nuevo. El 6 garantiza que esta versión sustituya a la 1.0 (5).

## Historial 0.x

Las entregas internas que llevaron a la 1.0 se documentan como hitos 0.1–0.5 en el
[CHANGELOG](../CHANGELOG.md):

| Hito | Contenido |
|---|---|
| 0.1 | Launcher Metro, foco, categorías y favoritos |
| 0.2 | Panel de fecha, reloj, clima y memoria |
| 0.3 | Ajustes, temas e interruptor de launcher |
| 0.4 | Caché, iconos progresivos, recientes e incremental |
| 0.5 | Rejilla de 5 columnas, badges y fondos incluidos |
| **1.0** | Selector de imagen de galería y tema automático |
| **1.1** | Anillo de batería, reloj en la cabecera, mosaico de Avisos, menú por app y ajustes unificados |
| **1.2** | Reloj analógico/digital/oculto, fecha grande al ocultarlo y panel de 5 tarjetas |
| **1.3** | BTC en la cabecera, Agenda (calendario), gear de Ajustes y Apagar consola |

No se publicaron APKs con los nombres 0.x; son hitos de desarrollo para explicar la evolución.
Las compilaciones internas que se generaron con nombres 1.0–1.3 pertenecen a este periodo de
prototipos y quedan reemplazadas por la 1.0 pública.

## Reglas para las siguientes versiones

- Subir siempre `versionCode` en cada APK que se distribuya.
- Mantener `versionName` legible (`1.1`, `1.2`, ...).
- Firmar todas las actualizaciones con la **misma clave**; si cambia, Android no instala encima
  y hay que desinstalar (se pierden datos).
- No publicar `keystore/` ni contraseñas.


