# Pendientes — NSP Launcher

Estado actual: **1.6** implementado y probado en emulador (29/10/2026), pendiente de publicar.
Incluye la consulta CASE (texto completo), la configuración de URL/token y la retirada de la
voz. Los ajustes del actualizador pasan a la 1.7.

## 1.5 — publicado (28/10/2026)

Reparación de la 1.4, actualizador, CASE y búsqueda. Todo en un solo APK (`versionCode 11`).

### Reparaciones

- RSS: limpiar la etiqueta al cerrar, aceptar CDATA, `trim()` y descartar texto fuera de `item`.
- Error visible cuando falla la red (hoy se ignora la excepción).
- Foco visible en las filas y **A** abre el detalle.
- Tarjeta Noticias con previa de dos titulares.
- Agenda: título 9sp, eventos 12–13sp, dos o tres eventos.

### Actualizador

- Consulta `releases/latest` de GitHub una vez al día.
- Compara versión numérica; descarga, valida paquete, firma y `versionCode`, y abre el instalador.
- Android exige confirmación; sin root no hay instalación silenciosa.
- Chip `↑` en la cabecera y fila de diagnóstico en Ajustes.
- Raíces GTS (Google) y DigiCert/Amazon (GitHub) empaquetadas como reintento TLS.

### CASE (sin banner)

- Voz en los textos del sistema e interruptor `CASE / neutral` en Ajustes.
- El banner de una línea y su presencia visual pasan a la 1.6, junto con el webhook.
- Sin banner no se toca el layout: Recientes conserva el espacio actual.

### Búsqueda de Google

Lupa en la cabecera, junto a BTC/hora/Ajustes; no ocupa una tarjeta ni espacio de
Agenda, Noticias o Recientes. Al pulsarla abre un overlay a pantalla completa con:

- Campo `Buscar en Google…`, teclado Android (táctil, Bluetooth/USB) y A/Enter para buscar.
- B abre/cancela el overlay.
- Últimas cinco búsquedas guardadas localmente.
- La búsqueda abre Chrome o Firefox con `https://www.google.com/search?q=…`.

Con mando sin teclado físico escribir será lento; por eso queda como acceso opcional, no
sustituye Noticias.

## 1.6 — implementado (29/10/2026)

### Consulta CASE (texto completo)

El endpoint es un Apps Script con `doPost` que recibe `{canal, token, texto:"/hoy"}` y
responde JSON; la app sigue el `302` hacia `googleusercontent` con un GET manual.
Especificación completa en [`SPEC-CASE.md`](SPEC-CASE.md).

- [x] La cabecera izquierda pasa a ser `▮▮▮▮ CASE`: el logo de barras se mueve junto al
  título y el conjunto es el botón que abre el panel. La derecha queda BTC, fecha, búsqueda,
  chip y engranaje. El bloque de fecha no cambia.
- [x] La app sigue llamándose NSP (icono, lista de apps, Ajustes, APK).
- [x] El panel muestra **el mensaje completo** o el enlace que devuelva, sin parser, sin
  resumen y sin voz.
- [x] Se retiran el banner y la voz de la 1.5. El script no necesita `updated` ni `banner`.
- [x] En Ajustes se quitan las filas `Personalidad` y `Abrir CASE`; en su lugar va el cuadro
  multilínea (o `Download/case.txt`) para pegar URL y token. Nunca van en el código, el APK
  ni el repo. Token enmascarado.
- [x] Refresco a las **08:00 y 20:00** con recuperación al arrancar; caché del último reporte.
- [x] Emojis sin filtrar; en Android 5.1 los recientes salen como cuadro (aceptado).
- [x] Se descarta ETag: el endpoint es dinámico y la caché es por horario.
- [x] Indicador de barras por estado (4 al día, 2 desactualizado, 1 rojo error, 0 sin
  configurar) y estados del panel (sin configurar, sin reporte, consultando, error y
  desactualizado).

## 1.7 — pendiente

### Ajustes del actualizador

- Activar o desactivar el chequeo automático.
- Frecuencia: diaria o semanal.
- Canal: estable.
- Buscar ahora.
- Borrar el APK descargado.

## Verificación en hardware (no es desarrollo pendiente)

Son cosas ya implementadas que solo se pueden confirmar en la consola; el emulador y esta PC
no reproducen su pila TLS ni su teclado.

- RSS: **verificado** en la consola (28/10/2026), cuatro categorías.
- Búsqueda: **verificada** en la consola (28/10/2026), con mando.
- Agenda: **verificada** en la consola (28/10/2026).
- Actualizador: la consulta funciona (dijo “última versión” el 28/10/2026). La primera
  descarga e instalación real ocurrirá cuando se publique la 1.6.
- Apagar consola: verificado en hardware (27/10/2026).

## Ideas para cuando se quieran retomar

- Filas de más jugados y accesos rápidos.
- Tamaño de mosaico configurable.
- Tarjeta de red, diagnóstico de rendimiento, exportar/importar ajustes, búsqueda local, caché de iconos y reloj de 12 h.
