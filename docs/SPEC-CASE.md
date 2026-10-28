# CASE 1.6 — especificación

Estado: **análisis cerrado** (28/10/2026). No implementado. Este documento es la
referencia para construir la 1.6; cualquier cambio de alcance se anota aquí primero.

## Objetivo

Un botón en la pantalla principal abre un panel con **el mensaje completo** que
devuelve el endpoint de CASE. El mensaje se muestra tal cual llega: sin parser, sin
resumen, sin banner y sin voz.

## Decisiones cerradas

- Texto completo y genérico: se muestra lo que venga, sin interpretar secciones.
- Sin banner, sin voz y sin resumen de una línea. Se retiran de la 1.5.
- Emojis: no se quitan ni se sustituyen. En Android 5.1 los que no estén en su fuente
  se verán como cuadro; se acepta para esta versión.
- URL y token configurables desde Ajustes. Nunca en el código, el APK ni el repo.
- Refresco a las 08:00 y 20:00, con recuperación al arrancar si se perdió el de las 20:00.
- El nombre visible es **CASE**. En documentación se llama “consulta CASE”; no es un
  webhook push (la app consulta, el script responde).
- Se conserva el estilo visual de CASE (barras, tipografía fina, azul de acento).

## Punto de entrada

- El título de la cabecera, hoy `NSP` (arriba a la izquierda, 28sp), pasa a decir **`CASE`**.
- Ese título es el botón que abre el panel. Sustituye al overlay de estado de la 1.5.
- Con mando: se resalta al enfocar (color de acento) y **A** lo abre; al volver, el foco
  regresa a él.
- Las cuatro barras de la derecha dejan de ser botón: quedan **solo** como indicador de
  estado (ver “Estados del indicador”).
- El bloque de fecha **no** es botón: se descartó esa opción.
- El nombre de la app no cambia: icono, lista de apps, Ajustes y APK siguen siendo **NSP**.
  Solo cambia el título de la portada.

## Panel

### Estructura

1. Cabecera: barras + `CASE` + `UNIDAD DE SOPORTE · NSP`; a la derecha, la marca de tiempo
   (`Actualizado hoy 08:02`, `Actualizado ayer 20:01`, `Sin actualizar`).
2. Cuerpo: `ScrollView` con el texto íntegro. 15sp, blanco, interlineado normal, márgenes
   laterales de 32dp. Se respetan saltos de línea y emojis. Sin Markdown, sin columnas.
3. Pie: `A actualizar  |  B volver`; mientras consulta, `Consultando…` y el botón pasa a
   `B cancelar`.

### Estados

| Estado | Qué se ve |
|---|---|
| Sin configurar | “Falta configurar CASE en Ajustes” + atajo para abrir Ajustes |
| Sin reporte | “Sin reporte todavía” + `A consultar` |
| Consultando | Último texto + “Consultando… (puede tardar ~20 s)” |
| Correcto | Texto + “Actualizado hoy 08:02” |
| Error | Último texto bueno + “No se pudo actualizar: …” y hora del intento |
| Desactualizado | Texto + “Sin actualizar desde ayer 20:01” (sin error de red) |

- Si hay texto en caché, siempre se muestra algo: nunca una pantalla vacía.
- El scroll con arriba/abajo del mando; no hay foco interno en el texto.
- Girar la pantalla no aplica (landscape fijo).

## Ajustes — Configuración CASE

Bloque nuevo en Ajustes, debajo del actualizador:

- **Cuadro multilínea** (8–10 líneas visibles) para pegar el bloque completo.
- **Importar desde archivo**: lee `Download/case.txt` y, si no existe, `case.txt` en la
  carpeta privada de la app; rellena el cuadro. Pensado para no pegar con el mando.
- **Probar**: ejecuta la consulta real y muestra `OK · 1.8 KB · 18 s` o el error. No
  guarda nada hasta que la prueba sale bien (o hasta pulsar Guardar).
- **Guardar**: extrae los valores, valida y escribe en `SharedPreferences("nsp")`.
- **Borrar configuración**: elimina URL, token y caché del reporte.
- El token se muestra enmascarado (`••••1234`); nunca completo en pantalla ni en logcat.
- El cuadro se limpia al guardar: no queda el token visible al volver a Ajustes.

## Formato aceptado al pegar

Se acepta el bloque de PowerShell tal cual, sin editarlo:

```powershell
$url   = "https://script.google.com/macros/s/…/exec"
$token = "1234"
$body  = @{ canal = "terminal"; token = $token; texto = "/hoy" } | ConvertTo-Json -Compress
```

Extracción tolerante, en este orden:

1. **URL**: primera cadena que empiece por `https://` y contenga `script.google.com`
   (si no, cualquier `https://…/exec`).
2. **Token**: valor entre comillas después de `$token` o `token =`; si no aparece, el del
   `body`.
3. **canal**: valor de `canal = "…"`; por defecto `terminal`.
4. **texto**: valor de `texto = "…"`; por defecto `/hoy`.

También se acepta un formato corto de dos líneas (`url=…`, `token=…`) y un JSON con las
mismas claves. Si falta URL o token: “Falta la URL o el token” y no se guarda nada.

## Red

- `POST` con `Content-Type: application/json; charset=utf-8` y cuerpo
  `{"canal":…,"token":…,"texto":…}`.
- **No** seguir el `302` automáticamente: leer `Location` y hacer **un** `GET` a
  `script.googleusercontent.com`.
