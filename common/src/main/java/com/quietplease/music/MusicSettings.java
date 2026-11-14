package com.quietplease.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.quietplease.QuietPlease;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

/**
 * Her parca icin kullanici ayarlari: ses seviyesi (%0-100) ve acik/kapali.
 *
 * config/quietplease.json dosyasinda saklanir. Sadece varsayilandan farkli olan parcalar yazilir:
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
    /** Mod kimligi -> o modun tum parcalari icin ses seviyesi; sadece %100'den farkli olanlar. */
    private static final Map<String, Integer> MOD_VOLUME = new ConcurrentHashMap<>();
    /** Kapali parca sayisi; sifirsa parca secimine hic karismayiz (hiz icin). */
    private static volatile int disabledCount = 0;

    private MusicSettings() {}

    private static Path file() {
        return com.quietplease.platform.Services.PLATFORM.configDir().resolve(QuietPlease.MODID + ".json");
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

    /** Ses seviyesi carpani: kapaliysa 0, degilse parcanin ve modunun ses seviyesi carpimi. */
    public static float factor(ResourceLocation track) {
        float mod = modFactor(track);
        TrackSetting s = SETTINGS.get(track);
        if (s == null) return mod;
        return s.enabled() ? s.volume() / 100.0F * mod : 0.0F;
    }

    /** Bir modun tum parcalari icin ses seviyesi (%0-100, varsayilan 100). */
    public static int modVolume(String namespace) {
        return MOD_VOLUME.getOrDefault(namespace, DEFAULT_VOLUME);
    }

    public static void setModVolume(String namespace, int volume) {
        int v = Math.max(0, Math.min(100, volume));
        if (v == DEFAULT_VOLUME) MOD_VOLUME.remove(namespace);
        else MOD_VOLUME.put(namespace, v);
    }

    private static float modFactor(ResourceLocation track) {
        if (MOD_VOLUME.isEmpty()) return 1.0F;
        Integer v = MOD_VOLUME.get(track.getNamespace());
        return v == null ? 1.0F : v / 100.0F;
    }

    /** Calan bir ses icin carpan. Ses henuz bir dosyaya cozulmemisse etkilenmez. */
    public static float factor(SoundInstance instance) {
        // Onizleme: kapali olsa bile dinlenebilsin, ama ayarlanan ses seviyesiyle.
        if (instance instanceof MusicPreview.PreviewSound preview) {
            return get(preview.trackId).volume() / 100.0F * modFactor(preview.trackId);
        }
        // Onizleme calarken oyunun kendi arka plan muzigi susar.
        if (MusicPreview.hasPreview() && instance.getSource() == SoundSource.MUSIC) return 0.0F;
        if (SETTINGS.isEmpty() && MOD_VOLUME.isEmpty()) return 1.0F;
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
        MOD_VOLUME.clear();
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
        Path path = file();
        JsonObject root = null;
        if (Files.exists(path)) {
            try {
                root = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
            } catch (Exception e) {
                QuietPlease.LOGGER.error("{} okunamadi, varsayilan ayarlar kullaniliyor", path, e);
                backupBroken(path);
            }
        }
        readRoot(root);
        QuietPlease.LOGGER.info("Quiet Please ayarlari yuklendi: {} parca ozel ayarli, {} kapali", SETTINGS.size(), disabledCount);
    }

    /** Dosyanin tamamini okur. Eksik bolumler varsayilan olur; boylece eski dosyalar da aynen yuklenir. */
    static void readRoot(JsonObject root) {
        SETTINGS.clear();
        try {
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
            QuietPlease.LOGGER.error("Parca ayarlari okunamadi", e);
        }
        MOD_VOLUME.clear();
        try {
            JsonObject mods = root != null && root.has("mods") ? root.getAsJsonObject("mods") : new JsonObject();
            for (String mod : mods.keySet()) {
                JsonObject o = mods.getAsJsonObject(mod);
                if (o.has("volume")) setModVolume(mod, o.get("volume").getAsInt());
            }
        } catch (Exception e) {
            QuietPlease.LOGGER.error("Mod ses seviyeleri okunamadi", e);
        }
        try {
            ModOptions.read(root != null && root.has("options") ? root.getAsJsonObject("options") : null);
        } catch (Exception e) {
            QuietPlease.LOGGER.error("Ayarlar okunamadi, varsayilanlar kullaniliyor", e);
            ModOptions.reset();
        }
        try {
            Playlist.read(root != null && root.has("playlist") ? root.getAsJsonObject("playlist") : null);
        } catch (Exception e) {
            QuietPlease.LOGGER.error("Calma listesi okunamadi, bos liste kullaniliyor", e);
            Playlist.read(null);
        }
        recount();
    }

    /** Dosyanin tamami: parca ayarlari + genel ayarlar. */
    static JsonObject writeRoot() {
        JsonObject tracks = new JsonObject();
        new TreeMap<>(Map.copyOf(SETTINGS)).forEach((id, s) -> {
            JsonObject o = new JsonObject();
            o.addProperty("volume", s.volume());
            o.addProperty("enabled", s.enabled());
            tracks.add(id.toString(), o);
        });
        JsonObject root = new JsonObject();
        root.add("tracks", tracks);
        JsonObject mods = new JsonObject();
        new TreeMap<>(Map.copyOf(MOD_VOLUME)).forEach((mod, v) -> {
            JsonObject o = new JsonObject();
            o.addProperty("volume", v);
            mods.add(mod, o);
        });
        root.add("mods", mods);
        root.add("options", ModOptions.write());
        root.add("playlist", Playlist.write());
        return root;
    }

    // ---------------------------------------------------------------- hazir ayarlar (preset)

    /** Hazir ayar adi: sadece harf, rakam, _ ve - (dosya adi olarak guvenli). */
    public static boolean isValidPresetName(String name) {
        return name.matches("[A-Za-z0-9_-]{1,64}");
    }

    private static Path presetDir() {
        return com.quietplease.platform.Services.PLATFORM.configDir().resolve(QuietPlease.MODID + "-presets");
    }

    public static Path presetFile(String name) {
        return presetDir().resolve(name + ".json");
    }

    /** Tum ayarlari (parcalar, ayarlar, calma listesi) config/quietplease-presets/&lt;ad&gt;.json dosyasina yazar. */
    public static Path exportPreset(String name) throws IOException {
        Path path = presetFile(name);
        writeAtomically(path, GSON.toJson(writeRoot()));
        return path;
    }

    /** Hazir ayari yukler ve config/quietplease.json olarak kaydeder. */
    public static void importPreset(String name) throws IOException {
        JsonObject root = GSON.fromJson(Files.readString(presetFile(name), StandardCharsets.UTF_8), JsonObject.class);
        if (root == null) throw new IOException("empty file");
        readRoot(root);
        save();
    }

    /** Kayitli hazir ayarlarin adlari (komut onerileri icin). */
    public static java.util.List<String> presetNames() {
        try (var files = Files.list(presetDir())) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".json"))
                    .map(n -> n.substring(0, n.length() - 5))
                    .filter(MusicSettings::isValidPresetName)
                    .sorted().toList();
        } catch (IOException e) {
            return java.util.List.of();
        }
    }

    public static void save() {
        try {
            writeAtomically(file(), GSON.toJson(writeRoot()));
        } catch (IOException e) {
            QuietPlease.LOGGER.error("{} yazilamadi", file(), e);
        }
    }

    /**
     * Once yanindaki gecici dosyaya yazar, sonra tek adimda yerine tasir. Oyun yazarken cokerse ya da
     * bilgisayar kapanirsa eski dosya bozulmadan kalir.
     */
    private static void writeAtomically(Path target, String content) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Okunamayan ayar dosyasini quietplease.json.broken olarak saklar; yoksa bir sonraki kayit onu
     * varsayilanlarla ezer ve oyuncunun ayarlari tamamen kaybolur.
     */
    private static void backupBroken(Path path) {
        Path backup = path.resolveSibling(path.getFileName() + ".broken");
        try {
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            QuietPlease.LOGGER.warn("Bozuk ayar dosyasinin kopyasi: {}", backup);
        } catch (IOException e) {
            QuietPlease.LOGGER.error("Bozuk ayar dosyasi yedeklenemedi", e);
        }
    }
}
