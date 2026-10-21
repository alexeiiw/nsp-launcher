# Compilación

NSP Launcher se compila sin Gradle, con las herramientas del SDK de Android.
El script `build.ps1` ejecuta la cadena completa y firma el APK.

## Requisitos

| Componente | Versión | Notas |
|---|---|---|
| Windows | 10/11 | PowerShell 5.1 |
| Android SDK | platform **31+** | `platforms;android-31` o superior |
| Build-tools | **32.0.0** | `aapt2`, `d8`, `zipalign`, `apksigner` |
| JDK | **8** | obligatorio para `d8.bat` de build-tools 32 |
| platform-tools | cualquiera reciente | `adb` para instalar |

### Por qué JDK 8

El `d8.bat` de build-tools 32 lanza Java con `-Djava.ext.dirs`, opción eliminada en Java 9.
Con JDK 11 o 17 falla con:

```
-Djava.ext.dirs=... is not supported. Use -classpath instead.
```

Instala un JDK 8 (Temurin, Zulu, etc.) y pásalo con `-JdkPath`, o deja que el script lo
detecte en `%ProgramFiles%\Android\jdk`, `%ProgramFiles%\Java` o `JAVA_HOME`.
El JBR de Android Studio (17) no sirve para este paso.

### Instalar las piezas del SDK

Con Android Studio: SDK Manager → SDK Platforms → Android 12 (API 31) y
SDK Tools → Android SDK Build-Tools 32.0.0 + Android SDK Platform-Tools.

Con `sdkmanager`:

```powershell
sdkmanager "platforms;android-31" "build-tools;32.0.0" "platform-tools"
sdkmanager --licenses
```

## Compilar

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

Salida:

- `dist\NSPLauncher-1.0.apk`
- `dist\NSPLauncher.apk` (copia con nombre fijo)
- `dist\NSPLauncher-1.0.apk.idsig` (firma v2)

### Parámetros

```powershell
.\build.ps1 `
  -SdkRoot "C:\Android\Sdk" `
  -JdkPath "C:\Java\jdk8" `
  -VersionName "1.0" `
  -VersionCode 5 `
  -KeystorePath "C:\secure\nsp-release.jks" `
  -KeyAlias nsp `
  -StorePass "..." `
  -KeyPass "..."
```

- `-SkipSign`: deja un APK alineado sin firmar en `build\aligned.apk`.
- `-VersionName` / `-VersionCode`: versión del manifiesto.
- `-KeystorePath` y compañía: firma de release.
- Si no se indican, se usa `keystore\nsp.keystore` con contraseña `android` (depuración).

### Qué hace el script

1. `aapt2 compile` de los recursos.
2. `aapt2 link` genera el APK base y `R.java`.
3. `javac` (JDK 8) compila contra `android.jar`.
4. `d8 --min-api 22` genera `classes.dex`.
5. `jar` inserta el dex y `zipalign` alinea.
6. `apksigner` firma con v1 (necesario para Android 5.1) y v2.
7. `apksigner verify` y `aapt2 dump badging` muestran el resultado.

## Firma para publicar

No publiques `keystore\nsp.keystore`: es una clave de depuración y está en `.gitignore`.
Para Releases crea una clave propia **fuera del repositorio**:

```powershell
keytool -genkeypair -v -keystore C:\secure\nsp-release.jks `
  -alias nsp -keyalg RSA -keysize 2048 -validity 10000
```

Compila con:

```powershell
.\build.ps1 -KeystorePath "C:\secure\nsp-release.jks" -KeyAlias nsp
```

Guarda esa clave y sus contraseñas. **Todas las actualizaciones deben firmarse con la misma
clave** o Android no las instalará encima (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).

## Instalar

Por USB:

```powershell
.\install.ps1
```

o:

```powershell
adb devices
adb install -r .\dist\NSPLauncher-1.0.apk
adb shell am start -n com.nsp.launcher/.MainActivity
```

Por microSD/USB: copia el APK, activa Orígenes desconocidos y ábrelo con un gestor de archivos.

## Comprobar el APK

```powershell
aapt2 dump badging .\dist\NSPLauncher-1.0.apk
apksigner verify --print-certs .\dist\NSPLauncher-1.0.apk
```

Debe mostrar `sdkVersion:'22'`, `targetSdkVersion:'28'` y `versionName='1.0'`.

## Problemas comunes

| Síntoma | Causa / solución |
|---|---|
| `la ejecución de scripts está deshabilitada` | Usa `powershell -ExecutionPolicy Bypass -File .\build.ps1` |
| `d8 fallo` con `java.ext.dirs` | Estás usando JDK 9+; instala JDK 8 y pasa `-JdkPath` |
| `No se encontro: ...aapt2.exe` | Falta build-tools 32.0.0 |
| `No se encontro platforms\android-31\android.jar` | Instala `platforms;android-31` |
| `adb: no devices/emulators found` | Activa Depuración USB, prueba otro cable/puerto, `adb kill-server` |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | El APK está firmado con otra clave; desinstala (se pierden datos) o usa la clave original |
| `INSTALL_FAILED_OLDER_SDK` | El dispositivo es anterior a Android 5.1 (API 22) |
| El título NSP se corta | Actualiza a 1.2 o superior; el ancho del título ya no se comprime |

La primera vez que se abre el launcher, Android 5.1 muestra el aviso de pantalla completa
("Swipe down from the top to exit full screen"). Es del sistema y solo aparece una vez.
