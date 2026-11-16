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
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.Music;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;

/**
 * Oyuncunun kendi muzikleri: config/quietplease/music klasorundeki .ogg dosyalari.
 *
 * Kaynak paketi gerekmez. Her dosya "quietplease_music:dosya_adi" kimligiyle panelde "Kendi muzigin" grubunda gorunur,
 * calma listesine eklenebilir ve (ayar aciksa) oyunun arka plan muzigine karisir. Dosyalar oyunun kaynak yoneticisi
 * uzerinden okunur (ReloadableResourceManagerMixin), boylece ses motoru, sure ve ses dengeleme hesabi onlari
 * diger muzikler gibi acar.
 */
public final class CustomMusic {

    public static final String NAMESPACE = "quietplease_music";
    /** Ozel muzik calarken kullanilan ses olayi; dosya zaten sabit oldugu icin yalnizca bulunabilir bir olay gerekli. */
    public static final ResourceLocation EVENT = new ResourceLocation("minecraft", "music.game");
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
                List<Path> oggs = walk.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
                        .sorted(Comparator.comparing(p -> p.toString().toLowerCase(Locale.ROOT)))
                        .limit(MAX_FILES)
                        .toList();
                for (Path p : oggs) {
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

    @Nullable
    private static ResourceLocation id(String path) {
        return ResourceLocation.tryBuild(NAMESPACE, path);
    }

    /** "Benim Sarkim (v2).ogg" -> "benim_sarkim_v2"; alt klasorler "/" ile kalir. */
    static String slug(String relative) {
        String s = relative.replace('\\', '/');
        if (s.toLowerCase(Locale.ROOT).endsWith(".ogg")) s = s.substring(0, s.length() - 4);
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
        return new Sound(id.toString(), ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE, true, false, 16);
    }

    /**
     * Kaynak yoneticisi bu ad alanindan bir dosya isteyince (mixin): "sounds/&lt;ad&gt;.ogg" -> klasordeki dosya.
     */
    public static Optional<Resource> resource(ResourceLocation path, ReloadableResourceManager manager) {
        String p = path.getPath();
        if (!p.startsWith("sounds/") || !p.endsWith(".ogg")) return Optional.empty();
        ResourceLocation id = id(p.substring(7, p.length() - 4));
        Path file = id == null ? null : files.get(id);
        if (file == null || !Files.isRegularFile(file)) return Optional.empty();
        return Optional.of(new Resource(NAMESPACE, () -> Files.newInputStream(file)));
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
        WeighedSoundEvents event = Minecraft.getInstance().getSoundManager().getSoundEvent(music.getEvent().getLocation());
        if (event == null) return 0;
        int n = 0;
        for (var w : ((com.quietplease.mixin.WeighedSoundEventsAccessor) event).quietplease$getList()) {
            if (w instanceof Sound s && !MusicSettings.isDisabled(s.getLocation())) n++;
            else if (!(w instanceof Sound)) n++;
        }
        return n;
    }
}
