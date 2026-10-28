# Avance — NSP Launcher 1.5

Objetivo: `versionName 1.5`, `versionCode 11`. Última instalación manual.
Estado: **pruebas finales en emulador**.

## Bloques

- [x] 1. RSS: parser, foco, A, error visible, caché
- [x] 2. Previa de titulares en la tarjeta Noticias
- [x] 3. Agenda: tipografía 9sp / 12.5sp
- [x] 4. Fila de versión + estado + chip ↑ en cabecera
- [x] 5. Certificados GTS R1/R3/R4 + DigiCert G2 + Amazon Root CA 1
- [x] 6. Actualizador GitHub: consulta, descarga, firma, instalador
- [x] 7. CASE: voz + LED en cabecera + overlay (A comprueba)
- [x] 8. Búsqueda Google: overlay, teclado virtual, Chrome/Firefox
- [ ] 9. Pruebas en emulador
- [ ] 10. Documentación, build final, respaldo, Release

## Pruebas hechas (emulador API 22)

- Inicio: SISTEMA visible, sin desplazamiento heredado, foco en Avisos.
- RSS Mundo (BBC): 12 titulares, foco resaltado, metadatos.
- RSS Deportes (ESPN): 12 titulares; TLS con raíces empaquetadas.
- RSS Guatemala (Google News): 12 titulares en español.
- CASE: LED, estado, BTC, clima, noticias, versión y actualización.
- Ajustes: versión 1.5 · código 11, estado del actualizador, CASE.
- Búsqueda: teclado virtual sin modo pantalla completa; Google abre en el
  navegador con la consulta correcta; historial de 5.
- Actualizador real: `releases/latest` responde y deja «Al día» con la 1.4 publicada.
- Actualizador simulado (servidor local con APK 1.6): detecta, muestra chip `↑ 1.6`,
  descarga 170 KB, valida paquete/firma/versionCode.
- Incidencia corregida: el instalador de Android 5.1 no acepta `content://`
  (solo `file://`). El APK se guarda ahora en la carpeta externa de la app y se
  abre con `file://`; en Android 7+ se mantiene `content://`.

## Notas técnicas

- `RssService`: parser por etiqueta con CDATA y `trim`; reintento TLS con raíces.
- `UpdateService`: `releases/latest`, comparación numérica, descarga a
  `getExternalFilesDir()` (o caché), validación de paquete/firma/versionCode.
- `ApkProvider`: sirve el APK por `content://` en Android 7+.
- `SearchActivity`: `IME_FLAG_NO_EXTRACT_UI` para evitar el teclado a pantalla
  completa en horizontal; historial de 5; Chrome/Firefox.
- CASE: LED de 4 barras en cabecera, overlay con estado y voz (interruptor en Ajustes).

## Bitácora

- 2026-10-28: ROADMAP actualizado (banner y webhook a 1.6).
- 2026-10-28: bloques 1–8 implementados. Compila 1.5 (170 KB).
- 2026-10-28: feeds verificados en línea: BBC, ESPN y Google News responden 200.
- 2026-10-28: pruebas de UI y RSS en emulador; corregido el arranque desplazado.
- 2026-10-28: actualizador probado contra Release real y contra servidor local 1.6.
- 2026-10-28: corregido el instalador para Android 5.1 (file://, carpeta externa).

## Fuera de 1.5

- Webhook de CASE y banner → 1.6
- Ajustes del actualizador → 1.6
- Prueba real de auto-actualización → día de la 1.6
