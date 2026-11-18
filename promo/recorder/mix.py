import argparse
import pathlib
import re
import subprocess
import sys

import numpy as np

SR = 48000

ap = argparse.ArgumentParser()
ap.add_argument("timeline")
ap.add_argument("out")
ap.add_argument("--var", action="append", default=[])
ap.add_argument("--ffmpeg", default="ffmpeg")
ap.add_argument("--target", type=float, default=-16.0)
ap.add_argument("--duck", type=float, default=9.0)
ap.add_argument("--length", type=float, default=0.0)
args = ap.parse_args()

names = dict(v.split("=", 1) for v in args.var)


def expand(path):
    return re.sub(r"\{(\w+)\}", lambda m: names[m.group(1)], path)


def decode(path, start=0.0, length=0.0):
    cmd = [args.ffmpeg, "-v", "error", "-ss", f"{start:.4f}", "-i", path]
    if length > 0:
        cmd += ["-t", f"{length:.4f}"]
    cmd += ["-f", "f32le", "-ac", "2", "-ar", str(SR), "-"]
    raw = subprocess.run(cmd, capture_output=True, check=True).stdout
    return np.frombuffer(raw, dtype=np.float32).reshape(-1, 2).astype(np.float64)


def loudness(x):
    if len(x) < SR * 0.4:
        x = np.concatenate([x, np.zeros((int(SR * 0.4) - len(x) + 1, 2))])
    proc = subprocess.run([args.ffmpeg, "-hide_banner", "-f", "f64le", "-ac", "2", "-ar", str(SR), "-i", "-",
                           "-af", "ebur128", "-f", "null", "-"], input=x.astype(np.float64).tobytes(), capture_output=True)
    found = re.findall(r"^\s+I:\s+(-?[0-9.]+) LUFS", proc.stderr.decode(errors="replace"), re.M)
    return float(found[-1]) if found else -70.0


def fades(x, fade_in, fade_out):
    n = len(x)
    if fade_in > 0:
        k = min(n, int(fade_in * SR))
        x[:k] *= np.sin(np.linspace(0, np.pi / 2, k))[:, None]
    if fade_out > 0:
        k = min(n, int(fade_out * SR))
        x[n - k:] *= np.cos(np.linspace(0, np.pi / 2, k))[:, None]
    return x


rows = []
for line in pathlib.Path(args.timeline).read_text(encoding="utf-8").splitlines():
    cells = line.split("\t")
    if len(cells) < 6 or cells[0] not in ("music", "voice", "sfx"):
        continue
    opts = dict(o.split("=", 1) for o in cells[6].split(",") if "=" in o) if len(cells) > 6 else {}
    rows.append((cells[0], expand(cells[1]), float(cells[2]), float(cells[3]), float(cells[4]), float(cells[5]), opts))

end = args.length or max(r[2] + (r[4] if r[4] > 0 else 3) for r in rows) + 0.5
total = int(end * SR)
layers = {k: np.zeros((total, 2)) for k in ("music", "voice", "sfx")}
report = []
hop = SR // 100
shape = None


def duck_shape():
    frames = total // hop
    env = np.sqrt(np.mean(layers["voice"][: frames * hop].reshape(frames, hop, 2) ** 2, axis=(1, 2)) + 1e-12)
    active = 20 * np.log10(env) > -45
    out = np.zeros(frames)
    level = 0.0
    for i in range(frames):
        want = 1.0 if active[max(0, i - 25):i + 6].any() else 0.0
        level += (want - level) * (0.35 if want > level else 0.04)
        out[i] = level
    out = np.repeat(out, hop)
    return np.concatenate([out, np.full(total - len(out), out[-1] if len(out) else 0.0)])


for kind, path, at, start, length, level, opts in sorted(rows, key=lambda r: r[0] == "music"):
    if kind == "music" and shape is None:
        shape = duck_shape()
    align = float(opts.get("align", 0))
    clip = decode(path, start, length)
    if kind == "sfx":
        gain = level
    else:
        gain = level - loudness(clip)
    clip = fades(clip * 10 ** (gain / 20), float(opts.get("in", 0.005 if kind != "music" else 0.02)), float(opts.get("out", 0.01 if kind != "music" else 0.05)))
    i0 = int(round((at - align) * SR))
    if i0 < 0:
        clip = clip[-i0:]
        i0 = 0
    n = min(len(clip), total - i0)
    if kind == "music":
        depth = float(opts.get("duck", args.duck))
        clip = clip[:n] * (10 ** (-depth * shape[i0:i0 + n] / 20))[:, None]
    layers[kind][i0:i0 + n] += clip[:n]
    report.append(f"{kind:5s} {pathlib.Path(path).name[:28]:28s} at {at:6.2f} len {len(clip) / SR:5.2f} gain {gain:+6.1f} dB")

music = layers["music"]
mix = music + layers["voice"] + layers["sfx"]
before = loudness(mix)
mix *= 10 ** ((args.target - before) / 20)
out = pathlib.Path(args.out)
proc = subprocess.run([args.ffmpeg, "-v", "error", "-y", "-f", "f64le", "-ac", "2", "-ar", str(SR), "-i", "-",
                       "-af", "alimiter=limit=0.84:attack=3:release=60:level=0", "-c:a", "pcm_s16le", str(out)],
                      input=mix.astype(np.float64).tobytes(), capture_output=True)
if proc.returncode != 0:
    sys.exit(proc.stderr.decode(errors="replace"))
for kind in ("voice", "music"):
    layer = music if kind == "music" else layers["voice"]
    report.append(f"{kind} layer after the master gain: {loudness(layer * 10 ** ((args.target - before) / 20)):.1f} LUFS")
report.append(f"mix {before:.1f} LUFS before the master gain, target {args.target:.1f} LUFS, length {end:.2f} s")
print("\n".join(report))
