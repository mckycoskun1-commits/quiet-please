param([string]$Raw)
$ErrorActionPreference = 'Stop'
$repo = Resolve-Path (Join-Path $PSScriptRoot '..\..')
if (-not $Raw) { $Raw = (Get-ChildItem (Join-Path $repo 'promo\videos') -Directory -Filter 'raw-*' | Sort-Object Name | Select-Object -Last 1).FullName }
$Raw = (Resolve-Path $Raw).Path
$inv = [Globalization.CultureInfo]::InvariantCulture
function F([double]$x) { $x.ToString('0.###', $inv) }
function AssTime([double]$x) {
    $cs = [int][Math]::Round($x * 100)
    '{0}:{1:00}:{2:00}.{3:00}' -f [int][Math]::Floor($cs / 360000), [int][Math]::Floor(($cs / 6000) % 60), [int][Math]::Floor(($cs / 100) % 60), ($cs % 100)
}

$ffmpeg = (Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
if (-not $ffmpeg) { throw 'ffmpeg not found (winget install Gyan.FFmpeg)' }
$icon = Join-Path $repo 'art\quietplease-icon.png'
Push-Location $Raw
try {
    $log = & cmd /c "`"$ffmpeg`" -hide_banner -t 60 -i raw.mp4 -vf scale=64:36,signalstats,metadata=print:key=lavfi.signalstats.YAVG -f null - 2>&1"
    $flash = $null
    $t = 0.0
    foreach ($line in $log) {
        if ($line -match 'pts_time:([0-9.]+)') { $t = [double]::Parse($Matches[1], $inv) }
        elseif ($line -match 'YAVG=([0-9.]+)' -and [double]::Parse($Matches[1], $inv) -gt 225) { $flash = $t; break }
    }
    if ($null -eq $flash) { throw 'White sync frame not found in raw.mp4' }
    $log = & cmd /c "`"$ffmpeg`" -hide_banner -i audio.wav -af silencedetect=n=-60dB:d=0.5 -f null - 2>&1"
    $onset = $null
    $longest = 0.0
    foreach ($line in $log) {
        if ($line -match 'silence_end: ([0-9.]+) \| silence_duration: ([0-9.]+)') {
            $d = [double]::Parse($Matches[2], $inv)
            if ($d -gt $longest) { $longest = $d; $onset = [double]::Parse($Matches[1], $inv) }
        }
    }
    if ($null -eq $onset) { throw 'Sync note not found in audio.wav' }
    Write-Host "Sync: white frame at $(F $flash) s in raw.mp4, note at $(F $onset) s in audio.wav"

    & $ffmpeg -hide_banner -loglevel error -y -ss (F $flash) -i raw.mp4 -ss (F $onset) -i audio.wav -map 0:v -map 1:a `
        -c:v h264_nvenc -preset p5 -cq 16 -pix_fmt yuv420p -c:a pcm_s16le -shortest synced.mkv
    if ($LASTEXITCODE -ne 0) { throw 'Sync step failed' }

    $segs = @(
        @{ S = 0.6; E = 6.0; Kind = 'world'; Cap = @(@{ T = 0.4; D = 4.6; Text = 'That one song again?' }) },
        @{ S = 8.0; E = 14.6; Kind = 'panel'; Cap = @(@{ T = 0.3; D = 6.0; Text = 'Turn it off. It stops right away.' }) },
        @{ S = 15.5; E = 21.2; Kind = 'panel'; Cap = @(@{ T = 0.3; D = 5.1; Text = 'Too loud? Turn down just that song.' }) },
        @{ S = 22.5; E = 30.0; Kind = 'panel'; Cap = @(@{ T = 0.3; D = 6.9; Text = 'Your own .ogg songs, right in the panel'; New = $true }) },
        @{ S = 30.5; E = 41.6; Kind = 'panel'; Cap = @(@{ T = 0.3; D = 10.5; Text = 'Build your own playlist' }) },
        @{ S = 43.0; E = 48.8; Kind = 'world'; Cap = @(@{ T = 1.2; D = 4.3; Text = 'Skip with a smooth crossfade' }) },
        @{ S = 49.5; E = 56.6; Kind = 'world'; Cap = @(@{ T = 0.4; D = 3.4; Text = 'Go back a song'; New = $true }, @{ T = 3.9; D = 2.9; Text = 'or never hear it again'; New = $true }) },
        @{ S = 57.0; E = 68.0; Kind = 'panel'; Cap = @(@{ T = 0.3; D = 10.4; Text = 'History and stats'; New = $true }) },
        @{ S = 68.5; E = 76.5; Kind = 'end'; Cap = @() }
    )

    foreach ($fmt in @('16x9', '9x16')) {
        $vertical = $fmt -eq '9x16'
        $W = if ($vertical) { 1080 } else { 1920 }
        $H = if ($vertical) { 1920 } else { 1080 }
        $g = New-Object System.Collections.Generic.List[string]
        $n = $segs.Count
        $g.Add("[0:v]split=$n" + (-join (0..($n - 1) | ForEach-Object { "[v$_]" })))
        $g.Add("[0:a]asplit=$n" + (-join (0..($n - 1) | ForEach-Object { "[a$_]" })))
        $events = New-Object System.Collections.Generic.List[string]
        $pos = 0.0
        for ($i = 0; $i -lt $n; $i++) {
            $s = $segs[$i]
            $len = $s.E - $s.S
            $chain = "[v$i]trim=start=$(F $s.S):end=$(F $s.E),setpts=PTS-STARTPTS"
            if ($vertical -and $s.Kind -eq 'panel') {
                $g.Add("$chain,split[p$i][q$i]")
                $g.Add("[q$i]scale=1080:1920,boxblur=40:4,eq=brightness=-0.1:saturation=1.2[pb$i]")
                $g.Add("[p$i]crop=1400:1080:260:0,scale=1080:834[pf$i]")
                $g.Add("[pb$i][pf$i]overlay=0:440[w$i]")
            }
            elseif ($vertical) {
                $g.Add("$chain,crop=608:1080:656:0,scale=1080:1920[w$i]")
            }
            else {
                $g.Add("$chain[w$i]")
            }
            if ($s.Kind -eq 'end') {
                $iconSize = if ($vertical) { 340 } else { 280 }
                $iconY = if ($vertical) { 560 } else { 170 }
                $g.Add("[w$i]split[e$i][f$i]")
                $g.Add("[f$i]boxblur=24:3,eq=brightness=-0.22,format=yuva420p,fade=t=in:st=0.8:d=0.8:alpha=1[fb$i]")
                $g.Add("[e$i][fb$i]overlay[eb$i]")
                $g.Add("[1:v]scale=$($iconSize):$($iconSize):flags=neighbor,format=rgba,fade=t=in:st=1.2:d=0.6:alpha=1[ic]")
                $g.Add("[eb$i][ic]overlay=(W-w)/2:$($iconY):shortest=1,fade=t=out:st=$(F ($len - 0.8)):d=0.8,setsar=1[x$i]")
                $cx = $W / 2
                $ty = if ($vertical) { 1010 } else { 560 }
                $step = if ($vertical) { 70 } else { 62 }
                $t0 = AssTime ($pos + 1.6)
                $t1 = AssTime ($pos + $len)
                $events.Add("Dialogue: 0,$t0,$t1,EndTitle,,0,0,0,,{\an5\pos($cx,$ty)\fad(500,600)}Quiet Please 2.0")
                $lines = @('Fabric, Forge, NeoForge, Quilt', 'Minecraft 1.18.2 to 26.3', 'Free on CurseForge')
                for ($k = 0; $k -lt $lines.Count; $k++) {
                    $ly = $ty + 90 + $k * $step
                    $events.Add("Dialogue: 0,$(AssTime ($pos + 2.0 + 0.25 * $k)),$t1,EndLine,,0,0,0,,{\an5\pos($cx,$ly)\fad(400,600)}$($lines[$k])")
                }
            }
            else {
                $g.Add("[w$i]setsar=1[x$i]")
            }
            $afadeOut = if ($i -eq $n - 1) { 1.5 } else { 0.15 }
            $g.Add("[a$i]atrim=start=$(F $s.S):end=$(F $s.E),asetpts=PTS-STARTPTS,afade=t=in:d=0.08,afade=t=out:st=$(F ($len - $afadeOut)):d=$(F $afadeOut)[y$i]")
            foreach ($c in $s.Cap) {
                $style = if ($s.Kind -eq 'world') { 'Top' } else { 'Bottom' }
                $mv = ''
                if ($vertical) { $style = 'Top'; $mv = if ($s.Kind -eq 'world') { '0,0,330' } else { '0,0,1360' } } else { $mv = '0,0,0' }
                $text = if ($c.New) { '{\c&H00D7FF&}NEW  {\c&HFFFFFF&}' + $c.Text } else { $c.Text }
                $events.Add("Dialogue: 0,$(AssTime ($pos + $c.T)),$(AssTime ($pos + $c.T + $c.D)),$style,,$mv,,{\fad(200,250)}$text")
            }
            $pos += $len
        }
        $g.Add((-join (0..($n - 1) | ForEach-Object { "[x$_][y$_]" })) + "concat=n=$($n):v=1:a=1[cv][ca]")
        $g.Add("[cv]subtitles=filename=captions-$($fmt).ass:fontsdir='C\:/Windows/Fonts'[outv]")
        $g.Add("[ca]loudnorm=I=-16:TP=-1.5:LRA=11,aresample=48000[outa]")
        ($g -join ";`n") | Set-Content -Encoding ascii "graph-$fmt.txt"

        $capSize = if ($vertical) { 66 } else { 58 }
        $topMargin = if ($vertical) { 330 } else { 60 }
        $ass = @"
[Script Info]
ScriptType: v4.00+
PlayResX: $W
PlayResY: $H
WrapStyle: 0
ScaledBorderAndShadow: yes

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Top,Segoe UI,$capSize,&H00FFFFFF,&H00FFFFFF,&H00101010,&H80000000,-1,0,0,0,100,100,0,0,1,4,2,8,70,70,$topMargin,1
Style: Bottom,Segoe UI,$capSize,&H00FFFFFF,&H00FFFFFF,&H50000000,&H50000000,-1,0,0,0,100,100,0,0,3,14,0,2,70,70,30,1
Style: EndTitle,Segoe UI,$(if ($vertical) { 104 } else { 92 }),&H00FFFFFF,&H00FFFFFF,&H00101010,&H80000000,-1,0,0,0,100,100,0,0,1,4,3,5,40,40,0,1
Style: EndLine,Segoe UI,$(if ($vertical) { 54 } else { 46 }),&H00E0E0E0,&H00FFFFFF,&H00101010,&H80000000,-1,0,0,0,100,100,0,0,1,3,2,5,40,40,0,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
"@
        ($ass + "`n" + ($events -join "`n") + "`n") | Set-Content -Encoding utf8 "captions-$fmt.ass"

        & $ffmpeg -hide_banner -loglevel error -y -i synced.mkv -loop 1 -framerate 60 -i $icon -/filter_complex "graph-$fmt.txt" `
            -map '[outv]' -map '[outa]' -c:v h264_nvenc -preset p6 -cq 19 -pix_fmt yuv420p -r 60 -c:a aac -b:a 192k -movflags +faststart "promo-$fmt.mp4"
        if ($LASTEXITCODE -ne 0) { throw "Edit failed: $fmt" }
        Write-Host "Done: $(Join-Path $Raw "promo-$fmt.mp4")"
    }
}
finally {
    Pop-Location
}