- Ese `GET` es de un solo uso: **no reintentar**. Un segundo intento devuelve vacío.
- Leer el cuerpo como UTF-8 explícito (≈1.8 KB); sin esto se rompen acentos y emojis.
- `User-Agent: NSPLauncher/1.6`.
- TLS: reutilizar `TrustedRoots` y el reintento con raíces GTS/DigiCert de 1.5.
- Todo en segundo plano con timeout de 120 s; la interfaz nunca se bloquea.
- Reintento solo si falla **antes** del `GET` (conexión); nunca después.

### Respuestas

- `200` + JSON `{ok, respuesta_plana, respuesta}` → mostrar `respuesta_plana`; si falta,
  `respuesta`.
- `200` + `{"ok":false,"error":"Token inválido"}` → mostrar ese error.
- `200` + HTML (“La secuencia de comandos se completó pero no mostró ningún valor”) →
  “El script no devolvió datos”.
- Cuerpo vacío o timeout → “Sin respuesta del script”.

## Caché y refresco

- Se guarda: texto mostrado, hora de la consulta, resultado (ok/error) y duración.
- Slots **08:00** y **20:00** hora local.
- Al arrancar: si el slot más reciente ya pasó y no se refrescó, se hace **una** consulta
  (una sola, no una por cada slot perdido).
- Con la app abierta a la hora exacta, se refresca al entrar en el slot.
- Nunca más de un refresco por slot.
- Sin red: se conserva el último bueno, se marca desactualizado y se reintenta en el
  siguiente slot o arranque.
- No hay ETag ni `If-Modified-Since`: el contenido cambia en cada llamada (cita y
  recomendación son aleatorias).

## Emojis

- Se muestran tal cual: sin filtrar, sin reemplazar por texto, sin listas blancas.
- Android 5.1 (API 22) no conoce emojis recientes: saldrán como cuadro. Aceptado.
- No se empaqueta fuente de emojis en 1.6; si molesta más adelante, se valora un
  subconjunto de NotoColorEmoji.
- UTF-8 de extremo a extremo; nunca `\uFFFD` ni `ISO-8859-1`.

## Estados del indicador (barras de la cabecera)

- 4 barras azules: reporte del slot actual, correcto.
- 2 barras: desactualizado (se perdió un slot o falló la red).
- 1 barra ámbar/roja: error repetido o configuración inválida.
- 0 barras: sin configurar.
- Los colores exactos se ajustan al tema claro/oscuro durante la implementación.

## Seguridad

- URL y token solo en `SharedPreferences` privadas (`nsp`); nunca en `strings.xml`,
  `BuildConfig`, gradle, docs ni commits.
- `.gitignore`: añadir `case.txt`, `*.local` y cualquier archivo de prueba con credenciales.
- Nada de credenciales en logcat, toasts ni capturas.
- El APK publicado puede subirse a GitHub: no lleva credenciales dentro.
- Riesgo aceptado: quien tenga acceso físico o root al dispositivo puede leer las
  preferencias. Si el token se filtra, se rota en el script y se vuelve a pegar.
- El endpoint probado el 28/10/2026 **no** se escribe en este documento ni en el repo.

## Navegación y accesibilidad

- **A**: abrir/actualizar. **B**: volver/cancelar. Arriba/abajo: scroll.
- Textos de 15sp mínimo en el cuerpo y 9–10sp en las etiquetas, como el resto de CASE.
- El recorrido de la cabecera sigue saltando los elementos `GONE` (chip de actualización).

## Criterios de aceptación

1. Sin configuración, el panel explica qué falta y lleva a Ajustes.
2. Pegar el bloque de PowerShell y guardar deja URL y token correctos, token enmascarado.
3. “Importar desde archivo” funciona con `Download/case.txt` sin teclado.
4. “Probar” devuelve el texto completo en menos de 30 s y detecta token inválido.
5. El panel muestra el mensaje íntegro, con saltos de línea y emojis, y hace scroll.
6. El título `CASE` de la cabecera abre el panel y el foco vuelve a él al salir.
7. Las barras de la derecha cambian de color/segmentos según el estado y no abren nada.
8. Reiniciar la app después de las 20:00 lanza una sola consulta de recuperación.
9. Sin red, el panel conserva el último reporte y lo marca como desactualizado.
10. `adb shell run-as` no es necesario para nada; el APK no contiene el token.
11. `git grep` del repo no encuentra ni la URL ni el token.

## Archivos previstos

- `CaseActivity.java`: reescritura como panel del reporte (se retira estado/voz).
- `CaseService.java` (nuevo): POST, 302 manual, GET único, UTF-8, caché, slots.
- `SettingsActivity.java`: bloque “Configuración CASE” con cuadro, importar, probar, borrar.
- `MainActivity.java`: título `CASE` como botón; barras solo como indicador; el bloque de
  fecha no cambia.
- `strings.xml` / `AndroidManifest.xml`: etiquetas y textos.
- `docs/ROADMAP.md`, `docs/ARCHITECTURE.md`, `avance.md`: al implementar.

## Fuera de alcance en 1.6

- Banner de una línea y cualquier resumen.
- Voz de CASE y su interruptor.
- Parser por secciones, tarjetas por categoría o iconos por bloque.
- Notificaciones del reporte, historial de reportes, exportar/importar configuración.
- Fuente de emojis empaquetada.
- Cambios en RSS, Agenda, BTC, clima o actualizador (salvo los ajustes ya previstos).

## Pendientes de confirmar

- Colores finales del indicador por estado.
- Si el texto debe conservar el ancho completo o limitarse a ~900dp para lectura cómoda.
