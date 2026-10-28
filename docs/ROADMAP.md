# Pendientes — NSP Launcher

Estado actual: **1.4** publicado. **1.5** en desarrollo: será la última instalación manual y la
primera versión con actualización automática.

## 1.5 — en desarrollo

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
- Chip `⬆` en la cabecera y fila de diagnóstico en Ajustes.
- Raíces GTS (Google) y DigiCert/Amazon (GitHub) empaquetadas como reintento TLS.

### CASE

- Voz en los textos del sistema e interruptor `CASE / neutral` en Ajustes.
- Banner de una línea, 28 dp, debajo de las dos filas del sistema.
- SELECT abre el detalle; se compacta el bloque superior para no quitar espacio a Recientes.

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

### Ajustes del actualizador

- Activar o desactivar el chequeo automático.
- Frecuencia: diaria o semanal.
- Canal: estable.
- Buscar ahora.
- Borrar el APK descargado.

## Pendiente de hardware

- Probar RSS en la consola real: BBC Mundo, ESPN y Google News Guatemala/Fútbol.
- Actualizador: primera verificación real cuando se publique la 1.6.
- Apagar consola: verificado en hardware (27/10/2026).

## Ideas para cuando se quieran retomar

- Filas de más jugados y accesos rápidos.
- Tamaño de mosaico configurable.
- Tarjeta de red, diagnóstico de rendimiento, exportar/importar ajustes, búsqueda local, caché de iconos y reloj de 12 h.
