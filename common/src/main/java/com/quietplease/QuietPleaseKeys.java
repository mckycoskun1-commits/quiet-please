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
import net.minecraft.resources.ResourceLocation;

/**
 * Kisayol tuslari: Ayarlar → Kontroller → Tus Atamalari → "Quiet Please".
 *
 * Varsayilan olarak hicbir tusa atanmamis (baska modlarin tuslariyla cakismasin); oyuncu istedigi tusu secer.
 * Tuslar sadece oyundayken ve acik bir ekran yokken calisir (Minecraft'in kendi kurali).
 */
public final class QuietPleaseKeys {

    private static final String CATEGORY = "key.categories.quietplease";

    public static final KeyMapping OPEN_PANEL =
            new KeyMapping("key.quietplease.open", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping SKIP =
            new KeyMapping("key.quietplease.skip", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping FAVORITE =
            new KeyMapping("key.quietplease.favorite", InputConstants.UNKNOWN.getValue(), CATEGORY);
    public static final KeyMapping PLAYLIST_MODE =
            new KeyMapping("key.quietplease.playlist_mode", InputConstants.UNKNOWN.getValue(), CATEGORY);

    private QuietPleaseKeys() {}

    public static java.util.List<KeyMapping> all() {
        return java.util.List.of(OPEN_PANEL, SKIP, FAVORITE, PLAYLIST_MODE);
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

    /** Calan arka plan sarkisini favorilere ekler ya da cikarir; ne oldugunu anlatan kisa mesaji dondurur. */
    public static Component toggleFavoriteOfPlaying(Minecraft mc) {
        ResourceLocation id = playingTrack(mc);
        if (id == null) return Lang.tr("key.favorite.nothing").withStyle(ChatFormatting.GRAY);
        boolean added = Playlist.toggleFavorite(id);
        MusicSettings.save();
        String name = id.getPath().substring(id.getPath().lastIndexOf('/') + 1);
        return Lang.tr(added ? "key.favorite.added" : "key.favorite.removed", name)
                .withStyle(added ? ChatFormatting.GOLD : ChatFormatting.GRAY);
    }

    @Nullable
    private static ResourceLocation playingTrack(Minecraft mc) {
        Playlist.PlaylistSound fromList = Playlist.playing();
        if (fromList != null) return fromList.trackId;
        SoundInstance current = ((com.quietplease.mixin.MusicManagerAccessor) mc.getMusicManager()).quietplease$getCurrentMusic();
        if (current == null || !mc.getSoundManager().isActive(current)) return null;
        Sound sound = current.getSound();
        return sound == null ? null : sound.getLocation();
    }
}
