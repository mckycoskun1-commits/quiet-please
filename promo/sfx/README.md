# Sound effects

The files live in `promo/sfx/files/` and are not committed: the Mixkit license allows them in videos but forbids sharing them on their own.

License checked on 2026-11-18 (Mixkit Sound Effects Free License): free for commercial and non-commercial use, YouTube and ads included, no credit needed; no redistribution of the files themselves, no claiming them on a rights management service.

| File | Mixkit id | Title | Loudest point |
|------|-----------|-------|---------------|
| vinyl-scratch.wav | 702 | Record player vinyl scratch | 0.01 to 1.0 s, even |
| vinyl-back.wav | 705 | Vinyl forward and back scratch | 0.30 s |
| record-swipe.wav | 708 | DJ record swipe | 0.01 s, 0.2 s long |
| vinyl-stop.wav | 704 | Vinyl stop fast scratch | only a 0.1 s click, not useful |
| whoosh-cinematic.wav | 1492 | Cinematic whoosh fast transition | 1.07 s |
| swoosh-fast.wav | 3115 | Fast transitions swoosh | 0.11 s |
| sweep-small.wav | 166 | Fast small sweep transition | 0.33 s |
| pop-whoosh.wav | 3005 | Explainer video pops whoosh light pop | 0.02 s |
| pop-bubble.wav | 2357 | Bubble pop up alert notification | 0.04 s |
| zoom-impact.wav | 772 | Quick zoom impact | 0.51 s |
| riser.wav | 790 | Cinematic trailer riser | ends at 2.53 s |
| logo-impact.wav | 2900 | Movie logo intro impact | slow swell, peak at 4.4 s |

Download: `https://assets.mixkit.co/active_storage/sfx/<id>/<id>.wav`.

`boom.wav` is our own, made with ffmpeg:

```
ffmpeg -f lavfi -i "aevalsrc=exprs='0.85*sin(2*PI*(40*t+(80/12)*(1-exp(-12*t))))*exp(-3*t)+0.3*(random(0)*2-1)*exp(-35*t)':s=48000:d=1.8" -af "lowpass=f=2500,aformat=channel_layouts=stereo" promo/sfx/files/boom.wav
```
