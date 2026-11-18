# Promo video: read this first

This file is for whoever records the next promo cut (2.0.1). Read it fully, then `promo/recorder/README.md`, `promo/video-script-2.0.0.md`, `promo/recorder/PromoDirector.java`, `promo/recorder/record.ps1` and `promo/recorder/edit.ps1` before changing anything.

## State after the second cut (2026-11-17, 17:00)

- The second cut is in `promo/videos/raw-2026-11-17-1653/` (`promo-16x9.mp4`, `promo-9x16.mp4`, 720p previews). It follows every decision below.
- Shot: seed 1152036, camera at -2768.5 98 2934.5, yaw 90, pitch 6, time 12250. Cherry leaves on the left, the sun over the sea in the middle, a green slope with flowers on the right. No seed within reach has cherry trees on both sides of a sea view; placing a tree with `place feature` failed on that slope.
- Sync measured with four cues: -3, -8 and +3 ms with only the start cue, -3 and -9 ms after the linear stretch. Capture dropped 2 frames.
- The game music is captured very quietly (about -45 dB RMS), so the demo songs are made 25 dB quieter than before and the edit evens out each scene before the final loudness pass.
- The cursor and taskbar tooltips showed up in world shots when the game window had no focus; the director now hides the cursor while no screen is open.

## Sound fix after the second cut (2026-11-17, 22:40)

- The user found animal sounds too loud in some places. Animal calls were at about 1 s, 44.5 s, 53 s and 74 s; the per-scene loudness step raised them by 20 to 30 dB. They cannot be filtered out of the mix: the game music is stereo and covers the same frequencies.
- Fix: the director now mutes the world sound categories (animals, monsters, ambient, blocks, weather, players, voice) in every run. The sound was recorded again with `record.ps1 -AudioOnly` and put under the old picture with `edit.ps1 -Video`.
- Result in `promo/videos/audio-2026-11-17-2235/`: the picture is identical to the second cut, all 18 click sounds land within 20 ms of the old ones, sync offsets 8, 3 and 13 ms with the start cue, 7 and -4 ms after the stretch, -17.8 LUFS.

## Music cutting out (2026-11-18)

- The user heard the music stop suddenly while nothing happened on screen. The mod was not at fault; every stop in `audio.wav` matches a timeline action. Three causes, all in the recorder or the edit:
  - Scene 1: the playlist starts a song by itself right after `preparePlaylist`, and the timeline cuts it at 1.5 s with `Playlist.playNow(0)` (Play now stops the current song at once, by design). The old cut started at 0.6 s, so the video opened with that song and then went silent. Scene 1 now starts at 1.8 s, just before rubedo comes in.
  - Scene 4 ended at 30.0 s, before the stop click on the demo song at 30.2 s, so the song vanished without a click. It now ends at 30.45 s.
  - Every cut faded the sound out and in over about 0.2 s, a short hole in the music. The scenes are now joined with a 0.25 s equal-power crossfade centred on the cut, which keeps the sync.
- The user found the button clicks too loud next to the music. A fixed cut did not help: the per-scene gain and loudnorm push every click into the limiter, so all clicks ended up at the same level. `edit.ps1` now finds every click in the synced sound (above -30 dB after a 3 kHz high-pass, music stays below -55 dB there) and turns the sound down for about 50 ms so the click ends 4 dB above the music in the 0.4 s before it (cuts of 7 to 29 dB, listed in `sync.txt`). Result: clicks 4 to 7 dB above the music instead of 8 to 19 dB.
- Skip, previous and "never play again" in scenes 6 and 7 are pressed as keys, so nothing moves on screen; only the action bar message and the captions explain them. That is how the features work, not a fault.

## What the user said about the first cut

The reference cut from 2026-11-17 is in `promo/videos/raw-2026-11-17-0149/` (`promo-16x9.mp4`, `promo-9x16.mp4`).

Keep:

