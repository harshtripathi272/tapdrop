# Turns the rendered promo into the three files the repo publishes:
#   web\promo.mp4          the full video, re-encoded to start playing at once on the web
#   web\promo-poster.jpg   the still shown before it plays
#   docs\promo.gif         an 8 second silent loop for the README, which cannot play video
# Run .\scripts\render.ps1 first.
param([string]$Ffmpeg = 'ffmpeg')
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)

if (-not (Get-Command $Ffmpeg -ErrorAction SilentlyContinue)) {
    $found = Get-ChildItem 'C:\ffmpeg*' -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $found) { throw 'ffmpeg not found. Pass its path with -Ffmpeg.' }
    $Ffmpeg = $found.FullName
}
$src = 'out\tapdrop-promo.mp4'
if (-not (Test-Path $src)) { throw 'out\tapdrop-promo.mp4 is missing. Run .\scripts\render.ps1 first.' }

# Limited-range yuv420p and faststart are what browsers, X and Reddit all expect.
& $Ffmpeg -y -loglevel error -i $src -vf "scale=in_range=full:out_range=tv,format=yuv420p" -c:v libx264 -preset slow -crf 21 -c:a aac -b:a 192k -movflags +faststart ..\web\promo.mp4
if ($LASTEXITCODE -ne 0) { throw 'web video failed' }

& $Ffmpeg -y -loglevel error -ss 3.7 -i $src -frames:v 1 -vf "scale=1280:-1" -q:v 3 ..\web\promo-poster.jpg
if ($LASTEXITCODE -ne 0) { throw 'poster failed' }

# The first tap, then the pay scene. Flat colour needs no dithering, which keeps the file small.
$graph = "[0:v]trim=0:4,setpts=PTS-STARTPTS[a];[0:v]trim=18:22,setpts=PTS-STARTPTS[b];" +
         "[a][b]concat=n=2:v=1,fps=15,scale=720:-1:flags=lanczos,split[s0][s1];" +
         "[s0]palettegen=max_colors=128[p];[s1][p]paletteuse=dither=none"
& $Ffmpeg -y -loglevel error -i $src -filter_complex $graph ..\docs\promo.gif
if ($LASTEXITCODE -ne 0) { throw 'gif failed' }

foreach ($f in '..\web\promo.mp4', '..\web\promo-poster.jpg', '..\docs\promo.gif') {
    $item = Get-Item $f
    Write-Host ("{0,-22} {1,6:N1} MB" -f $item.Name, ($item.Length / 1MB))
}
Write-Host 'WEB ASSETS DONE'
