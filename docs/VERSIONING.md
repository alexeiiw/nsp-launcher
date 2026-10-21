# Versionado

## Versión 1.0

- `versionName`: **1.0** (primer alcance completo).
- `versionCode`: **5**.
- Se instala encima de las compilaciones internas anteriores (versionCode 1–4) con la misma
  firma, sin desinstalar.

El nombre 1.0 no reinicia el contador de actualizaciones: Android usa `versionCode` para
decidir si un APK es más nuevo. El 5 garantiza que esta versión sustituya a las anteriores.

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

No se publicaron APKs con los nombres 0.x; son hitos de desarrollo para explicar la evolución.
Las compilaciones internas que se generaron con nombres 1.0–1.3 pertenecen a este periodo de
prototipos y quedan reemplazadas por la 1.0 pública.

## Reglas para las siguientes versiones

- Subir siempre `versionCode` en cada APK que se distribuya.
- Mantener `versionName` legible (`1.1`, `1.2`, ...).
- Firmar todas las actualizaciones con la **misma clave**; si cambia, Android no instala encima
  y hay que desinstalar (se pierden datos).
- No publicar `keystore/` ni contraseñas.
