# Módulo CASE 1.6 — especificación

Estado: **análisis cerrado** (28/10/2026). No implementado. Este documento es la
referencia para construir la 1.6 en otra sesión; cualquier cambio de alcance se anota
aquí primero.

En la documentación la función se llama **Módulo CASE** (o “Consulta CASE”): una
funcionalidad adicional del APK, no el núcleo del launcher. En pantalla se llama `CASE`.

## Objetivo

Un botón en la portada (el título `CASE`) abre un panel con **el mensaje completo** que
devuelve el endpoint, o con el **enlace** si lo que devuelve es una URL. Se muestra tal
cual llega: sin parser, sin resumen, sin banner y sin voz.

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

### Texto o enlace

- Si la respuesta es texto (lo normal), se muestra completa y con scroll.
- Si la respuesta es **una URL** (`http://` o `https://`), el panel la muestra igual y
  ofrece `A abrir enlace`, que la abre con Chrome o Firefox usando `BrowserUtil` (el mismo
  camino de la búsqueda).
- Si el texto contiene una URL mezclada con prosa, se muestra tal cual y se ofrece
  `A abrir enlace` para la primera URL encontrada, sin quitar el texto.
- El enlace no se descarga ni se convierte: se abre en el navegador del sistema.
- Si el JSON trae `respuesta_plana` vacío pero `respuesta` con contenido, se usa `respuesta`.

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

## Cambios por archivo

### `MainActivity.java`

- Línea ~339: `title.setText("NSP")` → `title.setText("CASE")`; se mantiene 28sp y la
  tipografía fina. Añadir `padding` interno para que el resaltado del foco no se recorte.
- Guardar la referencia (`homeTitle`) y registrarla como **primer** elemento:
  `headerItems.add(0, homeTitle)`.
- `updateHeaderSelection()`: hoy solo escala; el título no debe escalar (movería el
  header). Caso especial: si `v == homeTitle`, cambiar el color al azul de acento cuando
  está seleccionado y volver al color normal al salir; el resto sigue con la escala 1.12.
- `openHeaderItem()`: añadir la rama `v == homeTitle → openCase()`.
- `buildCaseMark()`: quitar `setOnClickListener` y **no** añadirlo a `headerItems`; las
  barras dejan de ser botón y dejan de detener el recorrido. Quitar su rama de
  `openHeaderItem()`.
- `openCase()` se conserva igual: `startActivity(new Intent(this, CaseActivity.class))`.
- Al volver del panel (si se abrió desde el título), devolver el foco al título:
  `headerFocus = true; headerPos = 0; updateHeaderSelection();` en `onResume`, solo si se
  recuerda que la apertura vino del título.
- Indicador: tras cada consulta y en `onResume`, leer el estado de `CaseService` y llamar
  `caseMark.setLit(...)`.
- El bloque de fecha no se toca.

### `CaseView.java`

- Sin cambios funcionales: sigue dibujando 4 barras. Opcional: color de error para el
  estado rojo. No recibe clics.

### `CaseActivity.java` (reescritura)

- Se retira todo lo de la 1.5: voz, `case_voice`, líneas NSP/actualización/batería/clima/
  BTC/noticias y la tecla A de comprobar actualización.
- Cabecera: barras + `CASE` + `UNIDAD DE SOPORTE · NSP`; a la derecha, la marca de tiempo.
- Cuerpo: `ScrollView` con un `TextView` de 15sp; se rellena con `CaseService` (caché o
  respuesta).
- Pie: `A actualizar | B volver` (o `A abrir enlace` si hay URL; o `B cancelar` mientras
  consulta).
- `onKeyDown`: A ejecuta acción, B cierra. Flechas: scroll del `ScrollView`.
- `onResume`: pinta la caché; si el slot está vencido, lanza una consulta.

### `CaseService.java` (nuevo, patrón de `BtcService`)

- Preferencias en `SharedPreferences("nsp")`: `case_url`, `case_token`, `case_canal`,
  `case_texto`, `case_body`, `case_time`, `case_status`.
- `loadConfig`, `saveConfig`, `clearConfig`, `isConfigured`, `maskedToken`.
- `refresh(Context, Callback)`: POST + `302` manual + un `GET` + UTF-8, en un hilo; callback
  en el hilo principal.