- The story, the 9 scenes and the captions. The user said the video explains the mod very well. The yellow NEW tags were removed on 2026-11-18 at the user's request; all captions are plain white.
- The closing end card. The user liked it a lot. Do not redesign it.
- Length about 65 to 70 seconds, both formats (16:9 and 9:16), loudness -16 LUFS.

Fix:

1. The sound is slightly out of sync in some places.
2. The camera turning all the time in the background felt odd. It must stand still.

## Decisions made with the user

- Camera: completely still. No turning, no moving forward, in the world scenes and behind the panels. One fixed shot for the whole video. The closing shot may use a second fixed framing at the same place if it looks better, joined by a cut, never by a pan.
- Place: a cherry grove on the coast. Pink cherry trees in the foreground (left and right thirds), the sea and the horizon behind them, the sun setting over the water.
- Time: sunset, frozen (no daylight or weather cycle, clear sky).
- Shaders: Solas Shader V3.2 with the user's own settings, through Iris and Sodium.
- Zoom in on the interface only at two moments, nowhere else:
  - Scene 3: the volume slider being dragged down to about 30%.
  - Scene 8: History & stats, when the Most played list opens.
- Scene 4: the "Your music" bug is fixed in 2.0.1 (mc/26.1.2 is on 2.0.1), so now click play on one demo song ("Campfire Lofi") and let it be heard for a few seconds. Check in `audio.wav` that the song is really audible there.

## Shaders: tested and working (2026-11-17)

- `promo/recorder/mods/` holds `iris-fabric-1.11.4+mc26.1.2.jar` and `sodium-fabric-0.9.2+mc26.1.2.jar`, downloaded from Modrinth with the user's approval (sha512 checked). The folder is gitignored; never commit the jars.
- Solas is the user's own file: `%APPDATA%\.minecraft\shaderpacks\Solas Shader V3.2.zip` plus its settings `Solas Shader V3.2.zip.txt`. Only copy them; never change, move or commit the user's files.
- Smoke test: with both jars in `fabric/run/mods`, the pack and its `.txt` in `fabric/run/shaderpacks`, and `fabric/run/config/iris.properties` set to `enableShaders=true`, `shaderPack=Solas Shader V3.2.zip`, `disableUpdateMessage=true`, `:fabric:runClient` loads Iris and Sodium (26.1 is not obfuscated, so the "refmap could not be read" warnings and "Unrecognized clouds setting" are harmless). Iris logged "Using shaderpack: Solas Shader V3.2.zip", "Profile: ULTRA (+22 options changed by user)", created the overworld pipeline, and the world rendered with Solas.
- PC: NVIDIA RTX 4070 SUPER, Ryzen 5 7500F, 1920x1080 screen. On mains power the display and sleep timeouts are off and there is no screen saver, so the screen stays on while nobody is there.
- Side effects that `record.ps1` must clean up after every run (add this to the script, like the songs and stats):
  - `fabric/run/mods/` jars, `fabric/run/shaderpacks/`, and in `fabric/run/config/`: `iris.properties`, `iris-excluded.json`, `sodium-options.json`, `sodium-mixins.properties`, `sodium-fingerprint.json`.
  - Sodium sets `exclusiveFullscreen:true` in `options.txt` on its first start and Iris adds key bindings there. `options.txt` is already restored from the backup, but exclusive fullscreen must also be off during the recording (the recorder uses a borderless always-on-top window for Desktop Duplication). Force `mc.options.exclusiveFullscreen()` off in the director setup, or write `exclusiveFullscreen:false` before launch.
- The noon test view was quite hazy. If the sea or the sun is hard to see at sunset, adjust a copy of the settings file in `fabric/run/shaderpacks`, never the user's original.

## Finding the shot

