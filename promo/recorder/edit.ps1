param([string]$Raw, [string]$Video)
$ErrorActionPreference = 'Stop'
$repo = Resolve-Path (Join-Path $PSScriptRoot '..\..')
if (-not $Raw) { $Raw = (Get-ChildItem (Join-Path $repo 'promo\videos') -Directory -Filter 'raw-*' | Sort-Object Name | Select-Object -Last 1).FullName }
$Raw = (Resolve-Path $Raw).Path
$Video = if ($Video) { (Resolve-Path $Video).Path } else { $Raw }
$rawVideo = Join-Path $Video 'raw.mp4'
$inv = [Globalization.CultureInfo]::InvariantCulture
function F([double]$x) { $x.ToString('0.######', $inv) }
function Ms([double]$x) { [Math]::Round($x * 1000).ToString($inv) }
function P([string]$s) { [double]::Parse($s, $inv) }
function AssTime([double]$x) {
    $cs = [int][Math]::Round($x * 100)
    '{0}:{1:00}:{2:00}.{3:00}' -f [int][Math]::Floor($cs / 360000), [int][Math]::Floor(($cs / 6000) % 60), [int][Math]::Floor(($cs / 100) % 60), ($cs % 100)
}
function Nearest($list, [double]$target, [double]$tol) {
    $best = $null
    foreach ($v in $list) { if ([Math]::Abs($v - $target) -le $tol -and ($null -eq $best -or [Math]::Abs($v - $target) -lt [Math]::Abs($best - $target))) { $best = $v } }
    $best
}
function ZoomFilter($rect, [double]$tin, [double]$tout, [double]$maxW) {
    $x = $rect[0]; $y = $rect[1]; $w = $rect[2]; $h = $rect[3]
    $z = [Math]::Min(1.7, [Math]::Min(0.94 * $maxW / $w, 0.85 * 1080 / $h))
    $cx = $x + $w / 2
    $cy = $y + $h / 2
    $t = '(in/60)'
    $e = "(1+$(F ($z - 1))*min(st(0,clip(($t-$(F $tin))/0.5,0,1))*ld(0)*(3-2*ld(0)),st(1,clip(($(F $tout)-$t)/0.5,0,1))*ld(1)*(3-2*ld(1))))"
    $x0 = "clip($(F $cx)-W/(2*$e),0,W-W/$e)"
    $y0 = "clip($(F $cy)-H/(2*$e),0,H-H/$e)"
    $x1 = "($x0+W/$e)"
    $y1 = "($y0+H/$e)"
    "perspective=x0='$x0':y0='$y0':x1='$x1':y1='$y0':x2='$x0':y2='$y1':x3='$x1':y3='$y1':sense=source:interpolation=cubic:eval=frame"
}

