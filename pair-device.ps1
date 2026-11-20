# Pairs a new Android device with adb over Wi-Fi, then connects to it.
# On the device: Developer options > Wireless debugging > Pair device with pairing code.
# You type the 6 digit code here yourself.
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $adb start-server

Write-Host ""
Write-Host "Waiting for a pairing screen (up to 5 min)..." -ForegroundColor Cyan
Write-Host "Phone: Settings > Developer options > Wireless debugging > Pair device with pairing code"
# Ignore pairing screens on devices that are already connected, such as the watch.
$connected = @(& $adb devices | Select-String "^(\d+\.\d+\.\d+\.\d+):\d+\s+device" | ForEach-Object { $_.Matches[0].Groups[1].Value })
$target = $null
for ($i = 0; $i -lt 300 -and -not $target; $i++) {
    foreach ($line in (& $adb mdns services | Select-String '_adb-tls-pairing')) {
        $candidate = ($line.ToString() -split '\s+')[-1]
        if ($connected -notcontains ($candidate -split ':')[0]) { $target = $candidate; break }
    }
    if (-not $target) { Start-Sleep -Seconds 1 }
}
if (-not $target) { Write-Host "No pairing screen seen. Run this again when the phone shows the code." -ForegroundColor Yellow; return }

Write-Host "Found pairing service at $target" -ForegroundColor Green
Write-Host "Type the 6 digit code shown on the device:" -ForegroundColor Cyan
& $adb pair $target

# Connect to the same device's debugging port, not another device's.
$ip = ($target -split ':')[0]
Start-Sleep -Seconds 2
$conn = & $adb mdns services | Select-String '_adb-tls-connect' | Where-Object { $_.ToString() -match [regex]::Escape($ip) } | Select-Object -First 1
if ($conn) { & $adb connect (($conn.ToString() -split '\s+')[-1]) } else { Write-Host "Paired, but no connect service found for $ip yet." -ForegroundColor Yellow }
& $adb devices -l
Write-Host ""
Write-Host "Done. You can leave this tab open." -ForegroundColor Green
