# Builds the TapDrop test app without Gradle and installs it on the watch.
# Usage: powershell -NoProfile -ExecutionPolicy Bypass -File build.ps1 [-NoInstall]
param([switch]$NoInstall)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$root       = $PSScriptRoot
$sdk        = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$bt         = Join-Path $sdk 'build-tools\35.0.0'
$platform   = Join-Path $sdk 'platforms\android-35'
$androidJar = Join-Path $platform 'android.jar'
$adb        = Join-Path $sdk 'platform-tools\adb.exe'
$jdk        = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'

# The java on PATH is version 8, too old for d8 and apksigner.
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"

function Assert-Ok($step) { if ($LASTEXITCODE -ne 0) { throw "$step failed with exit code $LASTEXITCODE" } }

# Fetches one SDK package from Google and checks it against the SHA-1 in Google's index.
function Get-SdkPackage($file, $sha1, $target) {
    if (Test-Path $target) { return }
    $dl = Join-Path $env:TEMP ("sdk-dl-" + [guid]::NewGuid().ToString('N').Substring(0, 8))
    New-Item -ItemType Directory -Path $dl | Out-Null
    $zip = Join-Path $dl $file
    Write-Host "Downloading $file from dl.google.com ..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri "https://dl.google.com/android/repository/$file" -OutFile $zip -UseBasicParsing
    $got = (Get-FileHash $zip -Algorithm SHA1).Hash.ToLower()
    if ($got -ne $sha1) { throw "Checksum mismatch for $file (got $got)" }
    $x = Join-Path $dl 'x'
    Expand-Archive -Path $zip -DestinationPath $x
    $top = Get-ChildItem $x -Directory | Select-Object -First 1
    New-Item -ItemType Directory -Force -Path (Split-Path $target) | Out-Null
    Move-Item $top.FullName $target
    Remove-Item $dl -Recurse -Force
    Write-Host "Installed $target" -ForegroundColor Green
}

Get-SdkPackage 'build-tools_r35_windows.zip' 'af059bb67cf7786f45ee0db85e2d24985df1b4b6' $bt
Get-SdkPackage 'platform-35_r02.zip'         '0bb560a90a7a2cbd0dd8348224d518b638fe7949' $platform

$b = Join-Path $root 'build'
if (Test-Path $b) { Remove-Item $b -Recurse -Force }
foreach ($d in 'gen', 'classes', 'dex') { New-Item -ItemType Directory -Force -Path (Join-Path $b $d) | Out-Null }

Write-Host "Compiling resources..." -ForegroundColor Cyan
& "$bt\aapt2.exe" compile --dir "$root\res" -o "$b\res.zip"; Assert-Ok 'aapt2 compile'
& "$bt\aapt2.exe" link -o "$b\base.apk" -I $androidJar --manifest "$root\AndroidManifest.xml" `
    --java "$b\gen" --min-sdk-version 30 --target-sdk-version 34 "$b\res.zip"; Assert-Ok 'aapt2 link'

Write-Host "Compiling Java..." -ForegroundColor Cyan
$sources = @(Get-ChildItem "$root\src", "$b\gen" -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& "$jdk\bin\javac.exe" --release 11 -encoding UTF-8 -classpath $androidJar -d "$b\classes" $sources; Assert-Ok 'javac'

Write-Host "Converting to dex..." -ForegroundColor Cyan
& "$jdk\bin\jar.exe" cf "$b\classes.jar" -C "$b\classes" .; Assert-Ok 'jar'
& "$bt\d8.bat" --lib $androidJar --min-api 30 --output "$b\dex" "$b\classes.jar"; Assert-Ok 'd8'

Write-Host "Packaging and signing..." -ForegroundColor Cyan
Copy-Item "$b\base.apk" "$b\unsigned.apk"
Push-Location "$b\dex"
try { & "$jdk\bin\jar.exe" uf "$b\unsigned.apk" classes.dex; Assert-Ok 'add dex' } finally { Pop-Location }
& "$bt\zipalign.exe" -f -p 4 "$b\unsigned.apk" "$b\aligned.apk"; Assert-Ok 'zipalign'

# A throwaway debug key, kept beside the source so reinstalls keep the same signature.
$ks = Join-Path $root 'debug.keystore'
if (-not (Test-Path $ks)) {
    & "$jdk\bin\keytool.exe" -genkeypair -keystore $ks -storepass android -keypass android -alias androiddebugkey `
        -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug,O=Android,C=US'; Assert-Ok 'keytool'
}
$apk = Join-Path $b 'tapdrop-spike.apk'
& "$bt\apksigner.bat" sign --ks $ks --ks-pass pass:android --key-pass pass:android --out $apk "$b\aligned.apk"; Assert-Ok 'apksigner'
Write-Host "Built $apk" -ForegroundColor Green

if ($NoInstall) { return }

# Pick the watch by hardware type, so a phone connected at the same time is never the target.
$dev = $null
foreach ($l in (& $adb devices | Select-String "`tdevice$")) {
    $serial = ($l.Line -split "`t")[0]
    $kind = (& $adb -s $serial shell getprop ro.build.characteristics) -join ''
    if ($kind -match 'watch') { $dev = $serial; break }
}
if (-not $dev) { throw 'No watch connected over adb. Run reconnect-watch.ps1, then run this again.' }
Write-Host "Installing on $dev ..." -ForegroundColor Cyan
& $adb -s $dev install -r $apk; Assert-Ok 'adb install'
& $adb -s $dev shell am start -n dev.tapdrop.spike/.MainActivity; Assert-Ok 'launch'
Write-Host "TapDrop is open on the watch." -ForegroundColor Green