$ffmpeg = (Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
if (-not $ffmpeg) { throw 'ffmpeg not found (winget install Gyan.FFmpeg)' }
$icon = Join-Path $repo 'art\quietplease-icon.png'
Push-Location $Raw
try {
    $marks = @{}
    $rects = @{}
    foreach ($line in Get-Content (Join-Path $Video 'marks.txt')) {
        $p = $line -split ' '
        $marks[$p[0]] = (P $p[1]) / 1000
        if ($p.Count -ge 6) { $rects[$p[0]] = @((P $p[2]), (P $p[3]), (P $p[4]), (P $p[5])) }
    }

    $log = & cmd /c "`"$ffmpeg`" -hide_banner -i `"$rawVideo`" -vf scale=64:36,signalstats,metadata=print:key=lavfi.signalstats.YAVG -f null - 2>&1"
    $flashes = New-Object System.Collections.Generic.List[double]
    $t = 0.0
    $prevWhite = $false
    foreach ($line in $log) {
        if ($line -match 'pts_time:([0-9.]+)') { $t = P $Matches[1] }
        elseif ($line -match 'YAVG=([0-9.]+)') {
            $white = (P $Matches[1]) -gt 225
            if ($white -and -not $prevWhite) { $flashes.Add($t) }
            $prevWhite = $white
        }
    }
    if ($flashes.Count -eq 0) { throw 'White sync frame not found in raw.mp4' }

    $log = & cmd /c "`"$ffmpeg`" -hide_banner -i audio.wav -af silencedetect=n=-60dB:d=0.5 -f null - 2>&1"
    $onsets = New-Object System.Collections.Generic.List[double]
    $first = $null
    $longest = 0.0
    foreach ($line in $log) {
        if ($line -match 'silence_end: ([0-9.]+) \| silence_duration: ([0-9.]+)') {
            $onsets.Add((P $Matches[1]))
            $d = P $Matches[2]
            if ($d -gt $longest) { $longest = $d; $first = P $Matches[1] }
        }
    }
    if ($null -eq $first) { throw 'Sync note not found in audio.wav' }

    $v1 = $flashes[0]
    $a1 = $first
    $cues = New-Object System.Collections.Generic.List[object]
    $cues.Add(@{ Name = 'cue1'; V = $v1; A = $a1 })
    foreach ($name in @('cue2', 'cue3', 'cue4')) {
        if (-not $marks.ContainsKey($name)) { continue }
        $dt = $marks[$name] - $marks['cue1']
        $v = Nearest $flashes ($v1 + $dt) 0.5
        $a = Nearest $onsets ($a1 + $dt) 0.5
        if ($null -ne $v -and $null -ne $a) { $cues.Add(@{ Name = $name; V = $v; A = $a }) }
        else { Write-Host "Cue $name not found (video $v, audio $a)" }
    }
    $last = $cues[$cues.Count - 1]
    $report = New-Object System.Collections.Generic.List[string]
    $report.Add("cue1: video $(F $v1) s, audio $(F $a1) s")
    foreach ($c in $cues) {
        if ($c.Name -eq 'cue1') { continue }
        $c.Off = ($c.A - $a1) - ($c.V - $v1)
        $report.Add("$($c.Name): offset with one start cue $(Ms $c.Off) ms (positive = sound late)")
    }
    $used = @($cues[0], $last)
    if ($cues.Count -ge 2) {
        $r = ($last.A - $a1) / ($last.V - $v1)
        $report.Add("linear stretch from cue1 and $($last.Name): audio rate x$(F $r)")
        $worst = 0.0
        foreach ($c in $cues) {
            if ($c.Name -eq 'cue1' -or $c.Name -eq $last.Name) { continue }
            $res = ($c.A - $a1) / $r - ($c.V - $v1)
            $report.Add("$($c.Name): offset after the linear stretch $(Ms $res) ms")
            $worst = [Math]::Max($worst, [Math]::Abs($res))
        }
        if ($worst -gt 0.02) {
            $used = @($cues)
            $report.Add('mid cues off by more than 20 ms: stretching piece by piece between all cues (0 ms at every cue)')
        }
    }
    else { $used = @($cues[0]) }

    $af = New-Object System.Collections.Generic.List[string]
    $labels = ''
    if ($used.Count -ge 2) {
        for ($i = 0; $i -lt $used.Count - 1; $i++) {
            $ra = ($used[$i + 1].A - $used[$i].A) / ($used[$i + 1].V - $used[$i].V)
            $rate = [int][Math]::Round(48000 * $ra)
            $af.Add("[1:a]atrim=start=$(F $used[$i].A):end=$(F $used[$i + 1].A),asetpts=N/SR/TB,asetrate=$rate,aresample=48000[ap$i]")
            $labels += "[ap$i]"
        }
        $af.Add("$($labels)concat=n=$($used.Count - 1):v=0:a=1[aout]")
        $dur = $last.V - $v1
    }
    else {
        $af.Add("[1:a]atrim=start=$(F $a1),asetpts=N/SR/TB[aout]")
        $dur = 90
    }
    ($af -join ";`n") | Set-Content -Encoding ascii 'graph-sync.txt'
    $drops = (Get-Content (Join-Path $Video 'ffmpeg.log') -ErrorAction SilentlyContinue | Select-String 'frame=' | Select-Object -Last 1)
    if ($drops) { $report.Add("capture: " + ($drops.Line -replace '\s+', ' ').Trim()) }
    $report | Set-Content -Encoding ascii 'sync.txt'
    $report | ForEach-Object { Write-Host $_ }

    & $ffmpeg -hide_banner -loglevel error -y -ss (F $v1) -t (F $dur) -i $rawVideo -i audio.wav -/filter_complex 'graph-sync.txt' `
        -map 0:v -map '[aout]' -fps_mode cfr -r 60 -c:v h264_nvenc -preset p5 -cq 16 -pix_fmt yuv420p -c:a pcm_s16le -shortest synced.mkv
    if ($LASTEXITCODE -ne 0) { throw 'Sync step failed' }

    function Levels([string]$pre, [string]$key) {
        $log = & cmd /c "`"$ffmpeg`" -hide_banner -i synced.mkv -vn -af $pre,asetnsamples=240,astats=metadata=1:reset=1:measure_perchannel=none:measure_overall=$key,ametadata=print:key=lavfi.astats.Overall.$key -f null - 2>&1"
        $list = New-Object System.Collections.Generic.List[double]
        foreach ($line in $log) {
            if ($line -match "$key=(\S+)") {
                $v = $Matches[1]
                $list.Add($(if ($v -match '^-?[0-9.]+$') { P $v } else { -120.0 }))
            }
        }
        , $list
    }
    function PowerMean($list, [int]$from, [int]$to) {
        $sum = 0.0
        $cnt = 0
        for ($k = [Math]::Max(0, $from); $k -le [Math]::Min($list.Count - 1, $to); $k++) { $sum += [Math]::Pow(10, $list[$k] / 10); $cnt++ }
        if ($cnt -eq 0 -or $sum -le 0) { return -120.0 }
        10 * [Math]::Log10($sum / $cnt)
    }
    $clickOver = 4.0
    $hf = Levels 'highpass=f=3000,highpass=f=3000' 'Peak_level'
    $bb = Levels 'anull' 'RMS_level'
    $shape = '0'
    $clickReport = New-Object System.Collections.Generic.List[string]
    $lastClick = -1000
    for ($k = 0; $k -lt $hf.Count; $k++) {
        if ($hf[$k] -le -30 -or $k - $lastClick -lt 60) { continue }
        $lastClick = $k
        $c = $k * 0.005
        $depth = [Math]::Min(30.0, [Math]::Max(0.0, (PowerMean $bb ($k - 1) ($k + 8)) - (PowerMean $bb ($k - 100) ($k - 20)) - $clickOver))
        $clickReport.Add("$(F $c)/$([Math]::Round($depth))")
        if ($depth -gt 0) { $shape = "max($shape,$(F $depth)*clip(min((t-$(F ($c - 0.01)))/0.005,($(F ($c + 0.05))-t)/0.02),0,1))" }
    }
    $duck = if ($shape -ne '0') { "asetnsamples=n=48,volume='pow(10,-($shape)/20)':eval=frame," } else { '' }
    Add-Content -Encoding ascii 'sync.txt' "clicks (time s/cut dB, each click ends $(F $clickOver) dB above the music before it): $($clickReport -join ' ')"
    Write-Host "clicks: $($clickReport -join ' ')"

    $segs = @(
        @{ S = 1.8; E = 6.0; Kind = 'world'; Cap = @(@{ T = 0.3; D = 3.7; Text = 'That one song again?' }) },
        @{ S = 8.0; E = 14.6; Kind = 'panel'; Cap = @(@{ T = 0.3; D = 6.0; Text = 'Turn it off. It stops right away.' }) },
        @{ S = 15.5; E = 21.2; Kind = 'panel'; Zoom = @{ Rect = 'zoom3'; In = 17.7; Out = 20.9 }; Cap = @(@{ T = 0.3; D = 5.1; Text = 'Too loud? Turn down just that song.' }) },
        @{ S = 22.5; E = 30.45; Kind = 'panel'; Cap = @(@{ T = 0.3; D = 7.3; Text = 'Your own .ogg songs, right in the panel' }) },
        @{ S = 30.5; E = 41.6; Kind = 'panel'; Cap = @(@{ T = 0.3; D = 10.5; Text = 'Build your own playlist' }) },
        @{ S = 43.0; E = 48.8; Kind = 'world'; Cap = @(@{ T = 1.2; D = 4.3; Text = 'Skip with a smooth crossfade' }) },
        @{ S = 49.5; E = 56.6; Kind = 'world'; Cap = @(@{ T = 0.4; D = 3.4; Text = 'Go back a song' }, @{ T = 3.9; D = 2.9; Text = 'or never hear it again' }) },
        @{ S = 57.0; E = 68.0; Kind = 'panel'; Zoom = @{ Rect = 'zoom8'; In = 63.2; Out = 66.1 }; Cap = @(@{ T = 0.3; D = 10.4; Text = 'History and stats' }) },
        @{ S = 68.5; E = 76.5; Kind = 'end'; Cap = @() }
    )

    $xf = 0.25
    $gains = @{}
    for ($i = 0; $i -lt $segs.Count; $i++) {
        $s = $segs[$i]
        $log = & cmd /c "`"$ffmpeg`" -hide_banner -ss $(F $s.S) -t $(F ($s.E - $s.S)) -i synced.mkv -vn -af ebur128 -f null - 2>&1"
        $li = ($log | Select-String '^\s+I:\s+(-?[0-9.]+) LUFS' | Select-Object -Last 1)
        $gain = 0.0
        if ($li) { $gain = [Math]::Max(-12.0, [Math]::Min(30.0, -20.0 - (P $li.Matches[0].Groups[1].Value))) }
        $gains[$i] = $gain
        Write-Host "segment $($i + 1): gain $(F $gain) dB"
    }

    foreach ($fmt in @('16x9', '9x16')) {
        $vertical = $fmt -eq '9x16'
        $W = if ($vertical) { 1080 } else { 1920 }
        $H = if ($vertical) { 1920 } else { 1080 }
        $g = New-Object System.Collections.Generic.List[string]
        $n = $segs.Count
        $g.Add("[0:v]split=$n" + (-join (0..($n - 1) | ForEach-Object { "[v$_]" })))
        $g.Add("[0:a]$($duck)asplit=$n" +(-join (0..($n - 1) | ForEach-Object { "[a$_]" })))
        $events = New-Object System.Collections.Generic.List[string]
        $pos = 0.0
        for ($i = 0; $i -lt $n; $i++) {
            $s = $segs[$i]
            $len = $s.E - $s.S
            $chain = "[v$i]trim=start=$(F $s.S):end=$(F $s.E),setpts=PTS-STARTPTS"
            if ($s.Zoom -and $rects.ContainsKey($s.Zoom.Rect)) {
                $maxW = if ($vertical -and $s.Kind -eq 'panel') { 1400 } else { 1920 }
                $chain += ',' + (ZoomFilter $rects[$s.Zoom.Rect] ($s.Zoom.In - $s.S) ($s.Zoom.Out - $s.S) $maxW)
            }
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
            $aS = if ($i -gt 0) { $s.S - $xf / 2 } else { $s.S }
            $aE = if ($i -lt $n - 1) { $s.E + $xf / 2 } else { $s.E }
            $edge = ''
            if ($i -eq 0) { $edge += ',afade=t=in:d=0.08' }
            if ($i -eq $n - 1) { $edge += ",afade=t=out:st=$(F ($aE - $aS - 1.5)):d=1.5" }
            $g.Add("[a$i]atrim=start=$(F $aS):end=$(F $aE),asetpts=PTS-STARTPTS,volume=$(F $gains[$i])dB$edge[y$i]")
            foreach ($c in $s.Cap) {
                $style = if ($s.Kind -eq 'world') { 'Top' } else { 'Bottom' }
                $mv = ''
                if ($vertical) { $style = 'Top'; $mv = if ($s.Kind -eq 'world') { '0,0,330' } else { '0,0,1360' } } else { $mv = '0,0,0' }
                $events.Add("Dialogue: 0,$(AssTime ($pos + $c.T)),$(AssTime ($pos + $c.T + $c.D)),$style,,$mv,,{\fad(200,250)}$($c.Text)")
            }
            $pos += $len
        }
        $g.Add((-join (0..($n - 1) | ForEach-Object { "[x$_]" })) + "concat=n=$($n):v=1:a=0[cv]")
        $prev = 'y0'
        for ($i = 1; $i -lt $n; $i++) {
            $out = if ($i -eq $n - 1) { 'ca' } else { "z$i" }
            $g.Add("[$prev][y$i]acrossfade=d=$(F $xf):c1=qsin:c2=qsin[$out]")
            $prev = $out
        }
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
