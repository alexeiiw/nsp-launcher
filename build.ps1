param(
    [switch]$SkipSign,
    [string]$SdkRoot,
    [string]$JdkPath,
    [string]$VersionName = "1.0",
    [int]$VersionCode = 5,
    [string]$KeystorePath,
    [string]$KeyAlias = "nsp",
    [string]$StorePass = "android",
    [string]$KeyPass = "android"
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path

function Test-BuildTools([string]$dir) {
    if (-not (Test-Path -LiteralPath $dir)) { return $false }
    return (Test-Path -LiteralPath (Join-Path $dir "aapt2.exe")) -and
           (Test-Path -LiteralPath (Join-Path $dir "d8.bat")) -and
           (Test-Path -LiteralPath (Join-Path $dir "zipalign.exe")) -and
           (Test-Path -LiteralPath (Join-Path $dir "apksigner.bat"))
}

function Find-Sdk([string]$explicit) {
    $candidates = @()
    if ($explicit) { $candidates += $explicit }
    if ($env:ANDROID_HOME) { $candidates += $env:ANDROID_HOME }
    if ($env:ANDROID_SDK_ROOT) { $candidates += $env:ANDROID_SDK_ROOT }
    if ($env:LOCALAPPDATA) { $candidates += (Join-Path $env:LOCALAPPDATA "Android\Sdk") }
    $candidates += "C:\Program Files (x86)\Android\android-sdk"
    $candidates += "C:\Android\Sdk"
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) { return $candidate }
    }
    return $null
}

function Find-BuildTools([string]$sdk) {
    $preferred = Join-Path $sdk "build-tools\32.0.0"
    if (Test-BuildTools $preferred) { return $preferred }
    $dir = Join-Path $sdk "build-tools"
    if (-not (Test-Path -LiteralPath $dir)) { return $null }
    $dirs = Get-ChildItem -LiteralPath $dir -Directory | Sort-Object Name -Descending
    foreach ($d in $dirs) {
        if (Test-BuildTools $d.FullName) { return $d.FullName }
    }
    return $null
}

function Find-Platform([string]$sdk) {
    $preferred = Join-Path $sdk "platforms\android-31\android.jar"
    if (Test-Path -LiteralPath $preferred) { return $preferred }
    $dir = Join-Path $sdk "platforms"
    if (-not (Test-Path -LiteralPath $dir)) { return $null }
    $dirs = Get-ChildItem -LiteralPath $dir -Directory | Sort-Object Name -Descending
    foreach ($d in $dirs) {
        $jar = Join-Path $d.FullName "android.jar"
        if (Test-Path -LiteralPath $jar) { return $jar }
    }
    return $null
}

function Test-Jdk8([string]$jdkHome) {
    $release = Join-Path $jdkHome "release"
    if (-not (Test-Path -LiteralPath $release)) { return $false }
    return ((Get-Content -LiteralPath $release -Raw) -match 'JAVA_VERSION="1\.8')
}

function Find-Jdk8([string]$explicit) {
    if ($explicit -and (Test-Path -LiteralPath (Join-Path $explicit "bin\javac.exe")) -and (Test-Jdk8 $explicit)) {
        return $explicit
    }
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME "bin\javac.exe")) -and (Test-Jdk8 $env:JAVA_HOME)) {
        return $env:JAVA_HOME
    }
    $roots = @(
        (Join-Path $env:ProgramFiles "Android\jdk"),
        (Join-Path $env:ProgramFiles "Java"),
        (Join-Path $env:ProgramFiles "Eclipse Adoptium"),
        (Join-Path $env:ProgramFiles "Microsoft")
    )
    foreach ($root in $roots) {
        if (-not (Test-Path -LiteralPath $root)) { continue }
        $found = Get-ChildItem -LiteralPath $root -Recurse -Depth 4 -Filter javac.exe -ErrorAction SilentlyContinue
        foreach ($javac in $found) {
            $jdkHome = Split-Path (Split-Path $javac.FullName)
            if (Test-Jdk8 $jdkHome) { return $jdkHome }
        }
    }
    return $null
}

$Sdk = Find-Sdk $SdkRoot
if (-not $Sdk) {
    throw "No se encontro el Android SDK. Pasa -SdkRoot o define ANDROID_HOME."
}
$Bt = Find-BuildTools $Sdk
if (-not $Bt) {
    throw "No se encontraron build-tools con aapt2, d8, zipalign y apksigner en $Sdk\build-tools."
}
$Jar = Find-Platform $Sdk
if (-not $Jar) {
    throw "No se encontro platforms\android-31\android.jar en $Sdk. Instala la plataforma 31 o superior."
}
$Jdk = Find-Jdk8 $JdkPath
if (-not $Jdk) {
    throw "Se requiere JDK 8 (d8 de build-tools 32 usa java.ext.dirs). Instala un JDK 8 o pasa -JdkPath."
}

$Aapt2     = Join-Path $Bt "aapt2.exe"
$D8        = Join-Path $Bt "d8.bat"
$ZipAlign  = Join-Path $Bt "zipalign.exe"
$ApkSigner = Join-Path $Bt "apksigner.bat"
$Javac     = Join-Path $Jdk "bin\javac.exe"
$JarTool   = Join-Path $Jdk "bin\jar.exe"
$Keytool   = Join-Path $Jdk "bin\keytool.exe"

