# Renders a half-size draft of the promo and cuts contact sheets from it, four frames
# to a sheet, so the whole video can be checked at a glance.
#   .\scripts\preview.ps1                  default set of frames
#   .\scripts\preview.ps1 -Frames 60,345   your own frames (a multiple of four fills the sheets)
param(
    [int[]]$Frames = @(12, 56, 64, 110, 126, 170, 215, 234, 244, 290, 304, 345, 362, 368, 410, 470,
                       500, 575, 606, 650, 730, 800, 850, 890, 950, 966, 1010, 1050, 1084, 1130, 1150, 1185,
                       1215, 1250, 1275, 1360),
    [string]$Ffmpeg = 'ffmpeg'
)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)

if (-not (Get-Command $Ffmpeg -ErrorAction SilentlyContinue)) {
    $found = Get-ChildItem 'C:\ffmpeg*' -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $found) { throw 'ffmpeg not found. Pass its path with -Ffmpeg.' }
    $Ffmpeg = $found.FullName
}

New-Item -ItemType Directory -Force out | Out-Null
Remove-Item out\sheet-*.jpg -ErrorAction SilentlyContinue

npx remotion render src/index.ts Promo out/draft.mp4 --scale=0.5 --crf=26 --log=error
if ($LASTEXITCODE -ne 0) { throw 'draft render failed' }

$select = ($Frames | ForEach-Object { "eq(n\,$_)" }) -join '+'
& $Ffmpeg -y -loglevel error -i out/draft.mp4 -vf "select='$select',tile=2x2" -fps_mode passthrough -q:v 3 out/sheet-%02d.jpg
if ($LASTEXITCODE -ne 0) { throw 'cutting sheets failed' }

Write-Host "PREVIEW DONE: $((Get-ChildItem out\sheet-*.jpg).Count) sheets in out\"
