# Pendientes — NSP Launcher

**NSP = NVIDIA Shield Portable.**

Contexto de diseño: consola de 5" con 1280×720 (640×360 dp), Android 5.1.1 (API 22),
Tegra 4, 2 GB de RAM, mando integrado (cruceta, sticks analógicos, A/B/X/Y, L1/R1, START,
SELECT) y pantalla táctil. Todo lo de esta lista está pensado para esa pantalla, ese mando y
ese uso: jugar, escribir rápido y no tocar el launcher más de lo necesario.

Estado actual: **1.1** es la base funcional y cerró los cuatro puntos que estaban abiertos
(mosaico de Avisos, fondo y tema unificados, menú por app y anillo de batería).

**No hay pendientes abiertos.**

## Ideas para cuando se quieran retomar

No están planificadas ni compromise; se anotan por si en algún momento hay ganas:

- Filas de "más jugados" y de accesos rápidos dentro de la fila SISTEMA.
- Tamaño de mosaico configurable (4, 5 o 6 columnas) y mosaicos 2×2.
- Tarjeta de red (Wi-Fi, SSID y señal).
- Diagnóstico de rendimiento con botón "copiar" para reportar issues con datos reales.
- Exportar e importar ajustes (favoritos, categorías) en JSON.
- Autoactualizador desde GitHub Releases.
- Búsqueda local de apps.
- Gestión del tamaño de la caché de iconos.
- Reloj de 12 h y formato de fecha corto en la cabecera.

Reglas de trabajo: un punto por versión, `versionCode` siempre creciente, **siempre la misma
clave de firma**, pruebas en el emulador API 22 (1280×720 @ 320 dpi) y lo que toque la consola
real se anota como "pendiente de hardware".
