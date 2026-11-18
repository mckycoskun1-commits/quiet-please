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
t_all = np.arange(total) / SR

TUNE = [0, 0, 7, 7, 9, 9, 7, None, 5, 5, 4, 4, 2, 2, 0, None,
        7, 7, 5, 5, 4, 4, 2, None, 7, 7, 5, 5, 4, 4, 2, None]
BASE = 523.25


def spectral(x, shape):
    spec = np.fft.rfft(x)
    freqs = np.fft.rfftfreq(len(x), 1 / SR)
    return np.fft.irfft(spec * shape(freqs), len(x))


def melody_events():
    events = []
    pos = 0.15
    step = 0.33
    while pos < args.seconds - 1:
        for note in TUNE:
            dur = step * rng.uniform(0.8, 1.25)
            if note is not None:
                n = note
                if rng.random() < 0.14:
                    n += rng.choice([-1, 1, 6])
                cents = rng.uniform(-75, 75)
                events.append((pos, dur * 0.92, BASE * 2 ** ((n + cents / 100) / 12), rng.random() < 0.12))
            pos += dur
            if pos >= args.seconds - 1:
                break
    return events


def recorder(events):
    out = np.zeros(total)
    for start, dur, freq, squeak in events:
        i0 = int(start * SR)
        n = min(int(dur * SR), total - i0)
        if n <= 0:
            continue
        t = np.arange(n) / SR
        wobble = 1 + 0.012 * np.sin(2 * np.pi * 5.5 * t) + 0.02 * np.sin(2 * np.pi * 0.7 * t)
        f = freq * wobble
        if squeak:
            k = int(min(0.12, dur * 0.4) * SR)
            f[:k] *= 3.0
        phase = np.cumsum(f) / SR
        wave = np.where((phase % 1.0) < 0.25, 1.0, -0.6)
        breath = rng.standard_normal(n) * 0.18
        env = np.minimum(1, t / 0.015) * np.minimum(1, (dur - t) / 0.03)
        out[i0:i0 + n] += (wave + breath) * env
    return spectral(out, lambda fr: np.clip((fr - 300) / 400, 0, 1) * np.exp(-((fr - 2800) / 5000) ** 2) * 1.6)


def kazoo(events):
    out = np.zeros(total)
    for start, dur, freq, _ in events:
        i0 = int((start + 0.06) * SR)
        n = min(int(dur * SR), total - i0)
        if n <= 0:
            continue
        t = np.arange(n) / SR
        f = freq / 2 * 2 ** (rng.uniform(-60, 60) / 1200) * (1 + 0.03 * np.sin(2 * np.pi * 7 * t))
        phase = np.cumsum(f) / SR
        saw = 2 * (phase % 1.0) - 1
        buzz = 1 + 0.6 * np.sign(np.sin(2 * np.pi * 95 * t))
        env = np.minimum(1, t / 0.02) * np.minimum(1, (dur - t) / 0.04)
        out[i0:i0 + n] += saw * buzz * env
    def formant(fr):
        return (np.exp(-((fr - 650) / 180) ** 2) + 0.8 * np.exp(-((fr - 1250) / 260) ** 2) + 0.5 * np.exp(-((fr - 2700) / 400) ** 2))
    return spectral(out, formant) * 1.4


def drums():
    out = np.zeros(total)
    beat = 60 / 101
    k = 0
    while True:
        at = 0.05 + k * beat + rng.normal(0, 0.025)
        if at > args.seconds - 0.5:
            break
        i0 = int(at * SR)
        if k % 2 == 0:
            n = int(0.35 * SR)
            t = np.arange(n) / SR
            f = 48 + 90 * np.exp(-t * 30)
            kick = np.tanh(4 * np.sin(2 * np.pi * np.cumsum(f) / SR)) * np.exp(-t * 9)
            out[i0:i0 + n] += kick[: total - i0] * 0.9
        else:
            n = int(0.22 * SR)
            t = np.arange(n) / SR
            bell = (np.sign(np.sin(2 * np.pi * 545 * t)) + np.sign(np.sin(2 * np.pi * 812 * t))) * np.exp(-t * 18)
            out[i0:i0 + n] += bell[: total - i0] * 0.25
        h0 = int((at + beat / 2 + 0.04) * SR)
        if h0 + 2000 < total:
            out[h0:h0 + 2000] += rng.standard_normal(2000) * np.exp(-np.arange(2000) / 300) * 0.3
        k += 1
    return out


events = melody_events()
mix = 0.55 * recorder(events) + 0.45 * kazoo(events) + 0.7 * drums()
delay = int(0.11 * SR)
mix[delay:] += 0.35 * mix[:-delay]
mix = np.tanh(2.2 * mix / (np.max(np.abs(mix)) + 1e-9))
hold = 4
mix = np.repeat(mix[::hold], hold)[:total]
mix = np.round(mix * 24) / 24
fade = int(0.02 * SR)
mix[:fade] *= np.linspace(0, 1, fade)
mix[-SR * 2:] *= np.linspace(1, 0, SR * 2)
mix *= 10 ** (-24 / 20) / (np.sqrt(np.mean(mix ** 2)) + 1e-9)
left = mix
right = np.roll(mix, int(0.012 * SR)) * 0.9
sf.write(args.out, np.stack([left, right], axis=1).astype(np.float32), SR)
print(f"{args.out}: {args.seconds:.0f} s, {len(events)} notes, peak {20 * np.log10(np.max(np.abs(mix)) + 1e-9):.1f} dBFS")
