package com.quietplease.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.quietplease.Lang;
import com.quietplease.QuietPlease;
import com.quietplease.mixin.MusicManagerAccessor;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Arka plan calma listesi: oyuncunun sectigi parcalar, sirasiyla.
 *
 * Calma listesi modu aciksa, oyun bir ARKA PLAN muzigi baslatacagi zaman (bkz. {@link ModOptions#isBackground})
 * rastgele parca yerine listedeki siradaki parca calar. Ejderha/jenerik muzigi ve modlarin dogrudan caldigi
 * sesler (cogu boss muzigi) hic etkilenmez. Liste bossa ya da mod kapaliysa oyun eskisi gibi davranir.
 *
 * config/quietplease.json icindeki "playlist" bolumunde saklanir:
 * <pre>
 * "playlist": { "tracks": ["minecraft:music/game/calm1", ...], "position": 0, "repeat": "repeat_list" }
 * </pre>
 */
public final class Playlist {

    /** Liste bitince ne olsun. */
    public enum Repeat {
        /** Bastan, ayni sirayla. */
        REPEAT_LIST("repeat_list"),
        /** Liste karistirilir ve bastan (sonsuza kadar). */
        SHUFFLE("shuffle"),
        /** Durur, oyunun kendi muzigine donulur. */
        STOP("stop");

        public final String id;

        Repeat(String id) {
            this.id = id;
        }

        public Component displayName() {
            return Lang.tr("playlist.repeat." + id);
        }

        static Repeat byId(String id) {
            for (Repeat r : values()) if (r.id.equals(id)) return r;
            return REPEAT_LIST;
        }
    }

    /** Listedeki bir parcayi caldiran ses: rastgele secim yapmadan dogrudan o dosya (onizlemedeki yontem). */
    public static final class PlaylistSound extends AbstractSoundInstance {
        private final Sound target;
        public final Identifier trackId;

        PlaylistSound(Identifier eventId, Sound target, Identifier trackId) {
            // SimpleSoundInstance.forMusic ile ayni ozellikler
            super(eventId, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.target = target;
            this.trackId = trackId;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.relative = true;
        }

        @Override
        public WeighedSoundEvents getOrResolve(SoundManager manager) {
            this.sound = target;
            return manager.getSoundEvent(this.identifier);
        }
    }

    private static final List<Identifier> QUEUE = new ArrayList<>();
    /** Calan (ya da en son calan) parcanin sirasi; -1 = henuz baslamadi. */
    private static int position = -1;
    private static Repeat repeat = Repeat.REPEAT_LIST;
    /** "Sonda dur" modunda liste bitti: oyunun kendi muzigine donuldu (liste degisene kadar). */
    private static boolean ended = false;
    /** Bir sonraki baslatmada calma listesi modu kapali olsa bile listeden cal (▶ dugmesi). */
    private static boolean oneShot = false;
    /** Her degisiklikte artar; panel listesini ne zaman yenileyecegini buradan anlar. */
    private static int version = 0;
    private static final RandomSource RANDOM = RandomSource.create();

    @Nullable
    private static PlaylistSound current;
    private static int elapsedTicks = 0;
    /** Ses dosyasi -> sure (sn); -1 = okunamadi. */
    private static final Map<Identifier, Integer> DURATIONS = new ConcurrentHashMap<>();

    private Playlist() {}

    // ---------------------------------------------------------------- okuma

    public static List<Identifier> tracks() {
        return Collections.unmodifiableList(QUEUE);
    }

    public static int size() {
        return QUEUE.size();
    }

    public static boolean isEmpty() {
        return QUEUE.isEmpty();
    }

    public static boolean contains(Identifier id) {
        return QUEUE.contains(id);
    }

    public static int position() {
        return position;
    }

    public static Repeat repeat() {
        return repeat;
    }

    public static boolean hasEnded() {
        return ended;
    }

    public static int version() {
        return version;
    }

    /** Calma listesi su an oyunun arka plan muzigini yonetiyor mu? */
    public static boolean isActive() {
        return ModOptions.playlistMode && !QUEUE.isEmpty() && !ended;
    }

    /**
     * Sirada calinacak parcalar, calinma sirasiyla (calan parca haric) - listedeki gercek sira numaralari.
     * Liste sonuna gelince basa sarar; {@link #isNextRound} sarmadan sonrakileri gosterir.
     */
    public static List<Integer> upcoming() {
        int n = QUEUE.size();
        List<Integer> order = new ArrayList<>(n);
        int start = position < 0 || position >= n ? -1 : position;
        int count = start < 0 ? n : n - 1;
        for (int k = 1; k <= count; k++) order.add(Math.floorMod(start + k, n));
        return order;
    }

    /** Bu sira, listenin bir sonraki turunda mi calacak (basa sarmadan sonra)? */
    public static boolean isNextRound(int index) {
        return position >= 0 && index < position;
    }

    // ---------------------------------------------------------------- degistirme

    public static void add(Identifier id) {
        if (QUEUE.contains(id)) return;
        QUEUE.add(id);
        changed();
    }

    public static void remove(Identifier id) {
        int i = QUEUE.indexOf(id);
        if (i >= 0) removeAt(i);
    }

    /** Ana paneldeki ☆/★ dugmesi. @return artik listede mi */
    public static boolean toggle(Identifier id) {
        if (QUEUE.contains(id)) {
            remove(id);
            return false;
        }
        add(id);
        return true;
    }

    public static void removeAt(int index) {
        if (index < 0 || index >= QUEUE.size()) return;
        QUEUE.remove(index);
        // Calandan oncekiler kayarsa ya da calan silinirse imleci geri al: siradaki yine dogru parca olsun
        if (index <= position) position--;
        if (QUEUE.isEmpty()) position = -1;
        changed();
    }

    /** Iki sirayi yer degistirir (▲/▼). */
    public static void swap(int a, int b) {
        if (a < 0 || b < 0 || a >= QUEUE.size() || b >= QUEUE.size() || a == b) return;
        Collections.swap(QUEUE, a, b);
        if (position == a) position = b;
        else if (position == b) position = a;
        changed();
    }

    /** Karistir: calan parca basta kalir, gerisi karisir. */
    public static void shuffle() {
        if (position >= 0 && position < QUEUE.size()) {
            Identifier playing = QUEUE.remove(position);
            Collections.shuffle(QUEUE, new java.util.Random(RANDOM.nextLong()));
            QUEUE.add(0, playing);
            position = 0;
        } else {
            Collections.shuffle(QUEUE, new java.util.Random(RANDOM.nextLong()));
            position = -1;
        }
        changed();
    }

    public static void clear() {
        QUEUE.clear();
        position = -1;
        changed();
    }

    public static void setRepeat(Repeat value) {
        repeat = value;
        changed();
    }

    /** Siradaki parca bu olsun. */
    public static void setNext(int index) {
        if (index < 0 || index >= QUEUE.size()) return;
        position = index - 1;
        changed();
    }

    private static void changed() {
        ended = false;
        version++;
    }

    // ---------------------------------------------------------------- secim

    /**
     * Siradaki calinabilir parcanin sirasini secer ve imleci ilerletir. Calinamayanlari (kapali, yuklu degil) atlar.
     * Liste bitince tekrar moduna gore basa sarar, karistirir ya da durur.
     *
     * @return sira numarasi; -1 = calinacak parca yok (oyunun kendi muzigi calsin)
     */
    static int advance(Predicate<Identifier> playable) {
        int n = QUEUE.size();
        if (n == 0 || ended) return -1;
        Identifier last = position >= 0 && position < n ? QUEUE.get(position) : null;
        for (int step = 0; step < n; step++) {
            int next = position + 1;
            if (next >= n) {
                switch (repeat) {
                    case STOP -> {
                        ended = true;
                        position = n - 1;
                        version++;
                        return -1;
                    }
                    case SHUFFLE -> {
                        Collections.shuffle(QUEUE, new java.util.Random(RANDOM.nextLong()));
                        // ayni parca arka arkaya gelmesin
                        if (n > 1 && QUEUE.get(0).equals(last)) Collections.swap(QUEUE, 0, n - 1);
                    }
                    case REPEAT_LIST -> {}
                }
                next = 0;
            }
            position = next;
            version++;
            if (playable.test(QUEUE.get(next))) return next;
        }
        return -1;
    }

    /** Bir parcanin dosyasini iceren ses olayini ve dosyayi bulur. */
    @Nullable
    private static PlaylistSound createSound(Identifier trackId) {
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        PlaylistSound fallback = null;
        for (Identifier eventId : sm.getAvailableSounds()) {
            WeighedSoundEvents event = sm.getSoundEvent(eventId);
            if (event == null) continue;
            for (Weighted<Sound> w : ((com.quietplease.mixin.WeighedSoundEventsAccessor) event).quietplease$getList()) {
                if (w instanceof Sound s && s.getLocation().equals(trackId)) {
                    PlaylistSound sound = new PlaylistSound(eventId, s, trackId);
                    if (eventId.getPath().contains("music")) return sound; // muzik olayini tercih et
                    if (fallback == null) fallback = sound;
                }
            }
        }
        return fallback;
    }

    /**
     * MusicManager.startPlaying(Music) basinda cagrilir (mixin). Calma listesi bu muzigi devralacaksa
     * calinacak sesi dondurur; null = oyun her zamanki gibi kendi sectigini calsin.
     */
    @Nullable
    public static SoundInstance startInstead(Music music) {
        boolean force = oneShot;
        oneShot = false;
        if ((!ModOptions.playlistMode && !force) || QUEUE.isEmpty() || !ModOptions.isBackground(music)) return null;
        PlaylistSound[] found = new PlaylistSound[1];
        int index = advance(id -> {
            if (MusicSettings.isDisabled(id)) return false;
            found[0] = createSound(id);
            return found[0] != null;
        });
        if (index < 0) return null;
        current = found[0];
        elapsedTicks = 0;
        durationSeconds(current); // sureyi arka planda okumaya basla
        MusicSettings.save(); // sira yeniden baslatinca da korunsun
        QuietPlease.LOGGER.debug("Calma listesi: {}. parca {}", index + 1, current.trackId);
        return current;
    }

    /** Onizleme calarken liste ilerlemesin diye mixin bunu sorar. */
    public static boolean wouldTakeOver(Music music) {
        return (ModOptions.playlistMode || oneShot) && !QUEUE.isEmpty() && !ended && ModOptions.isBackground(music);
    }

    /** ▶: bu parcayi hemen cal (calma listesi modu kapali olsa bile, bir kerelik). */
    public static void playNow(int index) {
        if (index < 0 || index >= QUEUE.size()) return;
        setNext(index);
        oneShot = true;
        restartMusic(0);
    }

    /**
     * Calan arka plan muzigini durdurur ve yenisini baslatir. Arka plan muzigi calinabilecek bir durumdaysa
     * hemen baslatir (oyun duraklatilmisken MusicManager tick'lenmedigi icin dogrudan); degilse
     * ornegin ejderha muzigi caliyorsa) sadece bekleme suresini ayarlar.
     *
     * @param delayTicks hemen baslatilamazsa sonraki sarkiya kadar bekleme
     */
    public static void restartMusic(int delayTicks) {
        Minecraft mc = Minecraft.getInstance();
        MusicManager mm = mc.getMusicManager();
        mm.stopPlaying();
        Music music = com.quietplease.platform.Services.PLATFORM.selectMusic(mc.getSituationalMusic());
        if (delayTicks <= 0 && music != null && ModOptions.isBackground(music)) {
            mm.startPlaying(ModOptions.adjustGap(music));
        } else {
            ((MusicManagerAccessor) mm).quietplease$setNextSongDelay(Math.max(0, delayTicks));
        }
    }

    // ---------------------------------------------------------------- calan parca, sure

    /** Calma listesinden calan parca (calmiyorsa null). */
    @Nullable
    public static PlaylistSound playing() {
        if (current == null) return null;
        return Minecraft.getInstance().getSoundManager().isActive(current) ? current : null;
    }

    /** Her istemci tick'inde: oyun duraklatilmamissa gecen sureyi say. */
    public static void clientTick(Minecraft mc) {
        if (current != null && !mc.isPaused() && mc.getSoundManager().isActive(current)) elapsedTicks++;
    }

    public static int elapsedSeconds() {
        return elapsedTicks / 20;
    }

    /** Parcanin suresi (sn). Ilk soruldugunda arka planda okunur; o zamana kadar ya da okunamazsa -1. */
    public static int durationSeconds(PlaylistSound sound) {
        Sound s = sound.getSound();
        if (s == null) return -1;
        Identifier path = s.getPath();
        Integer known = DURATIONS.get(path);
        if (known != null) return known;
        DURATIONS.put(path, -1);
        CompletableFuture.runAsync(() -> DURATIONS.put(path, readOggSeconds(path)), Util.backgroundExecutor());
        return -1;
    }

    /**
     * Ogg Vorbis dosyasinin suresi: son Ogg sayfasinin "granul konumu" (toplam ornek sayisi) / ornekleme hizi.
     * Ornekleme hizi ilk sayfadaki Vorbis tanitim basligindadir.
     */
    static int readOggSeconds(Identifier path) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(path)) {
            byte[] b = in.readAllBytes();
            long rate = 0;
            for (int i = 0; i + 16 < b.length && i < 8192; i++) {
                if (b[i] == 1 && b[i + 1] == 'v' && b[i + 2] == 'o' && b[i + 3] == 'r' && b[i + 4] == 'b'
                        && b[i + 5] == 'i' && b[i + 6] == 's') {
                    rate = le(b, i + 12, 4);
                    break;
                }
            }
            for (int i = b.length - 14; i >= 0; i--) {
                if (b[i] == 'O' && b[i + 1] == 'g' && b[i + 2] == 'g' && b[i + 3] == 'S') {
                    long granule = le(b, i + 6, 8);
                    return rate > 0 && granule > 0 ? (int) (granule / rate) : -1;
                }
            }
        } catch (Exception e) {
            QuietPlease.LOGGER.debug("Sure okunamadi: {}", path, e);
        }
        return -1;
    }

    private static long le(byte[] b, int offset, int bytes) {
        long v = 0;
        for (int i = bytes - 1; i >= 0; i--) v = (v << 8) | (b[offset + i] & 0xFF);
        return v;
    }

    // ---------------------------------------------------------------- gelistirme testi

    /** Oz-test icin: listenin o anki hali (dosyadaki bicimde). */
    public static JsonObject snapshot() {
        return write();
    }

    /** Oz-test icin: {@link #snapshot} ile alinan hali geri yukler. */
    public static void restore(JsonObject o) {
        read(o);
        current = null;
        oneShot = false;
        elapsedTicks = 0;
    }

    /** Oz-test icin {@link #advance} (ses calmadan). */
    public static int advanceForTest(Predicate<Identifier> playable) {
        return advance(playable);
    }

    // ---------------------------------------------------------------- dosya (MusicSettings okur/yazar)

    static void read(@Nullable JsonObject o) {
        QUEUE.clear();
        position = -1;
        repeat = Repeat.REPEAT_LIST;
        ended = false;
        if (o != null) {
            if (o.has("tracks")) {
                for (JsonElement e : o.getAsJsonArray("tracks")) {
                    Identifier id = Identifier.tryParse(e.getAsString());
                    if (id != null && !QUEUE.contains(id)) QUEUE.add(id);
                }
            }
            if (o.has("repeat")) repeat = Repeat.byId(o.get("repeat").getAsString());
            if (o.has("position")) position = Math.max(-1, Math.min(QUEUE.size() - 1, o.get("position").getAsInt()));
        }
        version++;
    }

    static JsonObject write() {
        JsonObject o = new JsonObject();
        JsonArray tracks = new JsonArray();
        QUEUE.forEach(id -> tracks.add(id.toString()));
        o.add("tracks", tracks);
        o.addProperty("position", position);
        o.addProperty("repeat", repeat.id);
        return o;
    }
}
