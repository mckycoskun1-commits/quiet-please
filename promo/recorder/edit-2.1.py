import argparse
import json
import os
import pathlib
import re
import subprocess
import sys

ap = argparse.ArgumentParser()
ap.add_argument("raw")
ap.add_argument("--voice", default="")
ap.add_argument("--out", default="2.1")
ap.add_argument("--bad", default="bad-song.wav")
ap.add_argument("--formats", default="16x9,9x16")
ap.add_argument("--preview", action="store_true")
ap.add_argument("--target", default="-19")
args = ap.parse_args()

here = pathlib.Path(__file__).resolve().parent
repo = here.parents[1]
raw = pathlib.Path(args.raw).resolve()
mannequin = "mannequin" in raw.name
ffmpeg = str(next((pathlib.Path(os.environ["LOCALAPPDATA"]) / "Microsoft" / "WinGet" / "Packages").rglob("ffmpeg.exe")))
voice = pathlib.Path(args.voice) if args.voice else repo / "promo" / "videos" / "voice-2026-11-18" / "eleven"
sfx = repo / "promo" / "sfx" / "files"
icon = repo / "art" / "quietplease-icon.png"
loom = pathlib.Path.home() / ".gradle" / "caches" / "fabric-loom" / "assets"
index = json.loads(sorted((loom / "indexes").glob("26.1.2*.json"))[-1].read_text())["objects"]


def asset(name):
    h = index["minecraft/sounds/music/game/" + name + ".ogg"]["hash"]
    return str(loom / "objects" / h[:2] / h)


def run(cmd, **kw):
    p = subprocess.run(cmd, cwd=raw, capture_output=True, text=True, **kw)
    if p.returncode != 0:
        sys.exit(p.stderr[-3000:])
    return p


def f(x):
    return f"{x:.4f}".rstrip("0").rstrip(".")


log = run([ffmpeg, "-hide_banner", "-i", "raw.mp4", "-vf",
           "scale=64:36,signalstats,metadata=print:key=lavfi.signalstats.YAVG", "-f", "null", "-"]).stderr
flashes = []
t = 0.0
prev = False
for line in log.splitlines():
    m = re.search(r"pts_time:([0-9.]+)", line)
    if m:
        t = float(m.group(1))
        continue
    m = re.search(r"YAVG=([0-9.]+)", line)
    if m:
        white = float(m.group(1)) > 225
        if white and not prev:
            flashes.append(t)
        prev = white
v1 = flashes[0]

marks = {}
rects = {}
for line in (raw / "marks.txt").read_text().splitlines():
    p = line.split()
    marks[p[0]] = float(p[1]) / 1000
    if len(p) >= 6:
        rects[p[0]] = [float(v) for v in p[2:6]]


def m(name, off=0.0):
    return marks[name] + off


segs = [
    dict(id="A", kind="world", src=(m("sceneA", 0.2), m("sceneA", 5.2)), view="in"),
    dict(id="B", kind="world", src=(m("sceneB", -0.07), m("sceneB", 4.13)), view="punch"),
    dict(id="C", kind="freeze", src=(m("sceneF", 0.3), m("sceneF", 5.7)), dur=5.4),
    dict(id="D12", kind="panel", src=(m("sceneD1", -0.15), m("sceneD3", -0.3)), zoom=("zoom3", m("zoom3", 0.04))),
    dict(id="D3", kind="panel", src=(m("play", -1.5), m("play", 2.1))),
    dict(id="D3b", kind="panel", src=(m("sceneD3b", 0.3), m("sceneD3b", 4.5)), speed=1.5),
    dict(id="D4a", kind="wide", src=(m("skip", -0.3), m("skip", 0.9))),
    dict(id="D4b", kind="wide", src=(m("previous", -0.2), m("previous", 0.9))),
    dict(id="D4c", kind="wide", src=(m("never", -0.2), m("never", 1.6))),
    dict(id="D5", kind="panel", src=(m("sceneD5", 0.2), m("sceneD5", 4.2)), zoom=("zoom8", m("zoom8", -0.05))),
    dict(id="E", kind="world", src=(m("sceneE", 0.3), m("sceneE", 4.1)), view="out"),
    dict(id="F", kind="end", src=(m("sceneF", 0.3), m("sceneF", 7.3))),
]
pos = 0.0
by = {}
for s in segs:
    s["speed"] = s.get("speed", 1.0)
    s["len"] = s["dur"] if s["kind"] == "freeze" else (s["src"][1] - s["src"][0]) / s["speed"]
    s["out"] = pos
    pos += s["len"]
    by[s["id"]] = s