- `isDue(Context)`: calcula el slot 08:00/20:00 con `Calendar` y compara con `case_time`.
- Caché: texto, hora y estado; se conserva el último bueno si falla.

### `SettingsActivity.java`

- Nuevo bloque “CASE” debajo del actualizador, con el mismo estilo de filas y foco.
- `EditText` multilínea (8–10 líneas, monoespaciado 12sp, `textMultiLine`), para pegar el
  bloque completo.
- Filas: **Guardar**, **Importar desde archivo**, **Probar**, **Borrar configuración**.
- Estado: `Configurado · token ••••1234 · última 28 oct 08:02` o `Sin configurar`.
- Importar: intenta `getExternalFilesDir(null)/case.txt` (sin permisos) y luego
  `Download/case.txt` (`Environment.getExternalStoragePublicDirectory`, requiere
  `READ_EXTERNAL_STORAGE`; en API 22 es permiso de instalación, en 23+ hay que pedirlo).
- Al guardar: parsea, valida, guarda, limpia el cuadro y muestra el token enmascarado.
- Se elimina la fila del interruptor de voz `CASE / neutral` y su código.

### `AndroidManifest.xml` / `strings.xml`

- `CaseActivity` ya tiene `label="CASE"`; se conserva.
- `app_name` sigue siendo **NSP**: no se renombra la app.
- Si se importa desde `Download/`: añadir `READ_EXTERNAL_STORAGE`.

### Documentación al implementar

- `docs/ROADMAP.md` (marcar 1.6), `docs/ARCHITECTURE.md` (módulo CASE y `CaseService`),
  `docs/CHANGELOG.md`, `docs/BUILD.md` (versionCode), `avance.md`.

## Orden de implementación

1. Respaldo del APK 1.5 en `backups/` antes de compilar.
2. `versionCode 12`, `versionName 1.6`, misma firma.
3. `CaseService` (red, caché, slots) y pruebas con el endpoint real.
4. `CaseActivity` (panel completo y estados).
5. `SettingsActivity` (cuadro, importar, probar, borrar; quitar voz).
6. `MainActivity` (título `CASE` botón, barras indicador, foco al volver).
7. Documentación y `avance.md`.
8. Compilar, firmar, probar en emulador API 22 y publicar.

## Pruebas en emulador (API 22)

- Sin configurar: el panel explica qué falta y lleva a Ajustes.
- Pegar el bloque de PowerShell: guarda URL/token y muestra `••••1234`.
- Importar desde `case.txt` en la carpeta de la app.
- Probar con token inválido (`Token inválido`) y con token bueno (~18 s).
- Panel: texto completo, saltos de línea, emojis (los nuevos como cuadro), scroll.
- Respuesta tipo URL (servidor local): se muestra y `A abrir enlace` abre el navegador.
- Cambiar la hora del emulador a 20:05 y reiniciar: una sola consulta de recuperación.
- Modo avión: conserva el último reporte y lo marca desactualizado.
- Foco: título `CASE` resaltado, A abre, B vuelve y el foco regresa al título.
- Regresión: BTC, fecha, búsqueda, chip de actualización, engranaje, RSS, Agenda.

## Riesgos anotados

- `CASE` es más ancho que `NSP`: comprobar en 1280×720 y 1920×1080 que BTC y fecha no se
  recorten; si pasa, reducir el título a 26sp o el margen izquierdo.
- El `GET` de `googleusercontent` es de un solo uso: cualquier reintento lo invalida.
- El APK no lleva credenciales; si se filtra el token, se rota en el script.

## Fuera de alcance en 1.6

- Banner de una línea y cualquier resumen.
- Voz de CASE y su interruptor.
- Parser por secciones, tarjetas por categoría o iconos por bloque.
- Notificaciones del reporte, historial de reportes, exportar/importar configuración.
- Fuente de emojis empaquetada.
- Cambios en RSS, Agenda, BTC, clima o actualizador (salvo los ajustes ya previstos).

## Detalles menores (se deciden al implementar)

- Colores finales del indicador por estado.
- Si el texto debe conservar el ancho completo o limitarse a ~900dp para lectura cómoda.
