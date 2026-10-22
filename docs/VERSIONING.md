# Versionado

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

No se publicaron APKs con los nombres 0.x; son hitos de desarrollo para explicar la evolución.
Las compilaciones internas que se generaron con nombres 1.0–1.3 pertenecen a este periodo de
prototipos y quedan reemplazadas por la 1.0 pública.

## Reglas para las siguientes versiones

- Subir siempre `versionCode` en cada APK que se distribuya.
- Mantener `versionName` legible (`1.1`, `1.2`, ...).
- Firmar todas las actualizaciones con la **misma clave**; si cambia, Android no instala encima
  y hay que desinstalar (se pierden datos).
- No publicar `keystore/` ni contraseñas.
