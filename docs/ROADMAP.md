# Pendientes — NSP Launcher

**NSP = NVIDIA Shield Portable.**

Contexto de diseño: consola de 5" con 1280×720 (640×360 dp), Android 5.1.1 (API 22),
Tegra 4, 2 GB de RAM, mando integrado (cruceta, sticks analógicos, A/B/X/Y, L1/R1, START,
SELECT) y pantalla táctil. Todo lo que se propose aquí está pensado para esa pantalla, ese
mando y ese uso: jugar, escribir rápido y no tocar el launcher más de lo necesario.

Estado actual: **1.0** es la base funcional (rejilla de 5 columnas, panel de sistema, clima,
contadores por mosaico, imagen de fondo de la galería, caché de arranque, uso como Inicio).
Este documento recoge solo lo que queda pendiente.

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

### 5. Anillo de batería en el panel (detalle)

**Qué es**
En la fila SISTEMA, un medidor circular (anillo) con el nivel de batería dibujado como arco y
el porcentaje en el centro. Debajo, el tiempo restante estimado o el estado de carga.

**Por qué**
La SHIELD Portable es una portátil de 5" y su batería es el dato más crítico cuando juegas
fuera de casa: el launcher de fábrica no lo muestra y con NSP el panel es lo primero que ves
al encender. Hoy el panel enseña fecha, reloj, clima y memoria, pero nada de batería.

**Dónde**
En la fila SISTEMA. Esa fila ya tiene Ajustes (96 dp) + fecha + reloj + clima + memoria, así
que el anillo tiene dos sitios posibles: **sustituyendo la tarjeta de reloj** (reloj y anillo no
caben juntos sin apretar) o en una **segunda fila con dos tarjetas**. Queda por decidir
mirándolo en la consola real.

**De dónde salen los datos**
- `BatteryManager` y el broadcast `ACTION_BATTERY_CHANGED`: nivel, estado de carga,
  temperatura y voltaje. Sin permisos y sin sondeos.
- Tiempo restante: `BatteryManager.BATTERY_PROPERTY_TIME_REMAINNING` es API 28 y no existe en
  5.1. Opciones: estimarlo con la tasa de descarga observada (media móvil del % por minuto
  guardada en `SharedPreferences`) o mostrar solo porcentaje e icono de carga.
- Refresco: al recibir el broadcast y un refresco suave cada 2-5 minutos mientras el launcher
  está visible.

**Diseño**
- Arco de 270°, fondo tenue y relleno con el color del nivel.
- Verde por encima del 40 %, ámbar del 15 % al 40 %, rojo por debajo del 15 %; azul suave
  mientras carga.
- Porcentaje grande en el centro; debajo, "cargando" o el tiempo estimado.
- Plano, como el resto: sin sombras y con el mismo lenguaje que `ClockView` (Canvas propio,
  sin librerías).

**Privacidad**
Nada sale de la consola: solo se lee el estado de la batería.

**Estimación**
1 sesión. Toca un `BatteryRingView` nuevo, `Dashboard` para alojar la tarjeta y `MainActivity`
para el refresco.

**Criterios de aceptación**
1. El anillo refleja el nivel real casi de inmediato.
2. Distingue cargando de descargando y no falla al enchufar.
3. No añade retardos perceptibles al arranque (dato por broadcast, no por sondeo).
4. Es legible en tema claro y en tema oscuro.
5. Si el tiempo restante no es fiable, no se muestra.

### 6. Tarjeta de red

Tarjeta compacta con el estado de Wi-Fi (SSID y señal) y el tipo de conexión. En una portátil
que se usa de pie, saber si está en la red de casa evita sorpresas al abrir una app.

### 7. Diagnóstico de rendimiento con botón "copiar"

Pantalla de Ajustes con datos reales: tiempo de arranque, si la caché sirvió, número de apps e
iconos, tamaño de la caché en disco, tiempo de escaneo, y frames por segundo aproximados.
Botón "copiar" para pegarlo en un issue de GitHub. Es la forma rápida de detectar en la consola
real si algo va lento (el emulador no sirve para medir el Tegra 4).

### 8. Tamaño de mosaico configurable

Opción de 4, 5 o 6 columnas y mosaico 1×1 o 2×2 para Favoritos/RECIENTES. En una pantalla de
5" se puede ver mejor con 4 columnas y letra grande; con muchas apps, 6 columnas entran más.

### 9. Exportar e importar ajustes

Favoritos, categorías y preferencias en un JSON que se pueda copiar a un archivo o compartir.
Sirve para reinstalar sin perder la configuración o para llevar la misma a otra consola.

---

## Prioridad 3 — comodidad y robustez

### 10. Autoactualizador desde GitHub Releases

La consola no tiene navegador cómodo. La app podría consultar un JSON de releases, descargar
el APK nuevo en la microSD y pedir al usuario que lo instale. En Android 5.1 se puede lanzar el
instalador del sistema sobre el archivo; el usuario confirma el "origen desconocido".

### 11. Búsqueda local de apps

Búsqueda por texto (teclado en pantalla) sobre la lista de apps, con resultados en rejilla.
Complementa a la navegación por categorías cuando hay muchas apps instaladas.

### 12. Gestión de la caché de iconos

Fila en Ajustes con el tamaño ocupado por la caché, botón para borrarla y opción de reconstruirla.
Útil si la consola va justa de almacenamiento interno.

### 13. Detalles del panel

Reloj configurable de 12/24 h, fecha con formato corto (mié 21), y opción de ocultar el panel
si se prefiere solo apps.

---

## Cómo se tacklea cada punto

1. Un punto por versión, con `versionCode` siempre creciente y el APK firmado con la misma clave.
2. Cada punto se prueba en el emulador API 22 (1280×720 @ 320 dpi) y, si toca el arranque o la
   memoria, se mide con el diagnóstico del punto 7.
3. Lo que se toca en la consola real se anota aquí como "pendiente de hardware".
4. Nada de esto está implementado todavía: es una lista de trabajo, no una promesa.
