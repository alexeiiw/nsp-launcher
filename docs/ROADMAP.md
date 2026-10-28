# Pendientes — NSP Launcher

Estado actual: **1.5** publicado (28/10/2026); última instalación manual. **1.6** siguiente:
webhook de CASE, banner y ajustes del actualizador.

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

## 1.6 — siguiente

### Webhook de CASE (POST)

El endpoint es un Apps Script con `doPost` que recibe `{canal, token, texto:"/hoy"}` y
responde JSON; la app debe seguir el `302` hacia `googleusercontent` con un GET manual.

- Refresco programado a las **08:00 y 20:00**; caché local del último reporte.
- `respuesta_plana` se muestra en el detalle; un resumen ocupa el banner.
- Requiere añadir `updated` y `banner` al JSON del script.
- Token exclusivo para la consola, guardado en SharedPreferences y enmascarado en Ajustes.
- Se descarta ETag: el endpoint es dinámico y la caché es por horario.
- Banner de 28 dp bajo las filas del sistema, con compactado del bloque superior; SELECT abre
  el detalle. Es la presencia visual de CASE.

### Ajustes del actualizador

- Activar o desactivar el chequeo automático.
- Frecuencia: diaria o semanal.
- Canal: estable.
- Buscar ahora.
- Borrar el APK descargado.

## Verificación en hardware (no es desarrollo pendiente)

Son cosas ya implementadas que solo se pueden confirmar en la consola; el emulador y esta PC
no reproducen su pila TLS ni su teclado.

- RSS: confirmar en la consola las cuatro categorías (BBC, ESPN, Google News
  Guatemala/Fútbol); el emulador ya las mostró, pero su pila TLS es distinta.
- Búsqueda: que el teclado en pantalla aparezca con el mando conectado y que Chrome o
  Firefox reciban la consulta.
- Actualizador: la primera verificación real de descarga e instalación ocurrirá cuando se
  publique la 1.6. La consulta, descarga, firma e instalador ya se probaron en el emulador.
- Apagar consola: verificado en hardware (27/10/2026).

## Ideas para cuando se quieran retomar

- Filas de más jugados y accesos rápidos.
- Tamaño de mosaico configurable.
- Tarjeta de red, diagnóstico de rendimiento, exportar/importar ajustes, búsqueda local, caché de iconos y reloj de 12 h.
