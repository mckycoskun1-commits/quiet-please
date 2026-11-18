package com.quietplease.music;

import com.quietplease.Lang;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Tek bir muzik parcasi (bir .ogg dosyasi).
 *
 * @param id     dosyanin konumu, ornegin minecraft:music/game/calm1
 *               (ayarlar bu kimlige gore kaydedilecek)
 * @param events bu dosyayi calabilen ses olaylari, ornegin minecraft:music.game
 * @param kind   muzik mi, plak mi, yoksa sadece uzun bir ses mi
 */
public record MusicTrack(ResourceLocation id, Set<ResourceLocation> events, Kind kind) {

    public enum Kind {
        /** Arka plan muzigi (olay adinda "music" geciyor). */
        MUSIC("music"),
        /** Jukebox plagi. */
        DISC("disc"),
        /** Nota blogu enstrumani (minecraft:note/harp...); ses seviyesi ayarlanabilir, calma listesine girmez. */
        NOTE("note"),
        /** Adi muzik gibi degil ama uzun/akisli calinan bir ses (bazi modlar muzigi boyle kaydeder). */
        OTHER("other");

        /** Ceviri anahtarinin son kismi (quietplease.kind.music) ve dosya ciktisindaki ad. */
        public final String id;

        Kind(String id) {
            this.id = id;
        }

        /** Sarki mi (muzik ya da plak)? Nota bloklari ve ortam sesleri degil. */
        public boolean isSong() {
            return this == MUSIC || this == DISC;
        }

        /** Oyunun diline gore ad: "Music" / "Müzik". */
        public Component displayName() {
            return Lang.tr("kind." + id);
        }
    }

    /** Dosya adinin son kismi: "minecraft:music/game/calm1" -> "calm1". */
    public String shortName() {
        String path = id.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /** Ekranda gosterilen ad; bkz. {@link #title(ResourceLocation)}. */
    public String title() {
        return title(id);
    }

    /**
     * Ekranda gosterilen ad: kendi muziginde dosyanin gercek adi (Turkce harfler ve bosluklarla), digerlerinde
     * dosya adinin son kismi. Ikisinde de alt cizgiler bosluk olur: "boss_theme" -> "boss theme".
     */
    public static String title(ResourceLocation id) {
        String name = CustomMusic.fileTitle(id);
        if (name == null) {
            String path = id.getPath();
            name = path.substring(path.lastIndexOf('/') + 1);
        }
        String spaced = name.replace('_', ' ').replaceAll(" {2,}", " ").trim();
        return spaced.isEmpty() ? name : spaced;
    }

    /** Parcanin geldigi mod (ya da kaynak paketi) kimligi. */
    public String namespace() {
        return id.getNamespace();
    }
}
