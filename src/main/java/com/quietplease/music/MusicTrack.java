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
        /** Adi muzik gibi degil ama uzun/akisli calinan bir ses (bazi modlar muzigi boyle kaydeder). */
        OTHER("other");

        /** Ceviri anahtarinin son kismi (quietplease.kind.music) ve dosya ciktisindaki ad. */
        public final String id;

        Kind(String id) {
            this.id = id;
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

    /** Parcanin geldigi mod (ya da kaynak paketi) kimligi. */
    public String namespace() {
        return id.getNamespace();
    }
}
