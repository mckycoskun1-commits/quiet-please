import argparse
import pathlib

import numpy as np
import soundfile as sf
from kokoro_onnx import Kokoro

here = pathlib.Path(__file__).resolve().parent
ap = argparse.ArgumentParser()
ap.add_argument("--voices", required=True)
ap.add_argument("--out", required=True)
ap.add_argument("--speed", type=float, default=1.1)
ap.add_argument("--script", default=str(here / "script-en.txt"))
ap.add_argument("--gap", type=float, default=0.5)
args = ap.parse_args()

kokoro = Kokoro(str(here / "models" / "kokoro-v1.0.onnx"), str(here / "models" / "voices-v1.0.bin"))
lines = []
for raw in pathlib.Path(args.script).read_text(encoding="utf-8").splitlines():
    parts = raw.split("\t")
    if len(parts) == 3:
        lines.append((parts[0], float(parts[1]), parts[2]))

out = pathlib.Path(args.out)
out.mkdir(parents=True, exist_ok=True)
for voice in args.voices.split(","):
    vdir = out / voice
    vdir.mkdir(exist_ok=True)
    reel = []
    timing = []
    for key, start, text in lines:
        samples, rate = kokoro.create(text, voice=voice, speed=args.speed, lang="en-us")
        samples = np.asarray(samples, dtype=np.float32)
        sf.write(vdir / f"{key}.wav", samples, rate)
        timing.append(f"{key}\t{start}\t{len(samples) / rate:.2f}")
        reel.append(samples)
        reel.append(np.zeros(int(args.gap * rate), dtype=np.float32))
    sf.write(out / f"reel-{voice}.wav", np.concatenate(reel), rate)
    (vdir / "timing.txt").write_text("\n".join(timing) + "\n", encoding="utf-8")
    print(voice, " ".join(t.split("\t")[0] + "=" + t.split("\t")[2] for t in timing))
