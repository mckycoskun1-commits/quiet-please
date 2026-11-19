package com.quietplease.music;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

/**
 * "Alt+Tab'da sessiz" ayari: oyun penceresi odakta degilken butun sesler duyulmayacak kadar kisilir.
 *
 * Tam 0 yapilmaz: 0 olunca oyun calan sesleri durdurur ve yeni sesleri hic baslatmaz. -80 dB'de kalan sesler oyuna
 * donulunce kaldiklari yerden duyulur (arka plan muzigi yeniden baslamaz).
 */
public final class FocusMute {

    private static boolean muted;

    private FocusMute() {}

    /** Ses motorunun her sese uyguladigi carpan (SoundEngineMixin). */
    public static float factor() {
        return muted ? MusicSettings.PREVIEW_MUTE : 1.0F;
    }

    public static boolean isMuted() {
        return muted;
    }

    /** Her tick: odak ya da ayar degisince calan butun seslerin seviyesi yeniden hesaplanir. */
    public static void clientTick(Minecraft mc) {
        boolean shouldMute = ModOptions.muteAllUnfocused && !mc.isWindowActive();
        if (shouldMute == muted) return;
        muted = shouldMute;
        refreshAll(mc);
    }

    private static void refreshAll(Minecraft mc) {
        for (SoundSource source : SoundSource.values()) {
            if (source == SoundSource.MASTER) continue;
            //? if >=26.1 {
            mc.getSoundManager().updateCategoryVolume(source, mc.options.getSoundSourceVolume(source));
            //?} else {
            /*mc.getSoundManager().updateSourceVolume(source, mc.options.getSoundSourceVolume(source));
            *///?}
        }
    }
}
