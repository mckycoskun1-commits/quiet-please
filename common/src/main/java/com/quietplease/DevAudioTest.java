package com.quietplease;

import com.quietplease.music.CustomMusic;
import com.quietplease.music.MusicPreview;
import com.quietplease.music.MusicTrack;
import com.quietplease.music.Playlist;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

/**
 * Sadece gelistirme sirasinda (QUIETPLEASE_AUTODUMP=audio) calisan ses testi. tools/audio-test.ps1 oyunun sesini
 * OpenAL Soft'un wave cikisiyla dosyaya yazar ve bu testin isaretledigi araliklarda sesin gercekten duyulup
 * duyulmadigini olcer; ses kanalinin durumuna bakmak yetmiyor (kanal "caliyor" gorunup ses vermeyebiliyor).
 *
 * Kendi muzik klasorunde qp_audiotest_a (440 Hz, onizleme) ve qp_audiotest_b (1000 Hz, arka plan) olmali (.ogg ya da .mp3).
 * Zaman cizelgesi (tick): 1 her sey susar, 40 isaret sesi, 100 onizleme A, 200 onizleme durur, 260 arka plan B,
 * 360 onizleme A (B susar), 440 onizleme durur (B hemen geri gelmeli), 540 bitis, 560 oyun kapanir.
 */
final class DevAudioTest {

    private static final ResourceLocation A = new ResourceLocation(CustomMusic.NAMESPACE, "qp_audiotest_a");
    private static final ResourceLocation B = new ResourceLocation(CustomMusic.NAMESPACE, "qp_audiotest_b");

    private static int tick = -1;

    private DevAudioTest() {}

    static void start() {
        tick = 0;
    }

    static void tick(Minecraft mc) {
        if (tick < 0) return;
        tick++;
        MusicManager mm = mc.getMusicManager();
        if (tick < 260) {
            // Arka plan muzigi kendiliginden baslamasin; sessiz araliklar gercekten sessiz olmali
            if (current(mc) != null) mm.stopPlaying();
            ((com.quietplease.mixin.MusicManagerAccessor) mm).quietplease$setNextSongDelay(1000);
        }
        switch (tick) {
            case 1 -> {
                CustomMusic.refresh();
                mm.stopPlaying();
                mc.getSoundManager().stop();
                QuietPlease.LOGGER.info("AUDIOTEST dosyalar: A={} B={}", CustomMusic.exists(A), CustomMusic.exists(B));
                mark("silence");
            }
            case 40 -> {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                mark("cue");
            }
            case 100 -> {
                mark("previewA");
                QuietPlease.LOGGER.info("AUDIOTEST onizleme A basladi: {}", MusicPreview.play(track(A)));
            }
            case 150, 400 -> QuietPlease.LOGGER.info("AUDIOTEST onizleme caliyor: {}", MusicPreview.isActive());
            case 200, 440 -> {
                mark(tick == 200 ? "previewStop1" : "previewStop2");
                MusicPreview.stop();
            }
            case 260 -> {
                mark("backgroundB");
                Playlist.playOnce(B, false);
                Playlist.restartMusic(0);
            }
            case 300, 500 -> {
                SoundInstance music = current(mc);
                QuietPlease.LOGGER.info("AUDIOTEST arka plan: {} (aktif {})",
                        music instanceof Playlist.PlaylistSound p ? p.trackId : music, music != null && mc.getSoundManager().isActive(music));
            }
            case 360 -> {
                mark("previewA2");
                QuietPlease.LOGGER.info("AUDIOTEST onizleme A (arka plan calarken) basladi: {}", MusicPreview.play(track(A)));
            }
            case 540 -> {
                mark("end");
                mm.stopPlaying();
                mc.getSoundManager().stop();
            }
            case 560 -> {
                tick = -1;
                mc.stop();
            }
            default -> {}
        }
    }

    private static SoundInstance current(Minecraft mc) {
        return ((com.quietplease.mixin.MusicManagerAccessor) mc.getMusicManager()).quietplease$getCurrentMusic();
    }

    private static MusicTrack track(ResourceLocation id) {
        return new MusicTrack(id, java.util.Set.of(CustomMusic.EVENT), MusicTrack.Kind.MUSIC);
    }

    private static void mark(String name) {
        QuietPlease.LOGGER.info("AUDIOTEST mark {} {}", name, System.currentTimeMillis());
    }
}
