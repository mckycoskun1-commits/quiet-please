package com.quietplease.music;

import com.quietplease.Lang;
import com.quietplease.QuietPlease;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.sounds.Music;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;

/**
 * Oyuncunun kendi muzikleri: config/quietplease/music klasorundeki .ogg ve .mp3 dosyalari (MP3: {@link Mp3}).
 *
 * Kaynak paketi gerekmez. Her dosya "quietplease_music:dosya_adi" kimligiyle panelde "Kendi muzigin" grubunda gorunur,
 * calma listesine eklenebilir ve (ayar aciksa) oyunun arka plan muzigine karisir. Sure ve ses dengeleme hesabi dosyalari
 * oyunun kaynak yoneticisinden (ReloadableResourceManagerMixin), ses motoru ise kendi dosya saglayicisindan
 * (SoundBufferLibraryMixin) okur.
 */
public final class CustomMusic {

    public static final String NAMESPACE = "quietplease_music";
    /** Ozel muzik calarken kullanilan ses olayi; dosya zaten sabit oldugu icin yalnizca bulunabilir bir olay gerekli. */
    public static final ResourceLocation EVENT = ResourceLocation.withDefaultNamespace("music.game");
    private static final int MAX_FILES = 500;

    /** Kimlik -> dosya. Panel her acildiginda yenilenir. */
    private static volatile Map<ResourceLocation, Path> files = Map.of();
    private static final RandomSource RANDOM = RandomSource.create();

    private CustomMusic() {}

    public static Path folder() {
        return com.quietplease.platform.Services.PLATFORM.configDir().resolve(QuietPlease.MODID).resolve("music");
    }

    /** Klasoru tarar (yoksa olusturur). */
    public static void refresh() {
        Path dir = folder();
        Map<ResourceLocation, Path> found = new LinkedHashMap<>();
        try {
            Files.createDirectories(dir);
            try (Stream<Path> walk = Files.walk(dir, 3)) {
                List<Path> songs = walk.filter(Files::isRegularFile)
                        .filter(p -> isSongFile(p.getFileName().toString()))
                        .sorted(Comparator.comparing(p -> p.toString().toLowerCase(Locale.ROOT)))
                        .limit(MAX_FILES)
                        .toList();
                for (Path p : songs) {
                    String base = slug(dir.relativize(p).toString());
                    String name = base;
                    for (int i = 2; found.containsKey(id(name)); i++) name = base + "_" + i;
                    ResourceLocation id = id(name);
                    if (id != null) found.put(id, p);
                }
            }
        } catch (IOException | RuntimeException e) {
            QuietPlease.LOGGER.warn("Kendi muzik klasoru okunamadi: {}", dir, e);
        }
        files = Map.copyOf(found);
    }

    /** Kendi muzigindeki bir parcanin dosya adi (uzantisiz, oldugu gibi); baska bir parcaysa null. */
    @Nullable
    public static String fileTitle(ResourceLocation id) {
        if (!isCustom(id)) return null;
        Path file = files.get(id);
        if (file == null) return null;
        String name = file.getFileName().toString();
        return isSongFile(name) ? name.substring(0, name.length() - 4) : name;
    }

    static boolean isSongFile(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(".ogg") || n.endsWith(".mp3");
    }

    /**
     * Bu kimligin (ya da ses motorunun istedigi "sounds/&lt;ad&gt;.ogg" yolunun) MP3 dosyasi; OGG ya da baska bir
     * ad alaniysa null.
     */
    @Nullable
    public static Path mp3File(ResourceLocation idOrPath) {
        if (!isCustom(idOrPath)) return null;
        String p = idOrPath.getPath();
        if (p.startsWith("sounds/") && p.endsWith(".ogg")) p = p.substring(7, p.length() - 4);
        ResourceLocation id = id(p);
        Path file = id == null ? null : files.get(id);
        return file != null && Mp3.isMp3(file) ? file : null;
    }

