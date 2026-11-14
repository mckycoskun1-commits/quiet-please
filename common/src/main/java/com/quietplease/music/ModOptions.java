package com.quietplease.music;

import com.google.gson.JsonObject;
import com.quietplease.Lang;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.util.Mth;

/**
 * Modun genel ayarlari (Ayarlar ekrani). config/quietplease.json icindeki "options" bolumunde saklanir:
 * <pre>
 * "options": { "nowPlayingText": true, "pauseMenuButton": true, "optionsButton": true, "showAmbient": false,
 *              "playlistMode": false, "gap": "vanilla", "gapMinSeconds": 30, "gapMaxSeconds": 60 }
 * </pre>
 * Eski dosyalarda bu bolum yoktur; o zaman varsayilanlar kullanilir.
 */
public final class ModOptions {

    /** Iki arka plan sarkisi arasindaki sessizlik. */
    public enum Gap {
        /** Oyunun kendi suresi (degismez). */
        VANILLA("vanilla"),
        /** 30-60 sn. */
        SHORT("short"),
        /** Neredeyse hic (1 sn). */
        NONE("none"),
        /** Kaydiricilardaki en kisa / en uzun sure. */
        CUSTOM("custom");

        public final String id;

        Gap(String id) {
            this.id = id;
        }

        public Component displayName() {
            return Lang.tr("settings.gap." + id);
        }

        static Gap byId(String id) {
            for (Gap g : values()) if (g.id.equals(id)) return g;
            return VANILLA;
        }
    }

    public static final int GAP_SLIDER_MAX_SECONDS = 1200;

    public static boolean nowPlayingText = true;
    public static boolean pauseMenuButton = true;
    public static boolean optionsButton = true;
    public static boolean showAmbient = false;
    public static boolean playlistMode = false;
    public static Gap gap = Gap.VANILLA;
    public static int gapMinSeconds = 30;
    public static int gapMaxSeconds = 60;
    public static boolean songPopup = true;
    public static PopupPosition popupPosition = PopupPosition.RIGHT_MIDDLE;
    public static boolean fadeOnSkip = true;

    public enum PopupPosition {
        RIGHT_MIDDLE("right_middle", true),
        LEFT_MIDDLE("left_middle", false),
        RIGHT_BOTTOM("right_bottom", true),
        LEFT_TOP("left_top", false);

        public final String id;
        public final boolean right;

        PopupPosition(String id, boolean right) {
            this.id = id;
            this.right = right;
        }

        public Component displayName() {
            return Lang.tr("settings.popup_position." + id);
        }

        static PopupPosition byId(String id) {
            for (PopupPosition p : values()) if (p.id.equals(id)) return p;
            return RIGHT_MIDDLE;
        }
    }

    private ModOptions() {}

    // ---------------------------------------------------------------- sarki arasi

    /**
     * Oyunun "durum muzigi" bir arka plan muzigi mi? Sadece bunlarin bekleme suresi degistirilir ve
     * bunlarin yerine calma listesi calar. Ejderha, jenerik ve modlarin "mevcut muzigi kes" isaretli muzikleri
     * (cogu zaman boss muzikleri) haric. Menu ve End vanilla'da da "kes" isaretli ama sadece arka plan muzigi.
     */
    public static boolean isBackground(Music music) {
        if (music instanceof AdjustedMusic) return true; // zaten arka plan diye kopyalanmisti
        if (music == Musics.END_BOSS || music == Musics.CREDITS) return false;
        return !music.replaceCurrentMusic() || music == Musics.MENU || music == Musics.END;
    }

    /** Ayara gore bekleme suresi degistirilmis kopya. Kopyalayinca da {@link #isBackground} dogru kalir. */
    public static final class AdjustedMusic extends Music {
        final Music original;

        AdjustedMusic(Music original, int minTicks, int maxTicks) {
            super(original.getEvent(), minTicks, maxTicks, original.replaceCurrentMusic());
            this.original = original;
        }
    }

    private static Music lastOriginal;
    private static Music lastAdjusted;

