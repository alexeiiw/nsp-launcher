# Pendientes — NSP Launcher

**NSP = NVIDIA Shield Portable.**

Contexto de diseño: consola de 5" con 1280×720 (640×360 dp), Android 5.1.1 (API 22),
Tegra 4, 2 GB de RAM, mando integrado (cruceta, sticks analógicos, A/B/X/Y, L1/R1, START,
SELECT) y pantalla táctil. Todo lo de esta lista está pensado para esa pantalla, ese mando y
ese uso: jugar, escribir rápido y no tocar el launcher más de lo necesario.

Estado actual: **1.3** es la base funcional (la 1.2 cerró el mosaico de Avisos, fondo y tema
unificados, menú por app y anillo de batería; la 1.3 añadió el chip BTC, la tarjeta de
Agenda, el gear de Ajustes en la cabecera y el Apagar consola).

Pendiente de hardware (de la 1.3): probar en la consola real el Apagar consola (activación
del servicio de accesibilidad), el chip BTC con red real y la tarjeta Agenda con el
calendario del usuario.

## Pendientes

### 1. Mini lector de RSS (1.4)

El botón **NVIDIA** (hoy sin uso) abre una segunda Activity "Noticias", estilo Metro:
12 titulares en lista, ARRIBA/ABAJO para moverse, **A** abre la nota en el navegador
(con las raíces de Let's Encrypt ya instaladas en la consola) y **B** vuelve.

- Fuentes verificadas en línea (26/10/2026): BBC Mundo y ESPN (`RSS 2.0`, 200 OK) y
  **Google News RSS** por consulta — la llave maestra para lo local: titulares de Prensa
  Libre, Soy502, etc. aunque sus feeds propios fallen (Prensa Libre 404).
- Parseo con `XmlPullParser` en hilo aparte; caché con edad (refresco al abrir, máx. cada
  30 min); **sin** servicios de fondo.
- TLS: Google News sirve con raíces **GTS** (Google Trust Services), tampoco en el almacén
  del 2015: añadir `gtsrootr1/r4` (`pki.goog`) al `TrustedRoots` con el patrón de doble
  intento de la 1.3, o instalarlas como cert. de usuario junto a las ISRG.
- Ajustes: fuentes activas y palabras clave (fútbol, Guatemala, "Liga Nacional"...).

Pendiente de hardware: verificar en la consola real las cadenas de certificados de cada
fuente (desde el PC de desarrollo no se ven: el filtro ESET de la red las intercepta).

### 2. Auto-actualización del launcher

La 1.3 abrió la puerta al HTTPS moderno; un auto-updater realista en Android 5.1 sin Play:

- La app consulta la **Release más reciente de GitHub** (`/releases/latest`, sin token)
  al arrancar (1×/día; el límite de la API anónima es 60/h por IP).
- Compara `versionCode`; si hay una nueva, avisa en la cabecera y, si el usuario acepta,
  descarga el APK por HTTPS y lanza el instalador del sistema. Instalar exige una
  confirmación del usuario; **la instalación silenciosa no existe sin root**, así que el
  "automático" es aviso + descarga + una tecla de confirmación.
- La revisión de firma idéntica y el `versionCode` creciente ya son reglas del proyecto.
- TLS de GitHub (`api.github.com` + `objects.githubusercontent.com`): confirmar la cadena
  en la consola; si falta alguna raíz, se empaqueta al `TrustedRoots` igual que las ISRG.
- Cuidados: conservar el APK hasta que la instalación termine y limpiar la copia después.

## Ideas para cuando se quieran retomar

No están planificadas ni comprometidas; se anotan por si en algún momento hay ganas:

- Filas de "más jugados" y de accesos rápidos dentro de la fila SISTEMA.
- Tamaño de mosaico configurable (4, 5 o 6 columnas) y mosaicos 2×2.
- Tarjeta de red (Wi-Fi, SSID y señal).
- Diagnóstico de rendimiento con botón "copiar" para reportar issues con datos reales.
- Exportar e importar ajustes (favoritos, categorías) en JSON.
- Búsqueda local de apps.
- Gestión del tamaño de la caché de iconos.
- Reloj de 12 h y formato de fecha corto en la cabecera.

Reglas de trabajo: un punto por versión, `versionCode` siempre creciente, **siempre la misma
clave de firma**, pruebas en el emulador API 22 (1280×720 @ 320 dpi) y lo que toque la consola
real se anota como "pendiente de hardware".
