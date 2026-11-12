package com.quietplease.music;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Music;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Arka plan muzigini atlama (panel, calma listesi paneli ve /quietplease skip).
 *
 * Sadece MusicManager'in caldigi muzik atlanir; plaklar ve modlarin dogrudan caldigi sesler (cogu boss muzigi) atlanmaz.
 */
public final class MusicControl {

    private static final RandomSource RANDOM = RandomSource.create();

    private MusicControl() {}

    /**
     * Calan arka plan sarkisini durdurur. Calma listesi aciksa siradaki parca hemen baslar; degilse sarki arasi
     * ayarina gore bekler (Yok: 1 sn, Kisa: 30-60 sn, Ozel: kaydiricilar, Vanilla: oyunun kendi arasi).
     */
    public static void skip() {
        if (Playlist.isActive()) {
            Playlist.restartMusic(0);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Music music = com.quietplease.platform.Services.PLATFORM.selectMusic(mc.getSituationalMusic());
        Playlist.restartMusic(delayAfterSkip(music));
    }

    /**
     * Atlar ve ne oldugunu anlatan ceviri anahtarini dondurur (komut ve kisayol tusu bunu gosterir):
     * "cmd.skip.next" (listeden siradaki basladi), "cmd.skip.gap" (sarki arasindan sonra) ya da
     * "cmd.skip.nothing" (calan arka plan muzigi yoktu).
     */
    public static String skipAndDescribe() {
        boolean playing = ((com.quietplease.mixin.MusicManagerAccessor) Minecraft.getInstance().getMusicManager())
                .quietplease$getCurrentMusic() != null;
        skip();
        if (!playing) return "cmd.skip.nothing";
        return Playlist.isActive() ? "cmd.skip.next" : "cmd.skip.gap";
    }

    /** Atladiktan sonra siradaki sarkiya kadar bekleme (tick). */
    static int delayAfterSkip(@Nullable Music music) {
        if (music == null) return 100;
        Music adjusted = ModOptions.adjustGap(music);
        int min = adjusted.minDelay();
        int max = Math.max(min, adjusted.maxDelay());
        return Math.max(20, Mth.nextInt(RANDOM, min, max));
    }
}
