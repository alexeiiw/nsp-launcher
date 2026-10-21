param(
    [string]$Apk,
    [string]$SdkRoot
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $Apk) {
    $Apk = Join-Path $Root "dist\NSPLauncher.apk"
}

function Find-Adb([string]$explicitSdk) {
    $candidates = @()
    if ($explicitSdk) { $candidates += (Join-Path $explicitSdk "platform-tools\adb.exe") }
    if ($env:ANDROID_HOME) { $candidates += (Join-Path $env:ANDROID_HOME "platform-tools\adb.exe") }
    if ($env:ANDROID_SDK_ROOT) { $candidates += (Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe") }
    if ($env:LOCALAPPDATA) { $candidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe") }
    $candidates += "C:\Program Files (x86)\Android\android-sdk\platform-tools\adb.exe"
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) { return $candidate }
    }
    $command = Get-Command adb -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    return $null
}

$Adb = Find-Adb $SdkRoot
if (-not $Adb) {
    throw "No se encontro adb. Define ANDROID_HOME, instala platform-tools o usa la instalacion por microSD."
}
if (-not (Test-Path -LiteralPath $Apk)) {
    throw "No se encontro el APK: $Apk. Ejecuta build.ps1 primero."
}

Write-Host "Dispositivos conectados:" -ForegroundColor Cyan
& $Adb devices
if ($LASTEXITCODE -ne 0) { throw "adb devices fallo" }

& $Adb install -r $Apk
if ($LASTEXITCODE -ne 0) { throw "adb install fallo" }

& $Adb shell am start -n com.nsp.launcher/.MainActivity
if ($LASTEXITCODE -ne 0) { throw "No se pudo abrir NSP" }

Write-Host "NSP instalado y abierto." -ForegroundColor Green
