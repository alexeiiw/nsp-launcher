# Avance — NSP Launcher 1.6

Objetivo: `versionName 1.6`, `versionCode 12`. Primera versión pensada para llegar por el
actualizador (la 1.5 fue la última instalación manual).
Estado: **implementada y probada en emulador** (29/10/2026); pendiente de publicar.

## Bloques

- [x] 1. `CaseService`: consulta POST + `302` con un GET único, caché del reporte, slots
  08:00/20:00, indicador por estado y token enmascarado.
- [x] 2. `CaseActivity`: panel con el texto completo, scroll, marca de tiempo, estados y
  teclas **A** actualizar, **X** abrir enlace, **B** volver.
- [x] 3. Ajustes → CASE: cuadro multilínea, importar `case.txt`, Guardar / Probar / Borrar y
  línea de estado. Se quitan `Personalidad` (voz) y `Abrir CASE`.
- [x] 4. Cabecera `▮▮▮▮ CASE` como **un solo botón** (barras + título), indicador de estado
  y regreso del foco al salir del panel.
- [x] 5. `READ_EXTERNAL_STORAGE` y manifiesto al día.
- [x] 6. Documentación (CHANGELOG, README, ROADMAP, VERSIONING, avance).
- [x] 7. Compilar, firmar con la misma clave y respaldar la 1.5.
- [ ] 8. Publicar la Release `v1.6` y probar la actualización real desde la consola.

## Pruebas hechas (emulador API 22)

- Cabecera: `▮▮▮▮ CASE` a la izquierda con 4 barras azules (al día); el resto de la
  cabecera (BTC, fecha, reloj, búsqueda, engranaje) sigue igual.
- Botón: toque y **A** abren el panel; **B** vuelve y el título queda resaltado con el foco.
- Panel: reporte real (899 caracteres) completo, con saltos de línea, scroll con el mando y
  marca `Actualizado hoy 10:16`; al refrescar vuelve arriba y cambia la hora.
- Importar `case.txt` desde `Download` (montado en el emulador) → cuadro relleno con el aviso
  "Archivo importado; pulsa Guardar CASE"; **Guardar CASE** persiste url/token/canal/texto y
  deja el token enmascarado (`token ••••1234`).
- **Probar CASE**: "Consultando… puede tardar unos 20 s" y luego "CASE respondió · 899
  caracteres" sin guardar nada.
- Recuperación al arrancar: con la caché vieja, abrir el launcher lanzó **una** consulta y
  actualizó el reporte sin intervención.
- Estados del panel: sin configurar, sin reporte, desactualizado (`Sin actualizar desde ayer
  20:01`, 2 barras), error (`No se pudo actualizar: Token inválido · hoy 10:21`, 1 barra
  roja) y correcto. En error se conserva el último texto bueno.
- Enlace: con un texto que contiene una URL aparece `X abrir enlace` y **X** abrió el
  navegador del sistema en `https://example.com/informe?id=42`.
- Sin regresiones: BTC, fecha, reloj, búsqueda, chip de actualización, engranaje, RSS, Agenda
  y Avisos siguen funcionando.

## Notas técnicas

- `CaseService`: POST JSON `{canal, token, texto}` con `Content-Type: application/json`,
  sin redirección automática; al `302` se hace **un solo** GET manual a
  `googleusercontent` (la URL es de un solo uso). TLS con reintento de raíces empaquetadas.
- Tiempos: la consulta tarda ~18 s; timeout de lectura 120 s para no cortar en 3G.
- Caché en `SharedPreferences("nsp")`: `case_url`, `case_token`, `case_canal`, `case_texto`,
  `case_text`, `case_time`, `case_status`, `case_error`, `case_attempt`.
- Slots: el reporte vale si es posterior al último 08:00/20:00; si no, el arranque lanza una
  consulta y no se repite automáticamente hasta el slot siguiente (el botón A reintenta
  cuando quieras).
- El panel solo vuelve al principio cuando **cambia** el texto, para no perder la lectura al
  volver de otra pantalla.
- Incidente del día: el endpoint respondía `Token inválido` incluso con el cuerpo exacto;
  era el token del script, no la app. Con el token corregido respondió `ok:true`.

## Bitácora

- 2026-10-28: especificación cerrada (`docs/SPEC-CASE.md`), logo confirmado en 4 barras.
- 2026-10-29: `CaseService`, `CaseActivity`, Ajustes y cabecera implementados; APK 1.6
  (código 12) firmado con la misma clave; respaldo de la 1.5 en `backups\`.
- 2026-10-29: pruebas de panel, estados, enlace, importación y recuperación al arrancar.
- 2026-10-29: endpoint validado con el token correcto; reporte real visible en la consola
  emulada.

## Fuera de 1.6

- Ajustes del actualizador (activar, frecuencia, canal, buscar ahora, borrar APK) → 1.7.
- Prueba de importación en hardware: el AVD no trae almacenamiento externo; se validó
  montando un `tmpfs` en `/storage/sdcard`.
- Instalación real de la actualización, que solo se puede probar al publicar la 1.6.
