# Reconnects adb to the watch over Wi-Fi. Pairing is remembered, so no code is needed.
# The watch must be on the same network with Wireless debugging turned on.
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $adb start-server

Write-Host "Looking for the watch (up to 30 s)..." -ForegroundColor Cyan
$target = $null
for ($i = 0; $i -lt 30 -and -not $target; $i++) {
    $line = & $adb mdns services | Select-String '_adb-tls-connect' | Select-Object -First 1
    if ($line) { $target = ($line.ToString() -split '\s+')[-1] } else { Start-Sleep -Seconds 1 }
}
if (-not $target) {
    Write-Host "Watch not found." -ForegroundColor Yellow
    Write-Host "On the watch: Settings > Developer options > Wireless debugging. Turn it off and on, then run this again."
    return
}
Write-Host "Found $target" -ForegroundColor Green
& $adb connect $target
& $adb devices -l