    /** MusicManager.tick() her tick'te cagirir. Arka plan muzigi degilse ya da ayar Vanilla ise ayni nesne doner. */
    public static Music adjustGap(Music music) {
        if (music == null || gap == Gap.VANILLA || music instanceof AdjustedMusic || !isBackground(music)) return music;
        int min = gapMinTicks();
        int max = gapMaxTicks();
        // Her tick yeni nesne olusturmamak icin son sonucu sakla
        if (lastOriginal == music && lastAdjusted.getMinDelay() == min && lastAdjusted.getMaxDelay() == max) return lastAdjusted;
        lastOriginal = music;
        lastAdjusted = new AdjustedMusic(music, min, max);
        return lastAdjusted;
    }

    public static int gapMinTicks() {
        return switch (gap) {
            case VANILLA -> -1;
            case SHORT -> 30 * 20;
            case NONE -> 20;
            case CUSTOM -> Math.max(20, Math.min(gapMinSeconds, gapMaxSeconds) * 20);
        };
    }

    public static int gapMaxTicks() {
        return switch (gap) {
            case VANILLA -> -1;
            case SHORT -> 60 * 20;
            case NONE -> 20;
            case CUSTOM -> Math.max(20, Math.max(gapMinSeconds, gapMaxSeconds) * 20);
        };
    }

    // ---------------------------------------------------------------- dosya (MusicSettings okur/yazar)

    static void reset() {
        nowPlayingText = true;
        pauseMenuButton = true;
        optionsButton = true;
        showAmbient = false;
        playlistMode = false;
        gap = Gap.VANILLA;
        gapMinSeconds = 30;
        gapMaxSeconds = 60;
        songPopup = true;
        popupPosition = PopupPosition.RIGHT_MIDDLE;
        fadeOnSkip = true;
    }

    static void read(JsonObject o) {
        reset();
        if (o == null) return;
        nowPlayingText = bool(o, "nowPlayingText", nowPlayingText);
        pauseMenuButton = bool(o, "pauseMenuButton", pauseMenuButton);
        optionsButton = bool(o, "optionsButton", optionsButton);
        showAmbient = bool(o, "showAmbient", showAmbient);
        playlistMode = bool(o, "playlistMode", playlistMode);
        if (o.has("gap")) gap = Gap.byId(o.get("gap").getAsString().toLowerCase(Locale.ROOT));
        if (o.has("gapMinSeconds")) gapMinSeconds = Mth.clamp(o.get("gapMinSeconds").getAsInt(), 0, GAP_SLIDER_MAX_SECONDS);
        if (o.has("gapMaxSeconds")) gapMaxSeconds = Mth.clamp(o.get("gapMaxSeconds").getAsInt(), 0, GAP_SLIDER_MAX_SECONDS);
        songPopup = bool(o, "songPopup", songPopup);
        if (o.has("popupPosition")) popupPosition = PopupPosition.byId(o.get("popupPosition").getAsString().toLowerCase(Locale.ROOT));
        fadeOnSkip = bool(o, "fadeOnSkip", fadeOnSkip);
    }

    static JsonObject write() {
        JsonObject o = new JsonObject();
        o.addProperty("nowPlayingText", nowPlayingText);
        o.addProperty("pauseMenuButton", pauseMenuButton);
        o.addProperty("optionsButton", optionsButton);
        o.addProperty("showAmbient", showAmbient);
        o.addProperty("playlistMode", playlistMode);
        o.addProperty("gap", gap.id);
        o.addProperty("gapMinSeconds", gapMinSeconds);
        o.addProperty("gapMaxSeconds", gapMaxSeconds);
        o.addProperty("songPopup", songPopup);
        o.addProperty("popupPosition", popupPosition.id);
        o.addProperty("fadeOnSkip", fadeOnSkip);
        return o;
    }

    private static boolean bool(JsonObject o, String key, boolean fallback) {
        return o.has(key) ? o.get(key).getAsBoolean() : fallback;
    }
}
