# Hoja de ruta — NSP Launcher

**NSP = NVIDIA Shield Portable.**

Contexto de diseño: consola de 5" con 1280×720 (640×360 dp), Android 5.1.1 (API 22),
Tegra 4, 2 GB de RAM, mando integrado (cruceta, sticks analógicos, A/B/X/Y, L1/R1, START,
SELECT) y pantalla táctil. Todo lo que se propose aquí está pensado para esa pantalla, ese
mando y ese uso: jugar, escribir rápido y no tocar el launcher más de lo necesario.

Estado actual: **1.0** es la base funcional (rejilla de 5 columnas, panel de sistema, clima,
contadores por mosaico, imagen de fondo de la galería, caché de arranque, uso como Inicio).
Esta hoja recoge lo que viene después.

---

## Prioridad 1 — lo que se usa todos los días

### 1. Mosaico de Notificaciones (detalle)

**Qué es**
Un mosaico más en la fila **SISTEMA**, junto a Ajustes, con la burbuja del total de
notificaciones activas. No es una tarjeta del panel: el panel ya está lleno (Ajustes + fecha +
reloj + clima + memoria) y en 5" cada milímetro cuenta; como mosaico se comporta igual que el
resto (foco, A para abrir, borde visible).

**Por qué**
En la consola los avisos llegan de Telegram, WhatsApp, Gmail o la propia Play Store. El launcher
de fábrica no mostraba contadores, así que había que abrir la app o bajar la persiana solo para
ver si había algo. Con el mosaico se ve de un vistazo y, además, se pueden **silenciar** sin
abrir la app: es el gesto más útil en una consola donde las notificaciones se acumulan.

**Cómo se ve**
```
SISTEMA:   [Ajustes] [Notificaciones]
                        (3)      <- burbuja con el total
```

**Al pulsarlo (A)** se abre un diálogo con la lista de apps que tienen avisos, ordenadas por
número de avisos y luego por app:

```
Notificaciones
  Telegram            3
  WhatsApp            1
  Play Store          1
```

- **A** en una fila → abre esa app.
- **Y** en una fila → descarta los avisos de esa app (la burbuja baja al instante).
- **X** → descarta todos los avisos listados.
- **B** → cierra el diálogo y vuelve al launcher.

**De dónde salen los datos**
- `NotificationService` (ya existe) cuenta por paquete; hoy solo guarda el número.
- Falta guardar **en memoria** las claves de cada notificación (`StatusBarNotification.getKey()`)
  para poder cancelar: `NotificationListenerService.cancelNotification(clave)` existe desde
  API 21.
- `NotificationStore` pasa a exponer: total global, lista (paquete → nº), y `clear(pkg)` / `clearAll()`.

**Privacidad**
- Solo se lee el **paquete** y el **número** de avisos. No se lee ni se guarda el título ni el
  cuerpo de la notificación.
- Nada se persiste en disco: al reiniciar la consola, los contadores empiezan en 0 (limitación de
  Android 5.1, ver más abajo).

**Limitación conocida de API 22**
`getActiveNotifications()` es API 23. En 5.1 el servicio solo ve lo que llega **mientras está
conectado**, así que al arrancar la consola el mosaico sale en 0 hasta que llegue el primer aviso.
En la práctica el servicio se queda vivo en cuanto se concede el Acceso a notificaciones, por lo
que el contador es fiable durante el uso diario.

**Estimación**: 1 sesión. Toca `NotificationService` (guardar claves), `NotificationStore`
(lista y cancelaciones), `MainActivity` (mosaico + diálogo) y el diálogo de lista.
No requiere permisos nuevos ni tocar la UI del panel.

**Criterios de aceptación**
1. El mosaico muestra el total y se actualiza en vivo (sin reiniciar).
2. La lista sale ordenada y con el número de cada app.
3. Y descarta los avisos de esa app y la burbuja baja.
4. Con cero notificaciones el mosaico se oculta o muestra 0 sin fallar.
5. No se lee el texto de las notificaciones en ningún momento.

### 2. Fila de "más jugados"

