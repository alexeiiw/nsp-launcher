# Changelog

## 1.6

- **Módulo CASE**: la cabecera pasa de `NSP` a `▮▮▮▮ CASE`. Las barras van pegadas al
  título y todo el grupo es **un solo botón** que abre el panel; al enfocar se resalta y
  **A** lo abre, **B** vuelve y el foco regresa al título. El nombre de la app, el icono,
  Ajustes y el APK siguen siendo **NSP**.
- **Panel CASE**: muestra **el reporte completo** que devuelve el script, con scroll y sin
  parser ni Markdown (emojis incluidos; los que Android 5.1 no conoce se ven como cuadro).
  Cabecera con barras, `CASE`, `UNIDAD DE SOPORTE · NSP` y la marca de tiempo; pie con
  `A actualizar | X abrir enlace | B volver`.
- **Estados**: sin configurar, sin reporte, consultando, correcto, error (conserva el último
  texto bueno y la hora del intento) y desactualizado (`Sin actualizar desde ayer 20:01`).
  Las barras indican: 4 azules al día, 2 desactualizado, 1 roja error, 0 sin configurar.
- **Refresco 08:00 / 20:00** con una sola consulta de recuperación al arrancar; el último
  reporte queda en caché y sin red se sigue mostrando. Un solo intento automático por slot;
  **A** en el panel permite reintentar cuando quieras.
- **Ajustes → CASE**: se quitan las filas `Personalidad` (voz) y `Abrir CASE`. En su lugar,
  cuadro multilínea para pegar el bloque con la URL y el token, **Importar desde archivo**
  (`Download/case.txt` o la carpeta de la app), **Guardar CASE**, **Probar CASE** y **Borrar
  configuración**, con línea de estado (`host · token ••••1234 · Actualizado hoy 08:02`).
- **Seguridad**: la URL y el token **no están en el código, el APK, la documentación ni el
  repositorio**; se guardan en `SharedPreferences` del aparato y el token se muestra
  enmascarado. La consulta es HTTPS con reintento de raíces empaquetadas y un único GET al
  destino de un solo uso del script.
- **Sin voz**: se retira la voz del robot y el overlay de la 1.5; CASE ya no habla.
- versionCode 12.

## 1.5

- **RSS arreglado**: el parser limpia la etiqueta al cerrar, acepta CDATA y descarta texto
  fuera de `item`; las filas vuelven a mostrar titulares. Foco visible, **A** abre el detalle,
  **B** vuelve y los errores de red se muestran en pantalla.
- **Previa en la tarjeta Noticias**: dos titulares, BBC Mundo y ESPN, con caché de 10 minutos.
- **Agenda legible**: título 9sp y eventos 12.5sp.
- **Versión y actualización visibles**: Ajustes muestra la versión y el estado; chip `↑1.6`
  en la cabecera cuando hay una Release nueva.
- **Certificados**: GTS Root R1/R3/R4, DigiCert Global Root G2 y Amazon Root CA 1 empaquetados
  como reintento TLS (Google News y GitHub).
- **Actualizador**: consulta la última Release una vez al día, descarga, valida paquete, firma
  y `versionCode`, y abre el instalador. Android pide confirmación; sin root no hay instalación
  silenciosa. En Android 5.1 el APK se entrega con `file://` desde la carpeta externa de la app
  (el instalador de esa versión no acepta `content://`); en Android 7+ se usa `content://`.
- **CASE**: cuatro LED en la cabecera, overlay con estado del sistema y voz del robot;
  interruptor CASE/neutral en Ajustes.
- **Búsqueda de Google**: lupa en la cabecera, overlay con teclado virtual, historial de cinco
  búsquedas y apertura en Chrome o Firefox.
- versionCode 11.

## 1.4

- Panel SISTEMA de dos líneas: Avisos/Batería/Clima/Memoria y Agenda/Noticias.
- Lector RSS: BBC Mundo, ESPN, Google News Guatemala y Fútbol; botón NVIDIA abre Noticias.
- Corrección del servicio de accesibilidad para Menú de energía.
- versionCode 10.

## 1.3

- **Ajustes suben a la cabecera**: icono ⚙ en la esquina, alcanzable con ARRIBA desde la
  fila SISTEMA (y B para volver a los mosaicos). En la fila queda espacio para una tarjeta
  nueva sin robar altura a la rejilla.
- **Chip BTC en la cabecera**: `BTC $68,214` con la edad de la cotización. Fuente:
  CoinGecko por HTTPS. Si el almacén de certificados del sistema (de 2015) no conoce
  Let's Encrypt, reintenta con las **raíces ISRG X1/X2 empaquetadas** en el APK — funciona
  también en Android 5.1 sin tocar el sistema. Se actualiza al abrir y al hacer clic.