total = pos


def at(seg, src):
    s = by[seg]
    return s["out"] + (src - s["src"][0]) / s["speed"]


def off(seg, x):
    return by[seg]["out"] + x


if mannequin:
    punch = (1290.0, 600.0)
    calm = (1100.0, 540.0)
    world_x = 1100.0
else:
    punch = (960.0, 430.0)
    calm = (960.0, 430.0)
    world_x = 960.0

off_out = at("D12", m("off"))
drag_out = at("D12", m("zoom3", 0.54))
play_out = at("D3", m("play"))
stop_out = at("D3", m("play", 1.95))
skip_out = at("D4a", m("skip"))
prev_out = at("D4b", m("previous"))
never_out = at("D4c", m("never"))
zoom8_out = at("D5", m("zoom8", -0.05))
F = by["F"]["out"]

lines = [
    ("p1-chris-1", 0, 0, off("A", 0.6), -17, [(0.0, 2.53, "Okay, this view is really nice.")], "Player"),
    ("p2-chris-1", 0, 0, off("B", 0.9), -16, [(0.0, 2.5, "Wait. Who put this song in my modpack?")], "Player"),
    ("n1-liam-1", 0, 0, off("C", 0.4), -16, [(0.0, 1.9, "Ever had a song ruin the moment?"), (2.2, 4.82, "Relax. Quiet Please fixes that.")], "Narr"),
    ("n2-liam-1", 0, 0, at("D12", m("sceneD1")), -16, [(0.0, 2.51, "Open the panel, find it, switch it off."), (2.93, 3.55, "Gone!")], "Narr"),
    ("n3-liam-1", 0, 0, at("D12", m("zoom3", -0.40)), -16, [(0.0, 2.86, "Too loud? Turn down just that one song.")], "Narr"),
    ("n4-liam-1", 0, 1.38, at("D3", m("play", -0.3)), -16, [(0.0, 1.2, "Play your own music.")], "Narr"),
    ("n4-liam-1", 1.38, 0, off("D3b", 0.3) - 0.18, -16, [(0.18, 1.44, "Build your own playlist.")], "Narr"),
    ("n5-liam-1", 0, 0.71, skip_out - 0.06, -16, [(0.06, 0.59, "Skip it,")], "Narr"),
    ("n5-liam-1", 0.71, 0.79, prev_out - 0.12, -16, [(0.12, 0.6, "go back,")], "Narr"),
    ("n5-liam-1", 1.5, 0, never_out - 0.12, -16, [(0.12, 1.48, "or never hear it again.")], "Narr"),
    ("n6-liam-1", 0, 0, off("D5", 0.3), -16, [(0.0, 2.02, "And see your history and stats.")], "Narr"),
    ("p3-chris-1", 0, 0, off("E", 0.5), -16.5, [(0.0, 1.89, "Now THAT is more like it.")], "Player"),
    ("n7-liam-1", 0, 0, F + 0.35, -16, [], "Narr"),
]

bloom_measure = "measure=125:20"
bloom1_at = off_out + 0.35
rows = [
    ("music", "{sweden}", 0.0, 45.0, by["A"]["len"], -21, "in=0.3,out=0.03"),
    ("music", "{bad}", by["B"]["out"], 0.0, by["B"]["len"], -17, "in=0.005,out=0.02,duck=4"),
    ("music", "{bad}", by["D12"]["out"], by["B"]["len"], off_out - by["D12"]["out"], -21, "in=0.05,out=0.01"),
    ("music", "{bloom}", bloom1_at, 125.0, drag_out - bloom1_at + 0.45, -22, "in=1.0,out=0.9," + bloom_measure),
    ("music", "{bloom}", drag_out, 125.0 + drag_out - bloom1_at, play_out - drag_out, -30, "in=0.9,out=0.15," + bloom_measure),
    ("music", "{campfire}", play_out, 0.0, stop_out - play_out, -21, "in=0.03,out=0.12"),
    ("music", "{bloom}", by["D3b"]["out"], 135.0, skip_out - by["D3b"]["out"] + 0.2, -22, "in=0.3,out=0.4," + bloom_measure),
    ("music", "{feather}", skip_out, 0.0, by["E"]["out"] - skip_out + 0.5, -22, "in=0.25,out=0.6"),
    ("music", "{sweden}", by["E"]["out"], 51.0, total - by["E"]["out"], -21, "in=0.4,out=1.5"),
]
for name, a, ln, t0, level, caps, style in lines:
    rows.append(("voice", "{voice}/" + name + ".mp3", t0, a, ln, level, "measure=0:0"))