$Build    = Join-Path $Root "build"
$Dist     = Join-Path $Root "dist"
$Keystore = if ($KeystorePath) { $KeystorePath } else { Join-Path $Root "keystore\nsp.keystore" }
$Manifest = Join-Path $Root "app\src\main\AndroidManifest.xml"
$ResDir   = Join-Path $Root "app\src\main\res"
$JavaDir  = Join-Path $Root "app\src\main\java"

$required = @($Aapt2, $D8, $ZipAlign, $ApkSigner, $Javac, $JarTool, $Keytool, $Jar, $Manifest, $ResDir, $JavaDir)
foreach ($p in $required) {
    if (-not (Test-Path -LiteralPath $p)) {
        throw "No se encontro: $p"
    }
}

$env:JAVA_HOME = $Jdk
$env:PATH = (Join-Path $Jdk "bin") + ";" + $env:PATH

Write-Host "NSP Launcher - build $VersionName" -ForegroundColor Cyan
Write-Host "SDK: $Sdk"
Write-Host "JDK: $Jdk"

if (Test-Path -LiteralPath $Build) {
    Remove-Item -LiteralPath $Build -Recurse -Force
}
$gen     = Join-Path $Build "gen"
$classes = Join-Path $Build "classes"
$dexDir  = Join-Path $Build "dex"
New-Item -ItemType Directory -Path $gen, $classes, $dexDir, $Dist -Force | Out-Null

Write-Host "[1/6] Recursos (aapt2)"
$resZip = Join-Path $Build "res.zip"
& $Aapt2 compile --dir $ResDir -o $resZip
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile fallo" }

$base = Join-Path $Build "base.apk"
& $Aapt2 link -o $base -I $Jar --manifest $Manifest $resZip --java $gen `
    --min-sdk-version 22 --target-sdk-version 28 --version-code $VersionCode --version-name $VersionName
if ($LASTEXITCODE -ne 0) { throw "aapt2 link fallo" }

Write-Host "[2/6] Java (javac)"
$srcs = @(Get-ChildItem -LiteralPath $JavaDir -Recurse -Filter *.java | ForEach-Object { $_.FullName })
$srcs += @(Get-ChildItem -LiteralPath $gen -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& $Javac -source 8 -target 8 -encoding UTF-8 -bootclasspath $Jar -d $classes @srcs
if ($LASTEXITCODE -ne 0) { throw "javac fallo" }

Write-Host "[3/6] Dex (d8)"
$classesJar = Join-Path $Build "classes.jar"
& $JarTool cf $classesJar -C $classes .
if ($LASTEXITCODE -ne 0) { throw "jar de clases fallo" }
& $D8 --min-api 22 --lib $Jar --output $dexDir $classesJar
if ($LASTEXITCODE -ne 0) { throw "d8 fallo" }

Write-Host "[4/6] Empaquetar classes.dex"
$unsigned = Join-Path $Build "unsigned.apk"
$aligned  = Join-Path $Build "aligned.apk"
Copy-Item -LiteralPath $base -Destination $unsigned -Force
& $JarTool uf $unsigned -C $dexDir classes.dex
if ($LASTEXITCODE -ne 0) { throw "jar fallo" }

& $ZipAlign -f 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw "zipalign fallo" }

if ($SkipSign) {
    Write-Host "APK sin firmar: $aligned" -ForegroundColor Yellow
    exit 0
}

Write-Host "[5/6] Firma (apksigner)"
if (-not (Test-Path -LiteralPath $Keystore)) {
    New-Item -ItemType Directory -Path (Split-Path -Parent $Keystore) -Force | Out-Null
    & $Keytool -genkeypair -v -keystore $Keystore -alias $KeyAlias -keyalg RSA -keysize 2048 `
        -validity 10000 -storepass $StorePass -keypass $KeyPass `
        -dname "CN=NSP Launcher,OU=NSP,O=NSP,L=NA,ST=NA,C=NA"
    if ($LASTEXITCODE -ne 0) { throw "keytool fallo" }
}

$outApk = Join-Path $Dist "NSPLauncher-$VersionName.apk"
& $ApkSigner sign --ks $Keystore --ks-key-alias $KeyAlias --ks-pass "pass:$StorePass" --key-pass "pass:$KeyPass" `
    --min-sdk-version 22 --v1-signing-enabled true --v2-signing-enabled true `
    --out $outApk $aligned
if ($LASTEXITCODE -ne 0) { throw "apksigner fallo" }
Copy-Item -LiteralPath $outApk -Destination (Join-Path $Dist "NSPLauncher.apk") -Force

Write-Host "[6/6] Verificacion"
& $ApkSigner verify --print-certs $outApk
if ($LASTEXITCODE -ne 0) { throw "verificacion de firma fallo" }

Write-Host ""
& $Aapt2 dump badging $outApk 2>$null | Select-String "package:|sdkVersion|targetSdkVersion|application-label:"
$size = [Math]::Round((Get-Item -LiteralPath $outApk).Length / 1KB, 1)
Write-Host ""
Write-Host "APK listo: $outApk ($size KB)" -ForegroundColor Green
