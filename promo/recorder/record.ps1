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
$stats = Join-Path $run 'config\quietplease-stats.json'
$hadStats = Test-Path $stats
if ($hadStats) { Copy-Item $stats $backup }
$mixinsOriginal = Get-Content $mixins -Raw

$musicDir = Join-Path $run 'config\quietplease\music'
New-Item -ItemType Directory -Force $musicDir | Out-Null
$songs = @(
    @{ Name = 'Campfire Lofi'; P = '2.4'; K = '1' },
    @{ Name = 'Morning Walk'; P = '2.0'; K = '1.1225' },
    @{ Name = 'Rainy Day'; P = '3.0'; K = '0.8909' }
)
$songFiles = @()
foreach ($s in $songs) {
    $P = $s.P
    $K = $s.K
    $graph = Join-Path $out ("song-" + ($s.Name -replace ' ', '-') + '.txt')
    @"
aevalsrc=exprs='st(0,mod(floor(t/$P),4));st(1,$K*if(eq(ld(0),0),261.63,if(eq(ld(0),1),220,if(eq(ld(0),2),174.61,196))));st(2,$K*if(eq(ld(0),0),329.63,if(eq(ld(0),1),261.63,if(eq(ld(0),2),220,246.94))));st(3,$K*if(eq(ld(0),0),392,if(eq(ld(0),1),329.63,if(eq(ld(0),2),261.63,293.66))));st(4,sin(PI*mod(t,$P)/$P));st(5,floor(t*8/$P));st(6,t-ld(5)*$P/8);st(7,mod(ld(5),4));st(8,2*if(eq(ld(7),0),ld(1),if(eq(ld(7),1),ld(2),if(eq(ld(7),2),ld(3),ld(2)))));st(9,exp(-7*ld(6))*min(1,($P/8-ld(6))*40));0.07*ld(4)*(sin(2*PI*ld(1)*t)+sin(2*PI*ld(2)*t)+sin(2*PI*ld(3)*t))+0.10*ld(4)*sin(PI*ld(1)*t)+0.16*ld(9)*sin(2*PI*ld(8)*ld(6))*(1+0.3*sin(4*PI*ld(8)*ld(6)))':s=48000:d=75,lowpass=f=3500,aecho=0.8:0.7:180|360:0.25|0.15,volume=8dB,afade=t=in:d=1.5,afade=t=out:st=72:d=3,aformat=channel_layouts=stereo[out]
"@ | Set-Content -Encoding ascii $graph
    $file = Join-Path $musicDir ($s.Name + '.ogg')
    & $ffmpeg -hide_banner -loglevel error -y -/filter_complex $graph -map '[out]' -c:a libvorbis -q:a 4 $file
    if ($LASTEXITCODE -ne 0) { throw "Demo song failed: $($s.Name)" }
    $songFiles += $file
}

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
    if ($hadStats) { Copy-Item (Join-Path $backup 'quietplease-stats.json') (Join-Path $run 'config') -Force }
    else { Remove-Item $stats -ErrorAction SilentlyContinue }
    foreach ($f in $songFiles) { Remove-Item $f -ErrorAction SilentlyContinue }
    Remove-Item Env:QUIETPLEASE_PROMO, Env:PROMO_OUT, Env:PROMO_FFMPEG, Env:ALSOFT_CONF -ErrorAction SilentlyContinue
    Push-Location $repo
    & .\gradlew.bat --stop | Out-Null
    Pop-Location
}

Write-Host "Raw recording: $out"