- The director makes a fresh creative world with seed 1234 and looks for the nearest cherry grove (`findClosestBiome3d`). That spot is inland. Find a cherry grove whose west side opens to the sea within roughly 30 to 150 blocks: sample biomes west of each candidate for `BiomeTags.IS_OCEAN` or `IS_BEACH`, try further candidates or other seeds if needed.
- The sun sets in the west. Yaw 90 looks west (yaw 0 is south, 180 north). Start with a pitch of about 3 to 8 degrees down so the horizon sits in the upper third.
- Sunset in ticks: the sky turns orange from about 12000, the sun touches the horizon around 12500 to 12800. Try a few values (for example 11800, 12200, 12500) and pick the best one.
- Put the camera low, 2 to 4 blocks above the ground at the edge of the grove, so trees frame the picture, not 7 blocks above the tree tops like before.
- Scout before recording: a short dev-only scouting pass that teleports to each candidate, waits until chunks are built and the shader is warm, and saves a screenshot per candidate and time. Look at the images, choose one, then hardcode seed, position, yaw, pitch and time in `PromoDirector`. Keep the screenshots in the raw folder, not in git.
- Wait long enough before the cue so all chunks are built with the shader (phase 3 waits 15 s today; raise it if the picture still pops in).

## Audio sync

What the first cut shows:

- `raw.mp4` is variable frame rate: 4834 frames in 81.5 s (59.16 fps on average), so frames were dropped under load. The timestamps follow the wall clock (gaps, not compression).
- There is only one sync point, the cue at the start. Everything after it relies on the screen capture clock and the OpenAL wave writer clock staying together for about 80 seconds.
- Shaders will make the load heavier, so this gets worse unless it is handled.

Do all of these:

1. Add a second cue at the end: master volume to 0 for about 1.5 s, then the white frame and the pling again, then stop ffmpeg. `edit.ps1` finds both cues in both files and compares the two intervals. If they differ by more than 15 ms, stretch the audio linearly to match the video before cutting.
2. Convert the synced video to constant 60 fps before trimming, so cut points are frame exact.
3. Make the capture lighter: today ffmpeg runs `ddagrab` then `hwdownload,format=bgra` then NVENC, a copy to the CPU and back for every frame. NVENC can take the Desktop Duplication frames directly. Check `ffmpeg.log` for dropped or duplicated frames.
4. Check sync at three or more points in the middle of the video, for example a button click sound against the frame where the button reacts, and the moment a song stops against the click on its switch. Write the offsets in milliseconds in your report. Aim for at most 40 ms everywhere.

## Zoom (edit only)

- Do the zoom in `edit.ps1`, not in the game, before the captions are burned in, so captions never get bigger.
- About 1.6x to 1.8x, centered on the slider row (scene 3) or the Most played list (scene 8). Ease in over about 0.5 s, hold, ease out over about 0.5 s. Smooth, no jumps, no other zooms.
- Let the director log the screen rectangle of the zoom target into `marks.txt` (GUI coordinates times the GUI scale), so the edit script does not guess.
- For 9:16 the panel is already enlarged; apply the same zoom to the source before the vertical crop and check that nothing important leaves the frame.

## How to work

- The user is away while this runs and cannot answer questions. Make sensible choices, note them, and keep going.
- Reply to the user in Turkish. No emoji, no comment lines in code or texts. Commit messages in English.
- Recording takes over the screen for about 2 minutes; nobody will touch the PC.
- At most three full recordings. If the shaders make the recording unusable and you cannot fix it, record the static sunset shot without shaders and say so clearly.
- After every Minecraft run: make sure the game is closed, then check that no java process and no Gradle daemon is left (`gradlew --stop`).
- Check the result yourself before reporting: look at frames from every scene, measure loudness, check the sync numbers.
- Make 720p previews (about 15 MB) and send both to the user with SendUserFile, together with a short Turkish summary of what changed.
- Commit and push the recorder and edit script changes to `mc/26.1.2` without asking. Never commit videos, jars or the shader pack. Do not upload the video anywhere.
- Update the promo memory note (promo-video-progress) with the result.

## 2.1 promo with voice-over (2026-11-18)

