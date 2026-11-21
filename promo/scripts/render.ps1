# Renders the finished promo to out\tapdrop-promo.mp4 at 1920 x 1080, then prints its
# length and loudness so a silent or clipped soundtrack is caught without listening.
param([string]$Ffmpeg = 'ffmpeg')
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)

if (-not (Get-Command $Ffmpeg -ErrorAction SilentlyContinue)) {
    $found = Get-ChildItem 'C:\ffmpeg*' -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $found) { throw 'ffmpeg not found. Pass its path with -Ffmpeg.' }
    $Ffmpeg = $found.FullName
}

node scripts/make-audio.mjs
if ($LASTEXITCODE -ne 0) { throw 'soundtrack failed' }

New-Item -ItemType Directory -Force out | Out-Null
npx remotion render src/index.ts Promo out/tapdrop-promo.mp4 --codec=h264 --crf=17 --log=error
if ($LASTEXITCODE -ne 0) { throw 'render failed' }

# Eight frames from the finished file on two sheets, as a last look.
Remove-Item out\final-*.jpg -ErrorAction SilentlyContinue
& $Ffmpeg -y -loglevel error -i out/tapdrop-promo.mp4 -vf "select='eq(n\,110)+eq(n\,215)+eq(n\,290)+eq(n\,366)+eq(n\,650)+eq(n\,1010)+eq(n\,1215)+eq(n\,1360)',scale=960:-1,tile=2x2" -fps_mode passthrough -q:v 3 out/final-%02d.jpg

# A picture of the soundtrack: time left to right, pitch bottom to top. The drop at
# 8 s, the taps and the breakdown at 34 s should all be visible in it.
& $Ffmpeg -y -loglevel error -i public/track.wav -lavfi "showspectrumpic=s=1800x500:legend=0:scale=log" out/spectrum.png

$ErrorActionPreference = 'Continue'
$report = & $Ffmpeg -hide_banner -i out/tapdrop-promo.mp4 -af volumedetect -vn -f null NUL 2>&1 | Out-String
$report -split "`n" | Where-Object { $_ -match 'Duration|Video:|Audio:|mean_volume|max_volume' } | ForEach-Object { $_.Trim() }
$size = [math]::Round((Get-Item out\tapdrop-promo.mp4).Length / 1MB, 1)
Write-Host "RENDER DONE: out\tapdrop-promo.mp4, $size MB"
