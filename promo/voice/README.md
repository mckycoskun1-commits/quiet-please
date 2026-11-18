# Voice-over

The final voice comes from ElevenLabs: the lines, settings and file names are in `lines-2.1.md`, the takes go to `takes/` (not committed).

## ElevenLabs takes

`eleven.py` reads the key from `ELEVENLABS_API_KEY`, either from the environment or from the user's Windows environment entry (so `setx` works without a restart). It takes the texts straight from the table in `lines-2.1.md`.

```
promo\voice\.venv\Scripts\python.exe -I promo\voice\eleven.py info
promo\voice\.venv\Scripts\python.exe -I promo\voice\eleven.py voices
promo\voice\.venv\Scripts\python.exe -I promo\voice\eleven.py say --player <name> --narrator <name> --takes 3
```

`say` writes `takes/<line>-<voice>-<take>.mp3`, skips files that already exist (`--force` remakes them), and accepts `--lines p1,n2`, `--speed`, `--stability` (0 creative, 0.5 natural, 1 robust) and several comma separated voices per role for comparisons.

### Run

Kokoro makes a quick local temp track from `script-en.txt` (id, start time in seconds, text) so timing can be tried before the real takes exist. It sounds flat; never use it in a published cut.

## Kokoro setup (once)

```
winget install --id Python.Python.3.12 -e --scope user
%LOCALAPPDATA%\Programs\Python\Python312\python.exe -m venv promo\voice\.venv
promo\voice\.venv\Scripts\python.exe -m pip install kokoro-onnx soundfile
```

Download `kokoro-v1.0.onnx` and `voices-v1.0.bin` from the `model-files-v1.0` release of github.com/thewh1teagle/kokoro-onnx into `promo/voice/models/` (not committed).

### Run

```
promo\voice\.venv\Scripts\python.exe -I promo\voice\speak.py --voices am_puck --out promo\videos\voice-<date>
```

Writes one wav per line into `<out>/<voice>/`, a `timing.txt` with the length of every line, and `reel-<voice>.wav` with all lines in a row.
