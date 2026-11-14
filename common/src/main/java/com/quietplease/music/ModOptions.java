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

    public static int popupScale = 100;
    public static int popupDuration = 4;
    public static int popupOffsetX = 0;
    public static int popupOffsetY = 0;
    public static PopupColor popupTitleColor = PopupColor.GOLD;
    public static PopupColor popupModColor = PopupColor.BEIGE;
    public static TextStyle popupTextStyle = TextStyle.NORMAL;
    public static boolean popupShadow = true;
    public static PopupBackground popupBackground = PopupBackground.NONE;
    public static boolean popupShowIcon = true;
    public static boolean popupShowMod = true;
    public static boolean popupEqualizer = true;

    public static final int POPUP_SCALE_MIN = 50;
    public static final int POPUP_SCALE_MAX = 200;
    public static final int POPUP_DURATION_MIN = 2;
    public static final int POPUP_DURATION_MAX = 15;
    public static final int POPUP_OFFSET_X_MAX = 400;
    public static final int POPUP_OFFSET_Y_MAX = 300;

    public static final int[] JUKEBOX_VALUES = {100, 25, 0};
    public static final int[] UNFOCUSED_VALUES = {100, 30, 0};
    public static final int[] COMBAT_VALUES = {100, 60, 30};
    public static final int[] FADE_VALUES = {0, 2, 5};
    public static final int[] NO_REPEAT_VALUES = {0, 5, 10};
    public static final int[] FAVORITE_VALUES = {1, 2, 3, 5};

    public static int jukeboxVolume = 0;
    public static int unfocusedVolume = 100;
    public static int combatVolume = 100;
    public static int fadeSeconds = 2;
    public static int noRepeat = 5;
    public static int favoriteWeight = 2;

    public enum PopupPosition {
        RIGHT_MIDDLE("right_middle", true, 1),
        RIGHT_TOP("right_top", true, 0),
        RIGHT_BOTTOM("right_bottom", true, 2),
        LEFT_TOP("left_top", false, 0),
        LEFT_MIDDLE("left_middle", false, 1),
        LEFT_BOTTOM("left_bottom", false, 2);

        public final String id;
        public final boolean right;
        public final int row;

        PopupPosition(String id, boolean right, int row) {
            this.id = id;
            this.right = right;
            this.row = row;
        }

        public Component displayName() {
            return Lang.tr("settings.popup_position." + id);
        }

        static PopupPosition byId(String id) {
            for (PopupPosition p : values()) if (p.id.equals(id)) return p;
            return RIGHT_MIDDLE;
        }
    }

    public enum PopupColor {
        GOLD("gold", 0xFFC966),
        BEIGE("beige", 0xD8CCB4),
        WHITE("white", 0xFFFFFF),
        GRAY("gray", 0xAAAAAA),
        YELLOW("yellow", 0xFFFF66),
        ORANGE("orange", 0xFFA040),
        RED("red", 0xFF6666),
        PINK("pink", 0xFF8FD0),
        PURPLE("purple", 0xC79BFF),
        BLUE("blue", 0x7FB2FF),
        AQUA("aqua", 0x66FFFF),
        GREEN("green", 0x7EE08A);

        public final String id;
        public final int rgb;

        PopupColor(String id, int rgb) {
            this.id = id;
            this.rgb = rgb;
        }

        public int argb() {
            return 0xFF000000 | rgb;
        }

        public Component displayName() {
            return Lang.tr("popup.color." + id).withStyle(s -> s.withColor(rgb));
        }

        static PopupColor byId(String id, PopupColor fallback) {
            for (PopupColor c : values()) if (c.id.equals(id)) return c;
            return fallback;
        }
    }

    public enum TextStyle {
        NORMAL("normal", false, false),
        BOLD("bold", true, false),
        ITALIC("italic", false, true),
        BOLD_ITALIC("bold_italic", true, true);

        public final String id;
        public final boolean bold;
        public final boolean italic;

        TextStyle(String id, boolean bold, boolean italic) {
            this.id = id;
            this.bold = bold;
            this.italic = italic;
        }

        public Component displayName() {
            return Lang.tr("popup.style." + id).withStyle(s -> s.withBold(bold).withItalic(italic));
        }

        static TextStyle byId(String id) {
            for (TextStyle t : values()) if (t.id.equals(id)) return t;
            return NORMAL;
        }
    }

    public enum PopupBackground {
        NONE("none"),
        SOFT("soft"),
        DARK("dark"),
        FRAMED("framed");

        public final String id;

        PopupBackground(String id) {
            this.id = id;
        }

        public Component displayName() {
            return Lang.tr("popup.background." + id);
        }

        static PopupBackground byId(String id) {
            for (PopupBackground b : values()) if (b.id.equals(id)) return b;
            return NONE;
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
        if (music != null && music == lastAdjusted) return true;
        if (music == Musics.END_BOSS || music == Musics.CREDITS) return false;
        return !music.replaceCurrentMusic() || music == Musics.MENU || music == Musics.END;
    }

    private static Music lastOriginal;
    private static Music lastAdjusted;

    /** MusicManager.tick() her tick'te cagirir. Arka plan muzigi degilse ya da ayar Vanilla ise ayni nesne doner. */
    public static Music adjustGap(Music music) {
        if (music == null || gap == Gap.VANILLA || music == lastAdjusted || !isBackground(music)) return music;
        int min = gapMinTicks();
        int max = gapMaxTicks();
        // Her tick yeni nesne olusturmamak icin son sonucu sakla
        if (lastOriginal == music && lastAdjusted.minDelay() == min && lastAdjusted.maxDelay() == max) return lastAdjusted;
        lastOriginal = music;
        lastAdjusted = new Music(music.sound(), min, max, music.replaceCurrentMusic());
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
        fadeOnSkip = true;
        jukeboxVolume = 0;
        unfocusedVolume = 100;
        combatVolume = 100;
        fadeSeconds = 2;
        noRepeat = 5;
        favoriteWeight = 2;
        resetPopup();
    }

    public static void resetPopup() {
        popupPosition = PopupPosition.RIGHT_MIDDLE;
        popupScale = 100;
        popupDuration = 4;
        popupOffsetX = 0;
        popupOffsetY = 0;
        popupTitleColor = PopupColor.GOLD;
        popupModColor = PopupColor.BEIGE;
        popupTextStyle = TextStyle.NORMAL;
        popupShadow = true;
        popupBackground = PopupBackground.NONE;
        popupShowIcon = true;
        popupShowMod = true;
        popupEqualizer = true;
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
        popupScale = integer(o, "popupScale", popupScale, POPUP_SCALE_MIN, POPUP_SCALE_MAX);
        popupDuration = integer(o, "popupDuration", popupDuration, POPUP_DURATION_MIN, POPUP_DURATION_MAX);
        popupOffsetX = integer(o, "popupOffsetX", popupOffsetX, 0, POPUP_OFFSET_X_MAX);
        popupOffsetY = integer(o, "popupOffsetY", popupOffsetY, -POPUP_OFFSET_Y_MAX, POPUP_OFFSET_Y_MAX);
        if (o.has("popupTitleColor")) popupTitleColor = PopupColor.byId(string(o, "popupTitleColor"), PopupColor.GOLD);
        if (o.has("popupModColor")) popupModColor = PopupColor.byId(string(o, "popupModColor"), PopupColor.BEIGE);
        if (o.has("popupTextStyle")) popupTextStyle = TextStyle.byId(string(o, "popupTextStyle"));
        popupShadow = bool(o, "popupShadow", popupShadow);
        if (o.has("popupBackground")) popupBackground = PopupBackground.byId(string(o, "popupBackground"));
        popupShowIcon = bool(o, "popupShowIcon", popupShowIcon);
        popupShowMod = bool(o, "popupShowMod", popupShowMod);
        popupEqualizer = bool(o, "popupEqualizer", popupEqualizer);
        jukeboxVolume = choice(o, "jukeboxVolume", jukeboxVolume, JUKEBOX_VALUES);
        unfocusedVolume = choice(o, "unfocusedVolume", unfocusedVolume, UNFOCUSED_VALUES);
        combatVolume = choice(o, "combatVolume", combatVolume, COMBAT_VALUES);
        fadeSeconds = choice(o, "fadeSeconds", fadeSeconds, FADE_VALUES);
        noRepeat = choice(o, "noRepeat", noRepeat, NO_REPEAT_VALUES);
        favoriteWeight = choice(o, "favoriteWeight", favoriteWeight, FAVORITE_VALUES);
        MusicMixer.optionsChanged();
    }

    private static int choice(JsonObject o, String key, int fallback, int[] allowed) {
        if (!o.has(key)) return fallback;
        int v = o.get(key).getAsInt();
        int best = allowed[0];
        for (int a : allowed) if (Math.abs(a - v) < Math.abs(best - v)) best = a;
        return best;
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
        o.addProperty("popupScale", popupScale);
        o.addProperty("popupDuration", popupDuration);
        o.addProperty("popupOffsetX", popupOffsetX);
        o.addProperty("popupOffsetY", popupOffsetY);
        o.addProperty("popupTitleColor", popupTitleColor.id);
        o.addProperty("popupModColor", popupModColor.id);
        o.addProperty("popupTextStyle", popupTextStyle.id);
        o.addProperty("popupShadow", popupShadow);
        o.addProperty("popupBackground", popupBackground.id);
        o.addProperty("popupShowIcon", popupShowIcon);
        o.addProperty("popupShowMod", popupShowMod);
        o.addProperty("popupEqualizer", popupEqualizer);
        o.addProperty("jukeboxVolume", jukeboxVolume);
        o.addProperty("unfocusedVolume", unfocusedVolume);
        o.addProperty("combatVolume", combatVolume);
        o.addProperty("fadeSeconds", fadeSeconds);
        o.addProperty("noRepeat", noRepeat);
        o.addProperty("favoriteWeight", favoriteWeight);
        return o;
    }

    private static int integer(JsonObject o, String key, int fallback, int min, int max) {
        return o.has(key) ? Mth.clamp(o.get(key).getAsInt(), min, max) : fallback;
    }

    private static String string(JsonObject o, String key) {
        return o.get(key).getAsString().toLowerCase(Locale.ROOT);
    }

    private static boolean bool(JsonObject o, String key, boolean fallback) {
        return o.has(key) ? o.get(key).getAsBoolean() : fallback;
    }
}