effects = [
    ("vinyl-scratch", by["C"]["out"], -7, "out=0.12", 0.7),
    ("whoosh-cinematic", by["D12"]["out"], -8, "align=1.07"),
    ("record-swipe", off_out, -6, ""),
    ("sweep-small", drag_out, -12, "align=0.33"),
    ("swoosh-fast", by["D3"]["out"], -8, "align=0.11"),
    ("pop-bubble", play_out, -14, ""),
    ("swoosh-fast", by["D3b"]["out"], -8, "align=0.11"),
    ("pop-whoosh", at("D3b", m("sceneD3b", 3.25)), -14, "align=0.02"),
    ("whoosh-cinematic", by["D4a"]["out"], -10, "align=1.07"),
    ("swoosh-fast", skip_out, -10, "align=0.11"),
    ("vinyl-back", prev_out, -8, "align=0.30"),
    ("record-swipe", never_out, -8, ""),
    ("swoosh-fast", by["D5"]["out"], -8, "align=0.11"),
    ("sweep-small", zoom8_out, -12, "align=0.33"),
    ("whoosh-cinematic", by["E"]["out"], -10, "align=1.07"),
    ("riser", F, -6, "align=2.53"),
    ("boom", F, -4, ""),
    ("pop-bubble", off("C", 0.4 + 3.33), -12, ""),
    ("pop-whoosh", F + 0.65, -14, "align=0.02"),
    ("pop-whoosh", F + 0.9, -14, "align=0.02"),
    ("pop-whoosh", F + 1.15, -14, "align=0.02"),
]
for e in effects:
    rows.append(("sfx", "{sfx}/" + e[0] + ".wav", e[1], 0.0, e[4] if len(e) > 4 else 0.0, e[2], e[3]))