- Story and voice lines: `promo/voice/lines-2.1.md` (player Chris, narrator Liam, ElevenLabs Eleven v4). Effects: `promo/sfx/README.md`.
- Recording order in `PromoDirector` is not the order of the video: Sweden starts 42 s before the first cue so its melody is full, then sceneA (sunset), sceneE (same shot for the ending), sceneF (end card background), then sceneB (Kazoo Disaster starts over the same view), cue2, sceneD1 to sceneD5 (fast tour: switch off Kazoo Disaster, volume slider, own music and playlist, cue3, skip, previous, never again, history and stats), cue4.
- Default recording: only the one wide camera and the view, no character in the scene; the player exists only as a voice, punch zoom and shake on sceneB are done in the edit. `record.ps1 -Mannequin` (folder `raw-mannequin-*`) records the same timeline with a `minecraft:mannequin` 24 blocks west of the wide camera, a close over-the-shoulder camera (fov 45) for sceneA/E/F/B, the mannequin turning and crouching in sceneB, and the wide camera from cue2 on. The user asked for both to compare.
- Render distance is 32. Before Sweden starts, phase 8 waits until every chunk the server tracks for the camera is on the client and the renderer reports all sections built, steady for 4 s (at most 5 minutes). The log shows `PROMO chunks loaded/total`.
- `record.ps1` makes `Kazoo Disaster.ogg` with `bad-song.py` (Python venv from `promo/voice`).
- `mix.py` builds the sound from a timeline file (music, voice and effect rows with target loudness, automatic ducking under the voice, -16 LUFS master with a limiter). `animatic-2.1.tsv` is the audio draft made from the game files before recording; levels: voice -16 LUFS (soft player line -18, angry line -14.5), music -21 to -22 without voice and about 9 dB lower under it, Kazoo Disaster -17 and only 4 dB lower under the angry line.
- `edit-2.1.py <raw folder> [--formats 16x9,9x16] [--preview]` (run with `promo/voice/.venv/Scripts/python.exe -I`) cuts the 2.1 video, about 47 s: A sunset (slow push in, player line), B Kazoo Disaster (punch zoom with shake, angry line), C freeze frame of B (record scratch, grey until "Relax.", icon pops on "Quiet Please"), D1/D2 panel in one piece (switch off, volume slider zoom), D3 play own song, D3b playlist at 1.5x, D4 three jump cuts on skip, previous and never, D5 history with zoom, E sunset again (slow pull out, relieved line), F end card. Voice phrases are placed on the clicks they describe (n5 is split into "Skip it", "go back", "or never hear it again").
- The 2.1 sound does not use the game recording: music comes from the game files (Sweden, Left to Bloom, Featherfall), `bad-song.wav` of the take and Campfire Lofi rebuilt from `song-Campfire-Lofi.txt`. The volume slider is shown by an 8 dB lower Left to Bloom after the drag. `mix.py` row option `measure=from:length` measures loudness on that part of the file (0:0 for the whole file), so split voice lines and the lowered song keep the same reference.
- Captions are the spoken lines: player lines yellow, narrator white; boxed on panel scenes, at the top on the wide skip/previous/never shots in 16:9 (the game's own message sits at the bottom), above the Shorts interface in 9:16. Outputs in the take folder: `promo-2.1-16x9.mp4`, `promo-2.1-9x16.mp4`, previews `preview-2.1-*.mp4`, `mix-2.1.tsv`, `mix-2.1-report.txt`.
- 2026-11-18: the user compared both cuts and chose the plain one (no mannequin): `promo/videos/raw-2026-11-18-1753/promo-2.1-*.mp4`. The mannequin option stays in the recorder but is not the plan.
- Voice takes are the free-plan ElevenLabs takes in `promo/videos/voice-2026-11-18/eleven` (`--voice` to point elsewhere). The user does not move to a paid plan: credit ElevenLabs in the descriptions and keep the videos unmonetized (see `promo/youtube-text.md`).
