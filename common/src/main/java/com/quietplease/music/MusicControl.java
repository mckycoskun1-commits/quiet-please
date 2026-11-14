package com.quietplease.music;

import com.quietplease.mixin.MusicManagerAccessor;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.Music;
import net.minecraft.util.Mth;
import net.minecraft.Util;

/**
 * Arka plan muzigini atlama (panel, calma listesi paneli, kisayol tusu ve /quietplease skip).
 *
 * Sadece MusicManager'in caldigi muzik atlanir; plaklar ve modlarin dogrudan caldigi sesler (cogu boss muzigi) atlanmaz.
 * Yumusak gecis aciksa calan sarki FADE_MS boyunca kisilarak biter, sonra atlanir.
 */
public final class MusicControl {

    public static final long FADE_MS = 1500;

    private static final java.util.Random RANDOM = new java.util.Random();

    @Nullable
    private static SoundInstance fading;
    private static long fadeStart;

    private MusicControl() {}

    /**
     * Calan arka plan sarkisini durdurur. Calma listesi aciksa siradaki parca hemen baslar; degilse sarki arasi
     * ayarina gore bekler (Yok: 1 sn, Kisa: 30-60 sn, Ozel: kaydiricilar, Vanilla: oyunun kendi arasi).
     * Kisilma surerken tekrar basilirsa hemen atlar.
     */
    public static void skip() {
        if (fading != null) {
            finishFade();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        SoundInstance current = currentMusic(mc);
        if (ModOptions.fadeOnSkip && current != null && mc.getSoundManager().isActive(current)) {
            fading = current;
            fadeStart = Util.getMillis();
            return;
        }
        skipNow();
    }

    private static void skipNow() {
        if (Playlist.isActive()) {
            Playlist.restartMusic(0);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Music music = com.quietplease.platform.Services.PLATFORM.selectMusic(mc.getSituationalMusic());
        Playlist.restartMusic(delayAfterSkip(music));
    }

    private static void finishFade() {
        fading = null;
        skipNow();
    }

    /** Su an kisilarak biten bir sarki var mi? */
    public static boolean isFading() {
        return fading != null;
    }

    /** Her istemci tick'inde: kisilmayi ilerletir, bitince atlar. */
    public static void clientTick(Minecraft mc) {
        if (fading == null) return;
        if (Util.getMillis() - fadeStart >= FADE_MS || !mc.getSoundManager().isActive(fading)) {
            finishFade();
            return;
        }
        MusicSettings.applyToPlayingSounds();
    }

    /** Ses seviyesi carpani: kisilan sarki icin 1'den 0'a iner, digerleri icin 1. */
    public static float fadeFactor(SoundInstance sound) {
        if (fading == null || sound != fading) return 1.0F;
        float t = (Util.getMillis() - fadeStart) / (float) FADE_MS;
        float v = Mth.clamp(1.0F - t, 0.0F, 1.0F);
        return v * v;
    }

    /**
     * Atlar ve ne oldugunu anlatan ceviri anahtarini dondurur (komut ve kisayol tusu bunu gosterir):
     * "cmd.skip.next" (listeden siradaki basladi), "cmd.skip.gap" (sarki arasindan sonra) ya da
     * "cmd.skip.nothing" (calan arka plan muzigi yoktu).
     */
    public static String skipAndDescribe() {
        boolean playing = currentMusic(Minecraft.getInstance()) != null;
        skip();
        if (!playing) return "cmd.skip.nothing";
        return Playlist.isActive() ? "cmd.skip.next" : "cmd.skip.gap";
    }

    @Nullable
    private static SoundInstance currentMusic(Minecraft mc) {
        return ((MusicManagerAccessor) mc.getMusicManager()).quietplease$getCurrentMusic();
    }

    /** Atladiktan sonra siradaki sarkiya kadar bekleme (tick). */
    static int delayAfterSkip(@Nullable Music music) {
        if (music == null) return 100;
        Music adjusted = ModOptions.adjustGap(music);
        int min = adjusted.getMinDelay();
        int max = Math.max(min, adjusted.getMaxDelay());
        return Math.max(20, Mth.nextInt(RANDOM, min, max));
    }
}
