package com.musicbox.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.musicbox.MusicBox;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Her parca icin kullanici ayarlari: ses seviyesi (%0-100) ve acik/kapali.
 *
 * config/musicbox.json dosyasinda saklanir. Sadece varsayilandan farkli olan parcalar yazilir:
 * <pre>
 * {
 *   "tracks": {
 *     "minecraft:music/game/calm1": { "volume": 40, "enabled": true },
 *     "minecraft:records/cat":      { "volume": 100, "enabled": false }
 *   }
 * }
 * </pre>
 */
public final class MusicSettings {

    public static final int DEFAULT_VOLUME = 100;

    /** Bir parcanin ayari. Varsayilan: %100, acik. */
    public record TrackSetting(int volume, boolean enabled) {
        static final TrackSetting DEFAULT = new TrackSetting(DEFAULT_VOLUME, true);

        boolean isDefault() {
            return volume == DEFAULT_VOLUME && enabled;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, TrackSetting> SETTINGS = new ConcurrentHashMap<>();
    /** Kapali parca sayisi; sifirsa parca secimine hic karismayiz (hiz icin). */
    private static volatile int disabledCount = 0;

    private MusicSettings() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(MusicBox.MODID + ".json");
    }

    // ---------------------------------------------------------------- okuma (oyun icinden cagrilir)

    public static TrackSetting get(ResourceLocation track) {
        return SETTINGS.getOrDefault(track, TrackSetting.DEFAULT);
    }

    public static boolean hasDisabled() {
        return disabledCount > 0;
    }

    public static boolean isDisabled(ResourceLocation track) {
        TrackSetting s = SETTINGS.get(track);
        return s != null && !s.enabled();
    }

    /** Ses seviyesi carpani: kapaliysa 0, degilse volume/100. */
    public static float factor(ResourceLocation track) {
        TrackSetting s = SETTINGS.get(track);
        if (s == null) return 1.0F;
        return s.enabled() ? s.volume() / 100.0F : 0.0F;
    }

    /** Calan bir ses icin carpan. Ses henuz bir dosyaya cozulmemisse etkilenmez. */
    public static float factor(SoundInstance instance) {
        // Onizleme: kapali olsa bile dinlenebilsin, ama ayarlanan ses seviyesiyle.
        if (instance instanceof MusicPreview.PreviewSound preview) return get(preview.trackId).volume() / 100.0F;
        // Onizleme calarken oyunun kendi arka plan muzigi susar.
        if (MusicPreview.hasPreview() && instance.getSource() == SoundSource.MUSIC) return 0.0F;
        if (SETTINGS.isEmpty()) return 1.0F;
        Sound sound;
        try {
            sound = instance.getSound();
        } catch (RuntimeException e) {
            return 1.0F; // bazi modlarin ozel ses siniflari cozulmeden once hata verebilir
        }
        return sound == null ? 1.0F : factor(sound.getLocation());
    }

    // ---------------------------------------------------------------- degistirme

    public static void setVolume(ResourceLocation track, int volume) {
        put(track, new TrackSetting(Math.max(0, Math.min(100, volume)), get(track).enabled()));
    }

    public static void setEnabled(ResourceLocation track, boolean enabled) {
        put(track, new TrackSetting(get(track).volume(), enabled));
    }

    public static void reset(ResourceLocation track) {
        SETTINGS.remove(track);
        recount();
    }

    public static void resetAll() {
        SETTINGS.clear();
        recount();
    }

    private static void put(ResourceLocation track, TrackSetting setting) {
        if (setting.isDefault()) SETTINGS.remove(track);
        else SETTINGS.put(track, setting);
        recount();
    }

    private static void recount() {
        disabledCount = (int) SETTINGS.values().stream().filter(s -> !s.enabled()).count();
    }

    /**
     * Degisiklikleri calmakta olan seslere hemen uygular: ses seviyeleri yeniden hesaplanir,
     * kapatilan (carpani 0 olan) parcalar durdurulur.
     */
    public static void applyToPlayingSounds() {
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().updateSourceVolume(SoundSource.MUSIC, mc.options.getSoundSourceVolume(SoundSource.MUSIC));
    }

    // ---------------------------------------------------------------- dosya

    public static void load() {
        SETTINGS.clear();
        Path path = file();
        if (Files.exists(path)) {
            try {
                JsonObject root = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
                JsonObject tracks = root != null && root.has("tracks") ? root.getAsJsonObject("tracks") : new JsonObject();
                for (String key : tracks.keySet()) {
                    ResourceLocation id = ResourceLocation.tryParse(key);
                    if (id == null) continue;
                    JsonObject o = tracks.getAsJsonObject(key);
                    int volume = o.has("volume") ? o.get("volume").getAsInt() : DEFAULT_VOLUME;
                    boolean enabled = !o.has("enabled") || o.get("enabled").getAsBoolean();
                    put(id, new TrackSetting(Math.max(0, Math.min(100, volume)), enabled));
                }
            } catch (Exception e) {
                MusicBox.LOGGER.error("{} okunamadi, varsayilan ayarlar kullaniliyor", path, e);
            }
        }
        recount();
        MusicBox.LOGGER.info("Music Box ayarlari yuklendi: {} parca ozel ayarli, {} kapali", SETTINGS.size(), disabledCount);
    }

    public static void save() {
        JsonObject tracks = new JsonObject();
        new TreeMap<>(Map.copyOf(SETTINGS)).forEach((id, s) -> {
            JsonObject o = new JsonObject();
            o.addProperty("volume", s.volume());
            o.addProperty("enabled", s.enabled());
            tracks.add(id.toString(), o);
        });
        JsonObject root = new JsonObject();
        root.add("tracks", tracks);
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            MusicBox.LOGGER.error("{} yazilamadi", file(), e);
        }
    }
}
