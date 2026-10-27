# Pendientes — NSP Launcher

Estado actual: **1.4**. Incluye panel de dos líneas, lector RSS y corrección del servicio de energía.

## Pendientes

### 1. Auto-actualización del launcher (1.5)

- Consulta la última Release de GitHub una vez al día.
- Si hay una versión con `versionCode` mayor: avisa, descarga el APK, valida su firma y abre el instalador del sistema.
- Android exige confirmación del usuario; no hay instalación silenciosa sin root.
- Confirmar en la consola las raíces TLS de GitHub.

### 2. Búsqueda de Google

Lupa en la cabecera, junto a BTC/hora/Ajustes; no ocupa una tarjeta ni espacio de
Agenda, Noticias o Recientes. Al pulsarla abre un overlay a pantalla completa con:

- Campo `Buscar en Google…`, teclado Android (táctil, Bluetooth/USB) y A/Enter para buscar.
- B abre/cancela el overlay.
- Últimas cinco búsquedas guardadas localmente.
- La búsqueda abre Chrome o Firefox con `https://www.google.com/search?q=…`.

Depende de los certificados ya instalados en la consola para HTTPS de Google. Con mando sin
teclado físico escribir será lento; por eso queda como acceso opcional, no sustituye Noticias.

## Pendiente de hardware

- Activar **NSP · Menú de energía** en Ajustes → Accesibilidad y comprobar que abre el diálogo Apagar/Reiniciar.
- Probar RSS en la consola real: BBC Mundo, ESPN y Google News Guatemala/Fútbol.

## Ideas para cuando se quieran retomar

- Filas de más jugados y accesos rápidos.
- Tamaño de mosaico configurable.
- Tarjeta de red, diagnóstico de rendimiento, exportar/importar ajustes, búsqueda local, caché de iconos y reloj de 12 h.