    /** Ses motoru bir MP3 akisi isteyince (SoundBufferLibraryMixin); MP3 degilse null, oyun her zamanki gibi acar. */
    @Nullable
    public static java.util.concurrent.CompletableFuture<net.minecraft.client.sounds.AudioStream> mp3Stream(ResourceLocation path) {
        Path file = mp3File(path);
        if (file == null) return null;
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return Mp3.stream(file);
            } catch (IOException e) {
                QuietPlease.LOGGER.warn("MP3 acilamadi: {}", file, e);
                throw new java.util.concurrent.CompletionException(e);
            }
        });
    }

    @Nullable
    private static ResourceLocation id(String path) {
        return ResourceLocation.tryBuild(NAMESPACE, path);
    }

    /** "Benim Sarkim (v2).ogg" (ya da .mp3) -> "benim_sarkim_v2"; alt klasorler "/" ile kalir. */
    static String slug(String relative) {
        String s = relative.replace('\\', '/');
        if (isSongFile(s)) s = s.substring(0, s.length() - 4);
        StringBuilder out = new StringBuilder();
        for (char c : s.toLowerCase(Locale.ROOT).toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '.' || c == '-') out.append(c);
            else if (out.length() > 0 && out.charAt(out.length() - 1) != '_') out.append('_');
        }
        String r = out.toString().replaceAll("_+/", "/").replaceAll("/_+", "/").replaceAll("^_+|_+$", "");
        return r.isEmpty() ? "song" : r;
    }

    public static String slugForTest(String relative) {
        return slug(relative);
    }

    public static boolean isCustom(ResourceLocation id) {
        return NAMESPACE.equals(id.getNamespace());
    }

    public static boolean exists(ResourceLocation id) {
        return files.containsKey(id);
    }

    public static List<MusicTrack> tracks() {
        List<MusicTrack> out = new ArrayList<>();
        for (ResourceLocation id : files.keySet()) out.add(new MusicTrack(id, Set.of(EVENT), MusicTrack.Kind.MUSIC));
        out.sort(Comparator.comparing(t -> t.id().getPath()));
        return out;
    }

    public static String groupName() {
        return Lang.tr("custom.group").getString();
    }

    /** Dosyayi calan ses tanimi (akisli, ses 1, perde 1). */
    public static Sound sound(ResourceLocation id) {
        return new Sound(id, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, true, false, 16);
    }

    /**
     * Kaynak yoneticisi bu ad alanindan bir dosya isteyince (mixin): "sounds/&lt;ad&gt;.ogg" -> klasordeki dosya.
     */
    public static Optional<Resource> resource(ResourceLocation path, ResourceManager manager) {
        String p = path.getPath();
        if (!p.startsWith("sounds/") || !p.endsWith(".ogg")) return Optional.empty();
        ResourceLocation id = id(p.substring(7, p.length() - 4));
        Path file = id == null ? null : files.get(id);
        if (file == null || !Files.isRegularFile(file)) return Optional.empty();
        Optional<PackResources> pack = manager.listPacks().findFirst();
        if (pack.isEmpty()) return Optional.empty();
        return Optional.of(new Resource(pack.get(), () -> Files.newInputStream(file)));
    }

    /**
     * Ses motorunun dosya saglayicisi (SoundBufferLibraryMixin). Oyun sesleri, kaynak paketlerinden yuklemede bir kez
     * toplanan bir haritadan okur; bu klasorun dosyalari o haritada olmadigi icin hic ses cikmazdi. Bu ad alani
     * klasorden, gerisi her zamanki gibi oyunun saglayicisindan gelir.
     */
    public static ResourceProvider provider(ResourceProvider original) {
        return path -> isCustom(path) ? resource(path, Minecraft.getInstance().getResourceManager()) : original.getResource(path);
    }

    /**
     * Arka plan muzigi baslayacakken: ayar aciksa ve kendi muzigin varsa, oyunun havuzundaki sarki sayisiyla
     * orantili bir sansla onun yerine kendi muziklerinden biri calar. null = oyun kendi sectigini calsin.
     */
    @Nullable
    public static SoundInstance startInstead(Music music) {
        if (!ModOptions.customInBackground || files.isEmpty() || !ModOptions.isBackground(music)) return null;
        List<ResourceLocation> candidates = new ArrayList<>();
        int weightSum = 0;
        for (ResourceLocation id : files.keySet()) {
            if (MusicSettings.isDisabled(id) || MusicMixer.recentRank(id) == 0) continue;
            candidates.add(id);
        }
        if (candidates.isEmpty()) return null;
        int pool = poolSize(music);
        if (RANDOM.nextInt(candidates.size() + pool) >= candidates.size()) return null;
        int[] weights = new int[candidates.size()];
        for (int i = 0; i < weights.length; i++) {
            ResourceLocation id = candidates.get(i);
            weights[i] = MusicMixer.isRecent(id) && candidates.size() > 1 ? 0 : MusicMixer.weight(id, 1);
            weightSum += weights[i];
        }
        if (weightSum <= 0) {
            java.util.Arrays.fill(weights, 1);
            weightSum = weights.length;
        }
        int r = RANDOM.nextInt(weightSum);
        for (int i = 0; i < weights.length; i++) {
            r -= weights[i];
            if (r < 0) return Playlist.createSoundFor(candidates.get(i));
        }
        return null;
    }

    /** Oyunun bu muzik icin secebilecegi acik dosya sayisi. */
    private static int poolSize(Music music) {
        WeighedSoundEvents event = Minecraft.getInstance().getSoundManager().getSoundEvent(music.getEvent().value().getLocation());
        if (event == null) return 0;
        int n = 0;
        for (var w : ((com.quietplease.mixin.WeighedSoundEventsAccessor) event).quietplease$getList()) {
            if (w instanceof Sound s && !MusicSettings.isDisabled(s.getLocation())) n++;
            else if (!(w instanceof Sound)) n++;
        }
        return n;
    }
}
