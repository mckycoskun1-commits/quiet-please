$ErrorActionPreference = 'Stop'
$repo = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$here = $PSScriptRoot
$src = Join-Path $repo 'common\src\main\java\com\quietplease'
$mixins = Join-Path $repo 'common\src\main\resources\quietplease.mixins.json'
$run = Join-Path $repo 'fabric\run'
$stamp = Get-Date -Format 'yyyy-MM-dd-HHmm'
$out = Join-Path $repo "promo\videos\raw-$stamp"
$backup = Join-Path $out 'backup'

$ffmpeg = (Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
if (-not $ffmpeg) { throw 'ffmpeg not found (winget install Gyan.FFmpeg)' }

New-Item -ItemType Directory -Force $backup | Out-Null
Copy-Item (Join-Path $run 'options.txt') $backup
Copy-Item (Join-Path $run 'config\quietplease.json') $backup
$mixinsOriginal = Get-Content $mixins -Raw

$wav = (Join-Path $out 'audio.wav') -replace '\\', '/'
@"
[general]
drivers = wave
frequency = 48000
channels = stereo
sample-type = int16

[wave]
file = $wav
"@ | Set-Content -Encoding ascii (Join-Path $out 'alsoft.ini')

try {
    Copy-Item (Join-Path $here 'PromoDirector.java') $src
    Copy-Item (Join-Path $here 'PromoMinecraftMixin.java') (Join-Path $src 'mixin')
    $patched = $mixinsOriginal -replace '"WeighedSoundEventsMixin"', ('"WeighedSoundEventsMixin",' + "`n    " + '"PromoMinecraftMixin"')
    [IO.File]::WriteAllText($mixins, $patched)

    $env:QUIETPLEASE_PROMO = '1'
    $env:PROMO_OUT = $out
    $env:PROMO_FFMPEG = $ffmpeg
    $env:ALSOFT_CONF = Join-Path $out 'alsoft.ini'
    Push-Location $repo
    & .\gradlew.bat :fabric:runClient
    Pop-Location
}
finally {
    Remove-Item (Join-Path $src 'PromoDirector.java') -ErrorAction SilentlyContinue
    Remove-Item (Join-Path $src 'mixin\PromoMinecraftMixin.java') -ErrorAction SilentlyContinue
    [IO.File]::WriteAllText($mixins, $mixinsOriginal)
    Copy-Item (Join-Path $backup 'options.txt') $run -Force
    Copy-Item (Join-Path $backup 'quietplease.json') (Join-Path $run 'config') -Force
    Remove-Item Env:QUIETPLEASE_PROMO, Env:PROMO_OUT, Env:PROMO_FFMPEG, Env:ALSOFT_CONF -ErrorAction SilentlyContinue
    Push-Location $repo
    & .\gradlew.bat --stop | Out-Null
    Pop-Location
}

Write-Host "Raw recording: $out"
