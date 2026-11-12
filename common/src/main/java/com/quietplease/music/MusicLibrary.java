package com.quietplease.music;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.ResourceLocation;

/**
 * Oyuna yuklenmis tum seslerin icinden muzikleri bulur ve geldikleri moda gore gruplar.
 */
public final class MusicLibrary {

    private MusicLibrary() {}

    /**
     * Ses kayitlarini tarar. Sonuc: mod kimligi -> o modun parcalari.
     * Minecraft ilk sirada, diger modlar alfabetik.
     */
    public static Map<String, List<MusicTrack>> scan() {
        SoundManager soundManager = Minecraft.getInstance().getSoundManager();

        // Ayni dosya birden fazla olayda kullanilabilir (ornegin calm1 hem music.game hem music.creative'de).
        Map<ResourceLocation, Set<ResourceLocation>> eventsByFile = new HashMap<>();
        Map<ResourceLocation, Boolean> streamByFile = new HashMap<>();

        for (ResourceLocation eventId : soundManager.getAvailableSounds()) {
            WeighedSoundEvents event = soundManager.getSoundEvent(eventId);
            if (event == null) continue;
            for (Weighted<Sound> entry : ((com.quietplease.mixin.WeighedSoundEventsAccessor) event).quietplease$getList()) {
                // Baska bir olaya yapilan referanslari atla; o olay zaten kendi basina taranacak.
                if (!(entry instanceof Sound sound)) continue;
                if (sound.getType() != Sound.Type.FILE) continue;
                ResourceLocation file = sound.getLocation();
                if (file.equals(SoundManager.EMPTY_SOUND_LOCATION)
                        || file.equals(SoundManager.INTENTIONALLY_EMPTY_SOUND_LOCATION)) continue;
                eventsByFile.computeIfAbsent(file, k -> new LinkedHashSet<>()).add(eventId);
                streamByFile.merge(file, sound.shouldStream(), Boolean::logicalOr);
            }
        }

        Map<String, List<MusicTrack>> byMod = new TreeMap<>(MusicLibrary::compareNamespaces);
        eventsByFile.forEach((file, events) -> {
            MusicTrack.Kind kind = classify(file, events, streamByFile.getOrDefault(file, false));
            if (kind != null) {
                byMod.computeIfAbsent(file.getNamespace(), k -> new ArrayList<>())
                        .add(new MusicTrack(file, Set.copyOf(events), kind));
            }
        });

        Map<String, List<MusicTrack>> sorted = new LinkedHashMap<>();
        byMod.forEach((mod, tracks) -> {
            tracks.sort(Comparator.comparing(MusicTrack::kind).thenComparing(t -> t.id().getPath()));
            sorted.put(mod, List.copyOf(tracks));
        });
        return sorted;
    }

    /** Bu dosya muzik mi? Degilse null doner. */
    private static MusicTrack.Kind classify(ResourceLocation file, Set<ResourceLocation> events, boolean stream) {
        boolean disc = file.getPath().contains("records/");
        boolean music = file.getPath().startsWith("music/");
        for (ResourceLocation e : events) {
            String p = e.getPath().toLowerCase(Locale.ROOT);
            // Plak olaylari: "music_disc.cat" (vanilla), "ignis_music_disc" (Cataclysm), "..._record" vb.
            if (p.startsWith("music_disc") || p.endsWith("_disc") || p.endsWith(".disc") || p.contains("record")) disc = true;
            else if (p.contains("music") || p.contains("soundtrack") || p.contains("song")) music = true;
        }
        if (disc) return MusicTrack.Kind.DISC;
        if (music) return MusicTrack.Kind.MUSIC;
        if (stream) return MusicTrack.Kind.OTHER;
        return null;
    }

    /** Mod kimligini okunur ada cevirir: "twilightforest" -> "The Twilight Forest". */
    public static String modName(String namespace) {
        if (namespace.equals("minecraft")) return "Minecraft";
        return com.quietplease.platform.Services.PLATFORM.modName(namespace).orElse(namespace);
    }

    private static int compareNamespaces(String a, String b) {
        if (a.equals(b)) return 0;
        if (a.equals("minecraft")) return -1;
        if (b.equals("minecraft")) return 1;
        return a.compareTo(b);
    }
}
