param(
    [string]$Node = '26.1.2-fabric',
    [ValidateSet('ogg', 'mp3')]
    [string]$Format = 'ogg',
    [string]$Recording
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$run = Join-Path $repo "versions\$Node\run"
$inv = [Globalization.CultureInfo]::InvariantCulture

$ffmpeg = (Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
if (-not $ffmpeg) { throw 'ffmpeg not found (winget install Gyan.FFmpeg)' }
$ffprobe = Join-Path (Split-Path $ffmpeg) 'ffprobe.exe'

function Invoke-Tool([string]$exe, [string[]]$arguments) {
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $exe
    $psi.Arguments = ($arguments | ForEach-Object { if ($_ -match '[\s"]') { '"' + ($_ -replace '"', '\"') + '"' } else { $_ } }) -join ' '
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false
    $p = [System.Diagnostics.Process]::Start($psi)
    $stdout = $p.StandardOutput.ReadToEndAsync()
    $stderr = $p.StandardError.ReadToEnd()
    $p.WaitForExit()
    return $stdout.Result + $stderr
}

function F([double]$x) { $x.ToString('0.000', $inv) }

if ($Recording) {
    $out = (Resolve-Path $Recording).Path
}
else {
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $out = Join-Path $repo "build\audio-test\$Node-$stamp"
    New-Item -ItemType Directory -Force $out | Out-Null
    $musicDir = Join-Path $run 'config\quietplease\music'
    New-Item -ItemType Directory -Force $musicDir | Out-Null
    $songs = @(@{ File = "qp_audiotest_a.$Format"; Hz = 440 }, @{ File = "qp_audiotest_b.$Format"; Hz = 1000 })
    $codec = if ($Format -eq 'mp3') { @('-c:a', 'libmp3lame', '-b:a', '192k') } else { @('-c:a', 'libvorbis', '-q:a', '4') }
    foreach ($s in $songs) {
        $null = Invoke-Tool $ffmpeg (@('-hide_banner', '-loglevel', 'error', '-y', '-f', 'lavfi', '-i', "sine=frequency=$($s.Hz):duration=60:sample_rate=48000",
            '-af', 'volume=6') + $codec + @((Join-Path $musicDir $s.File)))
    }

    $wavPath = (Join-Path $out 'audio.wav') -replace '\\', '/'
    @"
[general]
drivers = wave
frequency = 48000
channels = stereo
sample-type = int16

[wave]
file = $wavPath
"@ | Set-Content -Encoding ascii (Join-Path $out 'alsoft.ini')

    try {
        $env:QUIETPLEASE_AUTODUMP = 'audio'
        $env:ALSOFT_CONF = Join-Path $out 'alsoft.ini'
        $gradle = Start-Process -FilePath (Join-Path $repo 'gradlew.bat') -ArgumentList ":${Node}:runClient", '--console=plain', '--no-daemon' `
            -WorkingDirectory $repo -RedirectStandardOutput (Join-Path $out 'gradle.log') -RedirectStandardError (Join-Path $out 'gradle-err.log') `
            -NoNewWindow -PassThru
        $gradle.WaitForExit()
    }
    finally {
        foreach ($s in $songs) { Remove-Item (Join-Path $musicDir $s.File) -ErrorAction SilentlyContinue }
        Remove-Item Env:QUIETPLEASE_AUTODUMP, Env:ALSOFT_CONF -ErrorAction SilentlyContinue
    }
    Copy-Item (Join-Path $run 'logs\latest.log') (Join-Path $out 'latest.log')
}

$wav = Join-Path $out 'audio.wav'
$log = Join-Path $out 'latest.log'
$marks = @{}
foreach ($line in Get-Content $log) {
    if ($line -match 'AUDIOTEST mark (\w+) (\d+)') { $marks[$Matches[1]] = [int64]$Matches[2] }
    elseif ($line -match 'AUDIOTEST|SELFTEST (BASARILI|HATA)') { Write-Host $line }
}
if (-not $marks.ContainsKey('end') -or -not (Test-Path $wav)) { throw "Test did not finish, see $out" }

$item = Get-Item $wav
$durText = (Invoke-Tool $ffprobe @('-v', 'error', '-show_entries', 'format=duration', '-of', 'csv=p=0', $wav)).Trim()
$duration = 0.0
if (-not [double]::TryParse($durText, [Globalization.NumberStyles]::Float, $inv, [ref]$duration)) { $duration = ($item.Length - 68) / 192000.0 }
$startMs = [DateTimeOffset]::new($item.LastWriteTimeUtc).ToUnixTimeMilliseconds() - $duration * 1000.0

$offset = 0.0
$cueGuess = ($marks['cue'] - $startMs) / 1000.0
$from = [Math]::Max(0, $cueGuess - 1.5)
$sd = Invoke-Tool $ffmpeg @('-hide_banner', '-nostats', '-i', $wav, '-af', "atrim=start=$(F $from):end=$(F ($cueGuess + 1.5)),asetpts=PTS-STARTPTS,silencedetect=noise=-50dB:d=0.4", '-f', 'null', '-')
$ends = [regex]::Matches($sd, 'silence_end: ([\d.]+)') | ForEach-Object { $from + [double]::Parse($_.Groups[1].Value, $inv) }
if ($ends) {
    $onset = $ends | Sort-Object { [Math]::Abs($_ - $cueGuess) } | Select-Object -First 1
    $offset = $onset - $cueGuess
    Write-Host "Cue found $(F $offset) s from the predicted position"
}
else {
    Write-Host 'Cue not found, using file timing only'
}

function Level([double]$a, [double]$b, $hz) {
    $af = "atrim=start=$(F $a):end=$(F $b)"
    if ($hz) { for ($i = 0; $i -lt 4; $i++) { $af += ",bandpass=f=${hz}:width_type=q:w=8" } }
    $r = Invoke-Tool $ffmpeg @('-hide_banner', '-nostats', '-i', $wav, '-af', "$af,volumedetect", '-f', 'null', '-')
    $m = [regex]::Match($r, 'mean_volume: (-?[\d.]+) dB')
    if ($m.Success) { return [double]::Parse($m.Groups[1].Value, $inv) }
    return -999.0
}

$loud = -40.0
$quiet = -60.0
$checks = @(
    @{ Name = 'silence before cue'; Mark = 'silence'; From = 0.3; To = 1.6; Loud = $null; Quiet = 'all' },
    @{ Name = 'preview of own song (440 Hz)'; Mark = 'previewA'; From = 0.8; To = 4.5; Loud = 440; Quiet = 1000 },
    @{ Name = 'silence after preview stop'; Mark = 'previewStop1'; From = 0.3; To = 2.7; Loud = $null; Quiet = 'all' },
    @{ Name = 'own song as background (1000 Hz)'; Mark = 'backgroundB'; From = 0.8; To = 4.5; Loud = 1000; Quiet = 440 },
    @{ Name = 'preview over muted background'; Mark = 'previewA2'; From = 0.8; To = 3.5; Loud = 440; Quiet = 1000 },
    @{ Name = 'background back after preview stop'; Mark = 'previewStop2'; From = 0.3; To = 4.5; Loud = 1000; Quiet = 440 }
)
$failed = 0
$report = @()
$times = $marks.Values | Sort-Object
foreach ($c in $checks) {
    $mark = $marks[$c.Mark]
    $t = ($mark - $startMs) / 1000.0 + $offset
    $a = $t + $c.From
    $b = $t + $c.To
    $next = $times | Where-Object { $_ -gt $mark } | Select-Object -First 1
    if ($next) { $b = [Math]::Min($b, ($next - $startMs) / 1000.0 + $offset - 0.15) }
    $all = Level $a $b $null
    $l440 = Level $a $b 440
    $l1000 = Level $a $b 1000
    if ($c.Quiet -eq 'all') { $ok = $all -lt $quiet }
    else {
        $lv = if ($c.Loud -eq 440) { $l440 } else { $l1000 }
        $qv = if ($c.Quiet -eq 440) { $l440 } else { $l1000 }
        $ok = ($lv -gt $loud) -and ($qv -lt $quiet)
    }
    if (-not $ok) { $failed++ }
    $report += [string]::Format($inv, '{0,-4} {1,-36} {2,7:0.00}-{3,-7:0.00} s  all {4,6:0.0} dB  440Hz {5,6:0.0} dB  1000Hz {6,6:0.0} dB',
        $(if ($ok) { 'OK' } else { 'FAIL' }), $c.Name, $a, $b, $all, $l440, $l1000)
}
$report | Set-Content -Encoding utf8 (Join-Path $out 'report.txt')
$report | ForEach-Object { Write-Host $_ }
Write-Host "Recording: $out"
if ($failed -gt 0) { Write-Host "AUDIO TEST FAILED ($failed)"; exit 1 }
Write-Host 'AUDIO TEST PASSED'
