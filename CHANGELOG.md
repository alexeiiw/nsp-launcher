# Changelog

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