- **Tarjeta AGENDA** en la fila del panel, en naranja Metro: las 2 próximas actividades
  del calendario del sistema (hora + título, "Todo el día"), toca para abrir la app de
  calendario. Sin eventos muestra "Sin actividades". Permiso `READ_CALENDAR`.
- **Apagar consola** en Ajustes → SISTEMA: abre el diálogo del sistema (Apagar/Reiniciar)
  vía un servicio de accesibilidad mínimo (`NSP · Menú de energía`), que hay que activar
  una vez en Ajustes → Accesibilidad. Sin root.
- Versión 1.3 (versionCode 9).

## 1.2

- **Un solo reloj en la cabecera**, elegible en Ajustes: **Analógico / Digital / Oculto**.
- La **fecha larga** (día de la semana, día y mes) queda a la izquierda del reloj.
- Con el reloj **oculto**, la fecha crece en la esquina derecha (día grande + miércoles/octubre).
- **Segundero** aplica al analógico (aguja) y al digital (`HH:mm:ss`).
- **Se quita la tarjeta de fecha del panel**: quedan `Ajustes | Avisos | Batería | Clima | Memoria`,
  con más ancho para las tarjetas.
- Sin actualización automática: se mantiene el método de copiar el APK a la consola.
- Versión 1.2 (versionCode 7).

## 1.1

- **Anillo de batería** en el panel: nivel como arco de 270 %, porcentaje en el centro, color
  por nivel y tiempo restante estimado (con la tasa de descarga observada, porque en API 22
  no existe `BATTERY_PROPERTY_TIME_REMAINNING`).
- **Reloj analógico pequeño en la cabecera**, con fecha larga y hora en 24 h.
- **Pista de controles flotante**: aparece al arrancar, se desvanece a los ~6 s y vuelve si
  pulsas una tecla sin asignar.
- **Mosaico de Avisos** con el total de notificaciones, lista por app, Y para descartar los de
  esa app y X para descartarlos todos.
- **Menú por app** (long press): abrir, favorito, mover de categoría, info de la app y
  desinstalar.
- **Ajustes de fondo y tema unificados**: dos filas ("Tema" y "Fondo de pantalla") y un diálogo
  con Incluido / Imagen de la galería / Sin fondo / Quitar imagen. El tema automático por
  brillo solo actúa si el fondo se ve y el tema manual se respeta.
- Corrección: las notificaciones se contaban en un hilo secondary, así que los contadores no se
  refrescaban en vivo; ahora todo se actualiza en el hilo principal.
- Las secciones se actualizan en sitio al añadir o quitar favoritos y al cambiar de categoría.

## 1.0 — primer alcance completo

- **Selector de imagen de fondo** desde la galería del sistema, solo para el launcher.
- Copia a almacenamiento privado, rotación EXIF, recorte central a 1280×720.
- **Tema automático por brillo**: foto oscura → tema/velo oscuro; foto clara → tema/velo claro.
- Estilo manual Oscuro/Claro y opción de quitar la imagen.
- Rejilla de 5 columnas, foco 2D y scroll vertical.
- Contadores de notificaciones por app (solo número, sin leer contenido).
- Arranque con caché de metadatos/iconos, iconos progresivos y RECIENTES.
- Actualizaciones incrementales de favoritos y categorías.
- Panel de fecha, reloj, clima de Guatemala y memoria.
- Ajustes, temas, interruptor de launcher y documentación para GitHub.
- Hoja de ruta con las siguientes mejoras (`docs/ROADMAP.md`), empezando por el mosaico de
  Notificaciones con lista y descarte.

## Historial de prototipos

Las compilaciones internas anteriores se documentan aquí como pasos 0.x. La primera versión
pública es la 1.0, con `versionCode 5` para actualizar sobre cualquier compilación previa.

### 0.5 — rejilla, badges y fondos incluidos

- Rejilla de 5 columnas y foco 2D.
- Contadores de notificaciones y permiso de acceso.
- Dos fondos estáticos incluidos, con velo por tema.

### 0.4 — rendimiento

- Caché de metadatos y de iconos.
- Carga progresiva de iconos.
- RECIENTES y actualizaciones incrementales.

### 0.3 — sistema

- Panel de fecha, reloj analógico, clima y memoria.
- Ajustes: tema, segundero, clima, recarga.
- Interruptor para usar NSP como launcher.
- Recarga automática al instalar/desinstalar apps.

### 0.2 — launcher

- Escaneo de apps, mosaicos, foco con cruceta y stick.
- Favoritos, categorías y clasificación heurística.
- Tema oscuro/claro.

### 0.1 — prueba de concepto

- Primer launcher Metro para API 22.
- Apertura de apps y navegación básica con mando.


