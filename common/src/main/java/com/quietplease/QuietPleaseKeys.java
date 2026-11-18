package com.quietplease;

import com.mojang.blaze3d.platform.InputConstants;
import com.quietplease.gui.QuietPleaseScreen;
import com.quietplease.music.ModOptions;
import com.quietplease.music.MusicControl;
import com.quietplease.music.MusicSettings;
import com.quietplease.music.Playlist;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Kisayol tuslari: Ayarlar → Kontroller → Tus Atamalari → "Quiet Please".
 *
 * Varsayilan olarak hicbir tusa atanmamis (baska modlarin tuslariyla cakismasin); oyuncu istedigi tusu secer.
 * Tuslar sadece oyundayken ve acik bir ekran yokken calisir (Minecraft'in kendi kurali).
 */
public final class QuietPleaseKeys {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("quietplease", "main"));

    public static final KeyMapping OPEN_PANEL =
            new KeyMapping("key.quietplease.open", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping SKIP =
            new KeyMapping("key.quietplease.skip", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping FAVORITE =
            new KeyMapping("key.quietplease.favorite", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping PLAYLIST_MODE =
            new KeyMapping("key.quietplease.playlist_mode", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping PREVIOUS =
            new KeyMapping("key.quietplease.previous", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping DISABLE =
            new KeyMapping("key.quietplease.disable", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping PAUSE =
            new KeyMapping("key.quietplease.pause", InputConstants.UNKNOWN.getValue(), CATEGORY);

    private QuietPleaseKeys() {}

    public static java.util.List<KeyMapping> all() {
        return java.util.List.of(OPEN_PANEL, SKIP, PREVIOUS, PAUSE, FAVORITE, DISABLE, PLAYLIST_MODE);
    }

    /** Duraklat / devam: ne oldugunu anlatan kisa mesaj. */
    public static Component togglePause() {
        String result = MusicControl.togglePause();
        return Lang.tr(result).withStyle(result.equals("key.pause.nothing") ? ChatFormatting.GRAY
                : result.equals("key.pause.paused") ? ChatFormatting.YELLOW : ChatFormatting.GREEN);
    }

    /** Her istemci tick'inde: basilan tuslari isle. */
    public static void tick(Minecraft mc) {
        while (OPEN_PANEL.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new QuietPleaseScreen(null));
        }
        while (SKIP.consumeClick()) {
            String result = MusicControl.skipAndDescribe();
            // Sohbeti doldurmasin: kisa mesaj, hotbar'in ustunde birkac saniye
            mc.gui.setOverlayMessage(Lang.tr(result).withStyle(
                    result.equals("cmd.skip.nothing") ? ChatFormatting.GRAY : ChatFormatting.GREEN), false);
        }
        while (PREVIOUS.consumeClick()) {
            mc.gui.setOverlayMessage(previous(), false);
        }
        while (DISABLE.consumeClick()) {
            mc.gui.setOverlayMessage(disablePlaying(mc), false);
        }
        while (PAUSE.consumeClick()) {
            mc.gui.setOverlayMessage(togglePause(), false);
        }
        while (FAVORITE.consumeClick()) {
            mc.gui.setOverlayMessage(toggleFavoriteOfPlaying(mc), false);
        }
        while (PLAYLIST_MODE.consumeClick()) {
            ModOptions.playlistMode = !ModOptions.playlistMode;
            MusicSettings.save();
            Component state = Lang.tr(ModOptions.playlistMode ? "screen.on" : "screen.off");
            String key = ModOptions.playlistMode && Playlist.isEmpty() ? "key.playlist_mode.empty" : "key.playlist_mode.changed";
            mc.gui.setOverlayMessage(Lang.tr(key, state).withStyle(
                    ModOptions.playlistMode ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        }
    }

    /** Gecmisteki bir onceki sarkiyi calar; ne oldugunu anlatan kisa mesaji dondurur. */
    public static Component previous() {
        Identifier id = MusicControl.previous();
        if (id == null) return Lang.tr("key.previous.nothing").withStyle(ChatFormatting.GRAY);
        return Lang.tr("key.previous.playing", shortName(id)).withStyle(ChatFormatting.GREEN);
    }

    /** "Bir daha calma": calan arka plan sarkisini kapatir (siradakine gecilir). */
    public static Component disablePlaying(Minecraft mc) {
        Identifier id = playingTrack(mc);
        if (id == null) return Lang.tr("key.favorite.nothing").withStyle(ChatFormatting.GRAY);
        MusicSettings.setEnabled(id, false);
        MusicSettings.save();
        MusicSettings.applyToPlayingSounds();
        return Lang.tr("key.disable.done", shortName(id)).withStyle(ChatFormatting.RED);
    }

    private static String shortName(Identifier id) {
        return com.quietplease.music.MusicTrack.title(id);
    }

    /** Calan arka plan sarkisini favorilere ekler ya da cikarir; ne oldugunu anlatan kisa mesaji dondurur. */
    public static Component toggleFavoriteOfPlaying(Minecraft mc) {
        Identifier id = playingTrack(mc);
        if (id == null) return Lang.tr("key.favorite.nothing").withStyle(ChatFormatting.GRAY);
        boolean added = Playlist.toggleFavorite(id);
        MusicSettings.save();
        String name = shortName(id);
        return Lang.tr(added ? "key.favorite.added" : "key.favorite.removed", name)
                .withStyle(added ? ChatFormatting.GOLD : ChatFormatting.GRAY);
    }

    @Nullable
    private static Identifier playingTrack(Minecraft mc) {
        SoundInstance current = ((com.quietplease.mixin.MusicManagerAccessor) mc.getMusicManager()).quietplease$getCurrentMusic();
        if (current != null && mc.getSoundManager().isActive(current)) {
            if (current instanceof Playlist.PlaylistSound ps) return ps.trackId;
            Sound sound = current.getSound();
            if (sound != null) return sound.getLocation();
        }
        Playlist.PlaylistSound fromList = Playlist.playing();
        return fromList == null ? null : fromList.trackId;
    }
}
