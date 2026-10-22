# Pendientes — NSP Launcher

**NSP = NVIDIA Shield Portable.**

Contexto de diseño: consola de 5" con 1280×720 (640×360 dp), Android 5.1.1 (API 22),
Tegra 4, 2 GB de RAM, mando integrado (cruceta, sticks analógicos, A/B/X/Y, L1/R1, START,
SELECT) y pantalla táctil. Todo lo de esta lista está pensado para esa pantalla, ese mando y
ese uso: jugar, escribir rápido y no tocar el launcher más de lo necesario.

Estado actual: **1.0** es la base funcional (rejilla de 5 columnas, panel de sistema, clima,
contadores por mosaico, imagen de fondo de la galería, caché de arranque, uso como Inicio).
Aquí solo están los puntos que quedan pendientes.

Reglas de trabajo: un punto por versión, `versionCode` siempre creciente, **siempre la misma
clave de firma**, pruebas en el emulador API 22 (1280×720 @ 320 dpi) y lo que toque la consola
real se anota como "pendiente de hardware".

---

## 1. Mosaico de Notificaciones

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

---

## 2. Ajustes de fondo de pantalla y tema en un solo sitio

**El problema**
Hoy la sección APARIENCIA de Ajustes tiene cinco filas que hablan de lo mismo: **Tema**,
**Fondo**, **Imagen de fondo**, **Estilo del fondo** y **Quitar imagen**. Con una imagen
personalizada hay tres que se pisan entre sí:

- **"Tema" y "Estilo del fondo" parecen lo mismo** (los dos eligen oscuro/claro), pero el
  estilo además cambia el tema de toda la interfaz, cosa que el nombre no dice.
- **"Fondo" (activado/desactivado) convive con "Imagen de fondo"**: no queda claro si
  desactivar el fondo borra la imagen o solo la esconde.
- **"Quitar imagen" es una acción destructiva permanente** en la lista, y solo tiene sentido
  cuando hay imagen propia.
- **Con la imagen puesta y el fondo desactivado, el tema sigue siguiendo el brillo de la foto**
  aunque la foto no se vea: comportamiento inesperado.
- **Sin imagen propia, "Estilo del fondo" no aporta nada**: duplica "Tema".

**Propuesta**
Dejar **dos filas** y un diálogo:

- **Fondo de pantalla** → resume el estado actual y abre un diálogo con:
  - Incluido (con el tema actual)
  - Imagen de la galería
  - Sin fondo
  - (solo si hay imagen propia) Quitar imagen
- **Tema** → Oscuro / Claro. Con imagen propia, el tema elegida también manda sobre el velo.

**Reglas de coherencia**
- El tema automático por brillo de la foto **solo actúa si el fondo se está viendo**: con "sin
  fondo" o con el fondo incluido manda el tema que eligió el usuario.
- Si el usuario fija el tema a mano con una imagen puesta, se recuerda como *override* de esa
  imagen y no se le vuelve a imponer el automático en el siguiente arranque.
- El diálogo comunica el resultado: *"Fondo: imagen de la galería · tema automático (claro)"*.

**Qué se elimina**
Las filas "Estilo del fondo" y "Quitar imagen", y el valor "Personalizada / Incluida" de la
fila de imagen: pasan a vivir dentro del diálogo.

**Estimación**: 1 sesión. Toca `SettingsActivity` (filas y diálogo), `BackgroundManager`
(eliminación del concepto de estilo y del override automático) y `MainActivity` (respetar el
override al arrancar y que el fondo apagado no fuerce el tema).

**Criterios de aceptación**
1. Solo dos filas en APARIENCIA relacionadas con fondo y tema.
2. Un único sitio para elegir imagen, quitarla y desactivarla.
3. El estado actual se lee de un vistazo: "imagen de la galería", "fondo incluido" o "sin fondo".
4. Sin fondo, la foto deja de influir en el tema.
5. Si el usuario fija el tema a mano, se respeta aunque la foto sea más clara u oscura.

---

## 3. Menú por app (long press)

Un diálogo por app con: abrir, info de la app (Ajustes del sistema), desinstalar, fijar en
Favoritos y mover de categoría. Ahora el long press solo mueve de categoría.

- **A** en la fila → ejecuta la acción elegida.
- **B** → cerrar sin hacer nada.

---

## 4. Anillo de batería en el panel

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
