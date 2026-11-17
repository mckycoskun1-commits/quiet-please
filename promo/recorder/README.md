# Promo recorder

Records the promo video scenes automatically in the Fabric dev client (mc/26.1.2).

- `PromoDirector.java`: dev-only scene script. It creates a fresh creative world (seed 1152036), puts the camera on a fixed sunset shot over the sea in a cherry grove, then plays the scenes on a timer: the cursor glides to buttons, text is typed into search boxes. The camera never moves. It starts ffmpeg (Desktop Duplication straight into NVENC, 1080p 60 fps) at the start and closes the game at the end.
- `PromoMinecraftMixin.java`: calls the director once per frame from `Minecraft.runTick`.
- `record.ps1`: copies both files into the mod sources, adds the mixin, makes three short demo songs with ffmpeg in `fabric/run/config/quietplease/music` (simple synthesized chords, free to use, about as loud as the game music), adds the shaders, runs `:fabric:runClient`, then removes everything again and restores `fabric/run/options.txt`, `fabric/run/config/quietplease.json` and `fabric/run/config/quietplease-stats.json`. These files are never part of the mod.

Shaders: Iris and Sodium for 26.1.2 go in `promo/recorder/mods/` (gitignored, from Modrinth), Solas Shader V3.2 and its settings are copied from `%APPDATA%\.minecraft\shaderpacks`. Without them the script records without shaders; `-NoShaders` forces that.

Scenes (2.0.1, see `promo/video-script-2.0.0.md`): landscape, turn a song off, per-song volume, your own music group playing a demo song, playlist, skip with crossfade, previous song and never play again, history and stats, closing landscape.

Run from the repository root:

```
powershell -ExecutionPolicy Bypass -File promo\recorder\record.ps1
```

Output goes to `promo/videos/raw-<date>/`:

- `raw.mp4`: screen recording, no sound
- `audio.wav`: game sound, written by OpenAL Soft's wave backend (`ALSOFT_CONF`), so no other PC sound is recorded and nothing is heard while recording
- `marks.txt`: start of each scene and each sync cue in milliseconds after the first cue, and the screen rectangle of the two zoom targets

Edit (newest raw folder, or pass `-Raw <folder>`):

```
powershell -ExecutionPolicy Bypass -File promo\recorder\edit.ps1
```

It finds the sync cues in both files, corrects the sound to the picture, cuts the scenes, zooms in on the volume slider (scene 3) and the Most played list (scene 8), evens out the loudness of each scene, joins the sound of two scenes with a 0.25 s crossfade so the music never drops out at a cut, turns each button click down so it sits only 4 dB above the music, adds the captions and the end card and writes `promo-16x9.mp4` and `promo-9x16.mp4` into the same folder. Scene times, zoom times and captions are in the `$segs` table; they follow `buildTimeline`. The measured offsets are written to `sync.txt`.

Sync: master volume stays at 0 until the first cue. There are four cues (start, 7 s, 42.5 s, end): the sound is muted for a moment, then a white frame is shown and a click or note block plays at the same moment. The cues fall between the scenes and are cut out. The edit script stretches the sound linearly between the first and the last cue and checks the two middle cues; if one is off by more than 20 ms it stretches piece by piece between all cues.

Only music, interface and note block sounds are recorded: the director turns the world sound categories (animals, monsters, ambient, blocks, weather, players, voice) down to 0, because the edit raises quiet scenes by up to 30 dB and animal calls became too loud.

New sound for an existing recording: the timeline and the songs are fixed, so a second run plays the same sounds at the same moments. `record.ps1 -AudioOnly` runs without shaders and without screen capture and writes only `audio.wav` and `marks.txt` into `promo/videos/audio-<date>/`. Then put that sound under the old picture:

```
powershell -ExecutionPolicy Bypass -File promo\recorder\edit.ps1 -Raw promo\videos\audio-<date> -Video promo\videos\raw-<date>
```

The window still covers the screen during the run; do not touch the mouse or keyboard.

Finding a new shot:

- `-SeedScan -NoShaders` checks 48 seeds for a cherry grove with the open sea to its west and writes `seeds.txt` into `promo/videos/scout-<date>/`.
- `-Scout -Seed <seed>` looks for such spots in one world, places a camera at each and saves screenshots at three sunset times.
- `-Scout -Seed <seed> -Shots "x y z yaw pitch;..." -Times "12200,12350"` takes screenshots of fixed camera positions, to fine tune a shot.

Then put the chosen values into the `SEED` and `SHOT_*` constants in `PromoDirector`.

While recording the game window is borderless, always on top and covers the whole 1920x1080 screen. Do not touch the mouse or keyboard until the game closes itself (about 3 minutes).

Button labels are matched by their English text ("Settings", "Playlist", "Add songs", "+ Add", "Queue", "History & stats", "Most played", "Never played"), so a renamed label needs a change in `buildTimeline`.
