package com.quietplease.music;

import com.mojang.blaze3d.audio.Channel;
import com.quietplease.mixin.MusicManagerAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.Music;
import net.minecraft.util.Mth;
import net.minecraft.Util;

/**
 * Arka plan muzigini kontrol etme: atlama, duraklatma ve kapatilan parcalari durdurma
 * (panel, calma listesi paneli, kisayol tuslari ve komutlar).
 *
 * Sadece MusicManager'in caldigi muzik atlanir ve duraklatilir; plaklar ve modlarin dogrudan caldigi sesler
 * (cogu boss muzigi) atlanmaz. Yumusak gecis aciksa calan sarki FADE_MS boyunca kisilarak biterken siradaki
 * hemen baslar (capraz gecis).
 */
public final class MusicControl {

    public static final long FADE_MS = 1500;

    private static final java.util.Random RANDOM = new java.util.Random();

    @Nullable
    private static SoundInstance fading;
    private static long fadeStart;

    /** Duraklatilmis arka plan sarkisi; null = duraklatilmis sarki yok. */
    @Nullable
    private static SoundInstance paused;
    private static long pausedAt;

    private MusicControl() {}

    // ---------------------------------------------------------------- atlama

    /**
     * Calan arka plan sarkisini durdurur. Calma listesi aciksa siradaki parca hemen baslar; degilse sarki arasi
     * ayarina gore bekler (Yok: 1 sn, Kisa: 30-60 sn, Ozel: kaydiricilar, Vanilla: oyunun kendi arasi).
     *
     * Yumusak gecis aciksa capraz gecis yapilir: calan sarki MusicManager'dan ayrilir ve kendi kendine kisilarak
     * biter, siradaki sarki beklemeden onun altinda baslar. Kisilma surerken tekrar basilirsa kisilan sarki hemen
     * susar ve yeni baslayan sarki atlanir. Duraklatilmis sarki da atlanabilir.
     */
    public static void skip() {
        Minecraft mc = Minecraft.getInstance();
        if (fading != null) stopFading(mc);
        SoundInstance current = currentMusic(mc);
        boolean wasPaused = paused != null && paused == current;
        paused = null;
        if (ModOptions.fadeOnSkip && !wasPaused && current != null && mc.getSoundManager().isActive(current)) {
            ((MusicManagerAccessor) mc.getMusicManager()).quietplease$setCurrentMusic(null);
            fading = current;
            fadeStart = Util.getMillis();
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

    private static void stopFading(Minecraft mc) {
        SoundInstance sound = fading;
        fading = null;
        if (sound != null) mc.getSoundManager().stop(sound);
    }

    /** Bu ses, atlandigi icin kisilarak biten eski sarki mi? ("Su an calan" listesinde gosterilmez.) */
    public static boolean isFadingOut(SoundInstance sound) {
        return fading != null && sound == fading;
    }

    /** Su an kisilarak biten bir sarki var mi? */
    public static boolean isFading() {
        return fading != null;
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

    /** Atladiktan sonra siradaki sarkiya kadar bekleme (tick). */
    static int delayAfterSkip(@Nullable Music music) {
        if (music == null) return 100;
        Music adjusted = ModOptions.adjustGap(music);
        int min = adjusted.getMinDelay();
        int max = Math.max(min, adjusted.getMaxDelay());
        return Math.max(20, Mth.nextInt(RANDOM, min, max));
    }

    // ---------------------------------------------------------------- kapatilan parcalar

    /**
     * Kapatilan parcalar calmaya devam etmesin. Oyun, ses seviyesi 0 olan bir sesi durdurmaz, sadece sessize alir;
     * arka plan sarkisiysa MusicManager onu hala caliyor sanar ve sarki bitene kadar baska muzik baslatmaz.
     * Bu yuzden kapatilan ve calan her ses durdurulur; arka plan sarkisiysa Atla'daki gibi siradakine gecilir
     * (calma listesinde parca listeden cikmaz, kapali oldugu icin sirasi gelince atlanir). Onizleme etkilenmez.
     */
    public static void stopDisabled(Minecraft mc) {
        if (!MusicSettings.hasDisabled()) return;
        List<SoundInstance> off = new ArrayList<>();
        for (SoundInstance instance : channels(mc).keySet()) {
            if (instance instanceof MusicPreview.PreviewSound) continue;
            Sound sound;
            try {
                sound = instance.getSound();
            } catch (RuntimeException e) {
                continue;
            }
            if (sound != null && MusicSettings.isDisabled(sound.getLocation())) off.add(instance);
        }
        if (off.isEmpty()) return;
        SoundInstance current = currentMusic(mc);
        for (SoundInstance sound : off) {
            if (sound == current) {
                if (paused == sound) paused = null;
                if (current instanceof Playlist.PlaylistSound) Playlist.releaseCurrent();
                skipNow();
            } else {
                if (sound == fading) fading = null;
                if (sound == paused) paused = null;
                mc.getSoundManager().stop(sound);
            }
        }
    }

    // ---------------------------------------------------------------- duraklatma

    /**
     * Calan arka plan sarkisini duraklatir ya da duraklatilmis sarkiyi kaldigi yerden devam ettirir.
     * Duraklatilmisken oyun yeni bir sarki baslatmaz (sarki hala "calan" sayilir).
     *
     * @return ne oldugunu anlatan ceviri anahtari: "key.pause.paused", "key.pause.resumed" ya da "key.pause.nothing"
     */
    public static String togglePause() {
        Minecraft mc = Minecraft.getInstance();
        if (paused != null) {
            resume(mc);
            return "key.pause.resumed";
        }
        SoundInstance current = currentMusic(mc);
        ChannelAccess.ChannelHandle channel = current == null ? null : channels(mc).get(current);
        if (channel == null || channel.isStopped()) return "key.pause.nothing";
        if (fading != null) stopFading(mc);
        channel.execute(Channel::pause);
        paused = current;
        pausedAt = Util.getMillis();
        return "key.pause.paused";
    }

    private static void resume(Minecraft mc) {
        SoundInstance sound = paused;
        paused = null;
        if (sound == null) return;
        ChannelAccess.ChannelHandle channel = channels(mc).get(sound);
        if (channel != null) channel.execute(Channel::unpause);
        // Yumusak giris/cikis sureleri duraklatilan sure kadar kaysin; yoksa sarki devam edince erkenden kisilirdi
        MusicMixer.shiftStart(sound, Util.getMillis() - pausedAt);
        MusicSettings.applyToPlayingSounds();
    }

    /** Duraklatilmis bir arka plan sarkisi var mi? */
    public static boolean isPaused() {
        return paused != null;
    }

    public static boolean isPaused(SoundInstance sound) {
        return paused != null && sound == paused;
    }

    /**
     * Oyun ESC menusunden donunce butun sesleri devam ettirir (SoundEngine.resume); duraklatilan sarki
     * duraklatilmis kalsin (mixin cagirir).
     */
    public static void keepPaused() {
        if (paused == null) return;
        ChannelAccess.ChannelHandle channel = channels(Minecraft.getInstance()).get(paused);
        if (channel != null) channel.execute(Channel::pause);
    }

    // ---------------------------------------------------------------- tick

    /** Her istemci tick'inde: kisilmayi ilerletir, bitince durdurur; artik calmayan duraklatilmis sarkiyi unutur. */
    public static void clientTick(Minecraft mc) {
        if (paused != null && (paused != currentMusic(mc) || !mc.getSoundManager().isActive(paused))) {
            paused = null; // ornegin ejderha muzigi onu kesti
        }
        if (fading == null) return;
        if (Util.getMillis() - fadeStart >= FADE_MS || !mc.getSoundManager().isActive(fading)) {
            stopFading(mc);
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

    @Nullable
    private static SoundInstance currentMusic(Minecraft mc) {
        return ((MusicManagerAccessor) mc.getMusicManager()).quietplease$getCurrentMusic();
    }

    private static Map<SoundInstance, ChannelAccess.ChannelHandle> channels(Minecraft mc) {
        SoundEngine engine = ((com.quietplease.mixin.SoundManagerAccessor) mc.getSoundManager()).quietplease$getSoundEngine();
        return ((com.quietplease.mixin.SoundEngineAccessor) engine).quietplease$getInstanceToChannel();
    }
}
