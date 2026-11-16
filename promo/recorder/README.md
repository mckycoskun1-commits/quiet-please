# Promo recorder

Records the promo video scenes automatically in the Fabric dev client (mc/26.1.2).

- `PromoDirector.java`: dev-only scene script. It creates a fresh creative world, flies to a cherry grove, then plays the scenes on a timer: camera pans, the cursor glides to buttons, text is typed into search boxes. It starts ffmpeg (Desktop Duplication, 1080p 60 fps, NVENC) at the start and closes the game at the end.
- `PromoMinecraftMixin.java`: calls the director once per frame from `Minecraft.runTick`.
- `record.ps1`: copies both files into the mod sources, adds the mixin, runs `:fabric:runClient`, then removes everything again and restores `fabric/run/options.txt` and `fabric/run/config/quietplease.json`. These files are never part of the mod.

Run from the repository root:

```
powershell -ExecutionPolicy Bypass -File promo\recorder\record.ps1
```

Output goes to `promo/videos/raw-<date>/`:

- `raw.mp4`: screen recording, no sound
- `audio.wav`: game sound, written by OpenAL Soft's wave backend (`ALSOFT_CONF`), so no other PC sound is recorded and nothing is heard while recording
- `marks.txt`: start of each scene in milliseconds after the sync cue

Sync: master volume stays at 0 until the cue. At the cue a white frame is shown and a note block plays at the same moment. Align the first white frame in `raw.mp4` with the first sound in `audio.wav`.

While recording the game window is borderless, always on top and covers the whole 1920x1080 screen. Do not touch the mouse or keyboard until the game closes itself (about 1.5 minutes).

Before the 2.0 recording: update `buildTimeline` in `PromoDirector.java` for the 2.0 screens and features (button labels are matched by their English text, for example "Playlist", "Add songs", "+ Add", "Queue", "Even out volume").
