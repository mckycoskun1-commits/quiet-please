import argparse

import numpy as np
import soundfile as sf

ap = argparse.ArgumentParser()
ap.add_argument("out")
ap.add_argument("--seconds", type=float, default=60.0)
ap.add_argument("--seed", type=int, default=7)
args = ap.parse_args()

SR = 48000
rng = np.random.default_rng(args.seed)
total = int(args.seconds * SR)

TUNE = [0, 0, 7, 7, 9, 9, 7, None, 5, 5, 4, 4, 2, 2, 0, None,
        7, 7, 5, 5, 4, 4, 2, None, 7, 7, 5, 5, 4, 4, 2, None]
SOUR = {3: 1, 5: -1, 10: 1, 13: -1}
LATE = {4: 0.12, 11: 0.09}
BASE = 392.0
STEP = 0.36
CHORDS = [0, 0, 5, 0, 7, 7, 0, 0]


def spectral(x, shape):
    spec = np.fft.rfft(x)
    freqs = np.fft.rfftfreq(len(x), 1 / SR)
    return np.fft.irfft(spec * shape(freqs), len(x))


def melody_events():
    events = []
    pos = 0.2
    loop = 0
    while pos < args.seconds - 1:
        for i, note in enumerate(TUNE):
            dur = STEP * rng.uniform(0.88, 1.15)
            if rng.random() < 0.08:
                dur *= 1.5
            if note is not None:
                n = note
                if loop == 0 and i in SOUR:
                    n += SOUR[i]
                elif rng.random() < 0.1:
                    n += rng.choice([-1, 1])
                cents = rng.uniform(-30, 30) - 15 * np.sin(pos / 9)
                late = LATE.get(i, 0.0) if loop == 0 else max(0.0, rng.normal(0, 0.03))
                events.append((pos + late, dur, BASE * 2 ** ((n + cents / 100) / 12)))
            pos += dur
            if pos >= args.seconds - 1:
                break
        loop += 1
    return events


def toy_piano(events):
    out = np.zeros(total)
    partials = [(1.0, 1.0, 3.2), (2.01, 0.32, 5.0), (3.04, 0.12, 7.5), (4.12, 0.05, 10.0)]
    for start, dur, freq in events:
        i0 = int(start * SR)
        n = min(int(min(dur * 1.6, 1.3) * SR), total - i0)
        if n <= 0:
            continue
        t = np.arange(n) / SR
        note = sum(a * np.sin(2 * np.pi * freq * k * t) * np.exp(-t * d) for k, a, d in partials)
        env = np.minimum(1, t / 0.006) * np.minimum(1, (n / SR - t) / 0.05)
        out[i0:i0 + n] += note * env
    return out


def melodica(events):
    out = np.zeros(total)
    for start, dur, freq in events:
        i0 = int((start + rng.uniform(0.04, 0.09)) * SR)
        n = min(int(dur * 0.9 * SR), total - i0)
        if n <= 0:
            continue
        t = np.arange(n) / SR
        f = freq * 2 ** (rng.uniform(25, 45) / 1200) * (1 + 0.006 * np.sin(2 * np.pi * 4.5 * t))
        phase = np.cumsum(f) / SR
        tri = 2 * np.abs(2 * (phase % 1.0) - 1) - 1
        env = np.minimum(1, t / 0.06) * np.minimum(1, (n / SR - t) / 0.08)
        out[i0:i0 + n] += tri * env
    return spectral(out, lambda fr: 1 / (1 + (fr / 1800) ** 4))


def bass():
    out = np.zeros(total)
    beat = STEP * 2
    k = 0
    while True:
        at = 0.2 + k * beat + rng.normal(0, 0.035)
        if at > args.seconds - 0.8:
            break
        chord = CHORDS[(k // 2) % len(CHORDS)]
        if k in (1, 2) or rng.random() < 0.12:
            chord += 1
        root = 98.0 * 2 ** ((chord + (7 if k % 2 else 0)) / 12)
        i0 = int(at * SR)
        n = min(int(0.55 * SR), total - i0)
        t = np.arange(n) / SR
        tone = (np.sin(2 * np.pi * root * t) + 0.25 * np.sin(4 * np.pi * root * t)) * np.exp(-t * 4.5)
        out[i0:i0 + n] += tone * np.minimum(1, t / 0.01)
        k += 1
    return out


def drums():
    out = np.zeros(total)
    beat = STEP * 2
    k = 0
    while True:
        at = 0.2 + k * beat + rng.normal(0, 0.04)
        if at > args.seconds - 0.5:
            break
        if rng.random() < 0.06:
            k += 1
            continue
        i0 = int(at * SR)
        if k % 2 == 0:
            n = min(int(0.3 * SR), total - i0)
            t = np.arange(n) / SR
            f = 55 + 70 * np.exp(-t * 25)
            out[i0:i0 + n] += np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 11)
        else:
            n = min(int(0.12 * SR), total - i0)
            t = np.arange(n) / SR
            block = np.sin(2 * np.pi * 820 * t) * np.exp(-t * 45) + 0.4 * np.sin(2 * np.pi * 1310 * t) * np.exp(-t * 60)
            out[i0:i0 + n] += block * 0.35
            if rng.random() < 0.15:
                j0 = i0 + int(0.09 * SR)
                m = min(n, total - j0)
                out[j0:j0 + m] += block[:m] * 0.25
        k += 1
    return out


events = melody_events()
mix = 0.5 * toy_piano(events) + 0.32 * melodica(events) + 0.42 * bass() + 0.5 * drums()
mix = spectral(mix, lambda fr: 1 / (1 + (fr / 5000) ** 4))
mix = np.tanh(1.2 * mix / (np.max(np.abs(mix)) + 1e-9)) / np.tanh(1.2)
fade = int(0.02 * SR)
mix[:fade] *= np.linspace(0, 1, fade)
mix[-SR * 2:] *= np.linspace(1, 0, SR * 2)
mix *= 10 ** (-24 / 20) / (np.sqrt(np.mean(mix ** 2)) + 1e-9)
left = mix
right = np.roll(mix, int(0.008 * SR)) * 0.92
sf.write(args.out, np.stack([left, right], axis=1).astype(np.float32), SR)
print(f"{args.out}: {args.seconds:.0f} s, {len(events)} notes, peak {20 * np.log10(np.max(np.abs(mix)) + 1e-9):.1f} dBFS")