tsv = "\n".join("\t".join([r[0], r[1], f(r[2]), f(r[3]), f(r[4]), f(r[5]), r[6]]) for r in rows) + "\n"
(raw / "mix-2.1.tsv").write_text(tsv, encoding="utf-8")
run([ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-/filter_complex", "song-Campfire-Lofi.txt", "-map", "[out]", "campfire.wav"])
mix = run([sys.executable, "-I", str(here / "mix.py"), "mix-2.1.tsv", "mix-2.1.wav", "--ffmpeg", ffmpeg, "--target", args.target, "--length", f(total),
           "--var", "sweden=" + asset("sweden"), "--var", "bloom=" + asset("left_to_bloom"), "--var", "feather=" + asset("featherfall"),
           "--var", "bad=" + str(raw / args.bad), "--var", "campfire=" + str(raw / "campfire.wav"),
           "--var", "voice=" + str(voice), "--var", "sfx=" + str(sfx)]).stdout
(raw / "mix-2.1-report.txt").write_text(mix, encoding="utf-8")
print(mix)

T = "(in/60)"


def persp(e, cx, cy, dx="0", dy="0"):
    x0 = f"clip({f(cx)}+{dx}-W/(2*{e}),0,W-W/{e})"
    y0 = f"clip({f(cy)}+{dy}-H/(2*{e}),0,H-H/{e})"
    x1 = f"({x0}+W/{e})"
    y1 = f"({y0}+H/{e})"
    return (f"perspective=x0='{x0}':y0='{y0}':x1='{x1}':y1='{y0}':x2='{x0}':y2='{y1}':x3='{x1}':y3='{y1}'"
            ":sense=source:interpolation=cubic:eval=frame")


def rect_zoom(rect, tin, max_w):
    x, y, w, h = rect
    z = min(1.7, 0.94 * max_w / w, 0.85 * 1080 / h)
    e = f"(1+{f(z - 1)}*st(0,clip(({T}-{f(tin)})/0.5,0,1))*ld(0)*(3-2*ld(0)))"
    return persp(e, x + w / 2, y + h / 2)


def ass_time(x):
    cs = int(round(x * 100))
    return f"{cs // 360000}:{cs // 6000 % 60:02d}:{cs // 100 % 60:02d}.{cs % 100:02d}"


relax = 0.4 + 2.2
for fmt in args.formats.split(","):
    vertical = fmt == "9x16"
    W, H = (1080, 1920) if vertical else (1920, 1080)
    g = []
    n = len(segs)
    g.append(f"[0:v]split={n}" + "".join(f"[v{i}]" for i in range(n)))
    g.append("[1:v]split=2[icon0][icon1]")
    for i, s in enumerate(segs):
        k = s["kind"]
        if k == "freeze":
            a = v1 + s["src"][0]
            chain = (f"[v{i}]trim=start={f(a)}:end={f(a + s['dur'])},setpts=PTS-STARTPTS,fps=60,trim=duration={f(s['dur'])},"
                     + persp(f"(1.08+0.03*{T}/{f(s['dur'])})", *punch)
                     + f",hue=s='if(lt(t,{f(relax)}),0.45,min(1,0.45+(t-{f(relax)})*1.2))'")
        else:
            a, b = v1 + s["src"][0], v1 + s["src"][1]
            chain = f"[v{i}]trim=start={f(a)}:end={f(b)},setpts=(PTS-STARTPTS)/{f(s['speed'])},fps=60,trim=duration={f(s['len'])}"
            view = s.get("view")
            if view == "in":
                chain += "," + persp(f"(1+0.04*{T}/{f(s['len'])})", *calm)
            elif view == "out":
                chain += "," + persp(f"(1.05-0.05*{T}/{f(s['len'])})", *calm)
            elif view == "punch":
                chain += "," + persp(f"(1.02+0.06*{T}/{f(s['len'])})", *punch)
            if "zoom" in s and s["zoom"][0] in rects:
                max_w = 1400 if vertical else 1920
                chain += "," + rect_zoom(rects[s["zoom"][0]], (s["zoom"][1] - s["src"][0]) / s["speed"], max_w)
        if vertical and k == "panel":
            g.append(f"{chain},split[p{i}][q{i}]")
            g.append(f"[q{i}]scale=1080:1920,boxblur=40:4,eq=brightness=-0.1:saturation=1.2[pb{i}]")
            g.append(f"[p{i}]crop=1400:1080:260:0,scale=1080:834[pf{i}]")
            g.append(f"[pb{i}][pf{i}]overlay=0:440[w{i}]")
        elif vertical:
            cx = 960.0 if k == "wide" else world_x
            g.append(f"{chain},crop=608:1080:{int(cx - 304)}:0,scale=1080:1920[w{i}]")
        else:
            g.append(f"{chain}[w{i}]")
        if k == "freeze":
            size = 300 if vertical else 240
            y = 380 if vertical else 300
            pop = 0.4 + 3.33
            g.append(f"[icon0]scale={size}:{size}:flags=neighbor,format=rgba,fade=t=in:st={f(pop)}:d=0.12:alpha=1[ic0]")
            g.append(f"[w{i}][ic0]overlay=(W-w)/2:{y}:shortest=1,setsar=1[x{i}]")
        elif k == "end":
            size = 340 if vertical else 280
            y = 560 if vertical else 170
            g.append(f"[w{i}]split[e{i}][f{i}]")
            g.append(f"[f{i}]boxblur=24:3,eq=brightness=-0.22,format=yuva420p,fade=t=in:st=0:d=0.35:alpha=1[fb{i}]")
            g.append(f"[e{i}][fb{i}]overlay[eb{i}]")
            g.append(f"[icon1]scale={size}:{size}:flags=neighbor,format=rgba,fade=t=in:st=0.05:d=0.2:alpha=1[ic1]")
            g.append(f"[eb{i}][ic1]overlay=(W-w)/2:{y}:shortest=1,fade=t=out:st={f(s['len'] - 0.8)}:d=0.8,setsar=1[x{i}]")
        else:
            g.append(f"[w{i}]setsar=1[x{i}]")
    g.append("".join(f"[x{i}]" for i in range(n)) + f"concat=n={n}:v=1:a=0[cv]")
    g.append(f"[cv]subtitles=filename=captions-2.1-{fmt}.ass:fontsdir='C\\:/Windows/Fonts'[outv]")
    (raw / f"graph-2.1-{fmt}.txt").write_text(";\n".join(g), encoding="ascii")

    events = []
    for name, a, ln, t0, level, caps, style in lines:
        for c0, c1, text in caps:
            start = t0 + c0
            seg = next(s for s in segs if s["out"] <= start < s["out"] + s["len"])
            suffix = "Box" if seg["kind"] == "panel" else ("Top" if seg["kind"] == "wide" and not vertical else "")
            events.append([start, t0 + c1 + 0.35, style + suffix, text])
    events.sort(key=lambda e: e[0])
    for j in range(len(events) - 1):
        events[j][1] = min(events[j][1], events[j + 1][0] - 0.05)
    out = [f"Dialogue: 0,{ass_time(e[0])},{ass_time(e[1])},{e[2]},,0,0,0,,{{\\fad(120,150)}}{e[3]}" for e in events]
    cx = W // 2
    ty = 1010 if vertical else 560
    step = 70 if vertical else 62
    end_t = ass_time(total)
    out.append(f"Dialogue: 0,{ass_time(F + 0.4)},{end_t},EndTitle,,0,0,0,,{{\\an5\\pos({cx},{ty})\\fad(250,600)}}Quiet Please 2.0")
    for k2, text in enumerate(["Fabric, Forge, NeoForge, Quilt", "Minecraft 1.18.2 to 26.3", "Free on CurseForge"]):
        out.append(f"Dialogue: 0,{ass_time(F + 0.65 + 0.25 * k2)},{end_t},EndLine,,0,0,0,,"
                   f"{{\\an5\\pos({cx},{ty + 90 + k2 * step})\\fad(200,600)}}{text}")
    size = 66 if vertical else 58
    margin = 560 if vertical else 70
    box_margin = 560 if vertical else 30
    header = f"""[Script Info]
ScriptType: v4.00+
PlayResX: {W}
PlayResY: {H}
WrapStyle: 0
ScaledBorderAndShadow: yes

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Narr,Segoe UI,{size},&H00FFFFFF,&H00FFFFFF,&H00101010,&H80000000,-1,0,0,0,100,100,0,0,1,4,2,2,70,70,{margin},1
Style: Player,Segoe UI,{size},&H0060E0FF,&H0060E0FF,&H00101010,&H80000000,-1,0,0,0,100,100,0,0,1,4,2,2,70,70,{margin},1
Style: NarrTop,Segoe UI,{size},&H00FFFFFF,&H00FFFFFF,&H00101010,&H80000000,-1,0,0,0,100,100,0,0,1,4,2,8,70,70,60,1
Style: PlayerTop,Segoe UI,{size},&H0060E0FF,&H0060E0FF,&H00101010,&H80000000,-1,0,0,0,100,100,0,0,1,4,2,8,70,70,60,1
Style: NarrBox,Segoe UI,{size},&H00FFFFFF,&H00FFFFFF,&H50000000,&H50000000,-1,0,0,0,100,100,0,0,3,14,0,2,70,70,{box_margin},1
Style: PlayerBox,Segoe UI,{size},&H0060E0FF,&H0060E0FF,&H50000000,&H50000000,-1,0,0,0,100,100,0,0,3,14,0,2,70,70,{box_margin},1
Style: EndTitle,Segoe UI,{104 if vertical else 92},&H00FFFFFF,&H00FFFFFF,&H00101010,&H80000000,-1,0,0,0,100,100,0,0,1,4,3,5,40,40,0,1
Style: EndLine,Segoe UI,{54 if vertical else 46},&H00E0E0E0,&H00FFFFFF,&H00101010,&H80000000,-1,0,0,0,100,100,0,0,1,3,2,5,40,40,0,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
"""
    (raw / f"captions-2.1-{fmt}.ass").write_text(header + "\n".join(out) + "\n", encoding="utf-8")

    name = f"promo-{args.out}-{fmt}.mp4"
    run([ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-i", "raw.mp4", "-loop", "1", "-framerate", "60", "-i", str(icon),
         "-i", "mix-2.1.wav", "-/filter_complex", f"graph-2.1-{fmt}.txt", "-map", "[outv]", "-map", "2:a",
         "-c:v", "h264_nvenc", "-preset", "p6", "-cq", "19", "-pix_fmt", "yuv420p", "-r", "60",
         "-c:a", "aac", "-b:a", "192k", "-shortest", "-movflags", "+faststart", name])
    print("done", raw / name)
    if args.preview:
        scale = "720:1280" if vertical else "1280:720"
        run([ffmpeg, "-hide_banner", "-loglevel", "error", "-y", "-i", name, "-vf", f"scale={scale}", "-c:v", "h264_nvenc",
             "-preset", "p6", "-cq", "27", "-r", "30", "-c:a", "aac", "-b:a", "128k", "-movflags", "+faststart", f"preview-{args.out}-{fmt}.mp4"])
        print("preview", raw / f"preview-{args.out}-{fmt}.mp4")

print("length", f(total), "s; cue1 at", f(v1), "s in raw.mp4")