Contadores propios de aperturas (en `SharedPreferences`, sin permisos ni servicios de Google).
En una consola de juegos vale más que "Favoritos": la fila de arriba muestra las 5 apps que más
se lanzan. Se puede combinar con Favoritos: la primera fila "RECIENTES" y una segunda "JUEGOS"
con el ranking, o sustituir RECIENTES por el ranking.

### 3. Fila de accesos rápidos

Mosaicos pequeños en la fila SISTEMA para lo que se cambia a menudo sin abrir Ajustes:
tema, fondo on/off, acceso a notificaciones, recargar apps, usar como inicio. Cada uno es un
mosaico normal: foco con cruceta y A para activar.

### 4. Menú por app (long press)

Un diálogo por app con: abrir, info de la app (Ajustes del sistema), desinstalar, fijar en
Favoritos, mover de categoría. Ahora el long press solo mueve de categoría.

---

## Prioridad 2 — información y control

### 5. Tarjeta de batería y de red

Añadir al panel una tarjeta compacta: nivel de batería, si está cargando y estado de Wi-Fi
(SSID). En una portátil que se usa de pie, saber la batería y la red de un vistazo evita
sorpresas. Se puede fusionar con la tarjeta de memoria para no añadir ancho.

### 6. Diagnóstico de rendimiento con botón "copiar"

Pantalla de Ajustes con datos reales: tiempo de arranque, si la caché sirvió, número de apps e
iconos, tamaño de la caché en disco, tiempo de escaneo, y frames por segundo aproximados.
Botón "copiar" para pegarlo en un issue de GitHub. Es la forma rápida de detectar en la consola
real si algo va lento (el emulador no sirve para medir el Tegra 4).

### 7. Tamaño de mosaico configurable

Opción de 4, 5 o 6 columnas y mosaico 1×1 o 2×2 para Favoritos/RECIENTES. En una pantalla de
5" se puede ver mejor con 4 columnas y letra grande; con muchas apps, 6 columnas entran más.

### 8. Exportar e importar ajustes

Favoritos, categorías y preferencias en un JSON que se pueda copiar a un archivo o compartir.
Sirve para reinstalar sin perder la configuración o para llevar la misma a otra consola.

---

## Prioridad 3 — comodidad y robustez

### 9. Autoactualizador desde GitHub Releases

La consola no tiene navegador cómodo. La app podría consultar un JSON de releases, descargar
el APK nuevo en la microSD y pedir al usuario que lo instale. En Android 5.1 se puede lanzar el
instalador del sistema sobre el archivo; el usuario confirma el "origen desconocido".

### 10. Búsqueda local de apps

Búsqueda por texto (teclado en pantalla) sobre la lista de apps, con resultados en rejilla.
Complementa a la navegación por categorías cuando hay muchas apps instaladas.

### 11. Gestión de la caché de iconos

Fila en Ajustes con el tamaño ocupado por la caché, botón para borrarla y opción de reconstruirla.
Útil si la consola va justa de almacenamiento interno.

### 12. Detalles del panel

Reloj configurable de 12/24 h, fecha con formato corto (mié 21), y opción de ocultar el panel
si se prefiere solo apps.

---

## Decisiones ya tomadas (no se contemplan)

- **Pantalla con todas las apps**: no se hace por ahora.
- **Automatización de CI/CD**: no se hace por ahora.
- **Widgets de terceros (incluido el de Google)**: fuera de alcance; `AppWidgetHost` mete
  código ajeno en nuestro proceso (riesgo de crash) y aporta poco en una pantalla de 5".
- **Modo TV/HDMI**: fuera de alcance; el objetivo es la portátil en mano.
- **Búsqueda por voz**: el SHIELD no tiene micrófono fiable, así que se descarta.

---

## Cómo se tacklea cada punto

1. Un punto por versión, con `versionCode` siempre creciente y el APK firmado con la misma clave.
2. Cada punto se prueba en el emulador API 22 (1280×720 @ 320 dpi) y, si toca el arranque o la
   memoria, se mide con el diagnóstico del punto 6.
3. Lo que se toca en la consola real se anota aquí como "pendiente de hardware".
4. Nada de esto está implementado todavía: es una lista de trabajo, no una promesa.
