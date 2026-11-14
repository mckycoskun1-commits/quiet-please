package com.quietplease.music;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.quietplease.QuietPlease;
import com.quietplease.mixin.MusicManagerAccessor;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Arka plan calma listesi: oyuncunun sectigi parcalar, sirasiyla.
 *
 * Calma listesi modu aciksa, oyun bir ARKA PLAN muzigi baslatacagi zaman (bkz. {@link ModOptions#isBackground})
 * rastgele parca yerine listedeki siradaki calinabilir parca calar. Calan parca listede yerinde durur; bitince
 * listeden cikar, "dongu" isaretli parca ise yerinde kalir. Sonra ardindaki parca calar, liste bitince basa doner.
 * Ejderha/jenerik muzigi ve modlarin dogrudan caldigi sesler (cogu boss muzigi) hic etkilenmez. Liste bossa ya da
 * mod kapaliysa oyun eskisi gibi davranir.
 *
 * config/quietplease.json icindeki "playlist" bolumunde saklanir:
 * <pre>
 * "playlist": { "tracks": [...], "loop": [...], "history": [...], "favorites": [...], "next": "..." }
 * </pre>
 */
public final class Playlist {

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
        public WeighedSoundEvents resolve(SoundManager manager) {
            this.sound = target;
            return manager.getSoundEvent(this.identifier);
        }
    }

    private static final List<Identifier> QUEUE = new ArrayList<>();
    /** Dongu isaretli parcalar: calinca silinmez, yerinde kalir. */
    private static final Set<Identifier> LOOP = new HashSet<>();
    /** Oyuncunun daha once listeye ekledigi parcalar, en yenisi basta (oneriler icin). */
    private static final List<Identifier> HISTORY = new ArrayList<>();
    private static final int HISTORY_MAX = 64;
    /** Yildizli parcalar, eklenme sirasiyla. Calinca silinmez; ana panelde en ustte "Favoriler" grubu. */
    private static final Set<Identifier> FAVORITES = new LinkedHashSet<>();
    /** Baslatilan ve henuz bitmemis parca; bitince listeden cikar (donguyse yerinde kalir). */
    @Nullable
    private static Identifier currentId;
    /** Hicbir parca calmiyorken siradaki aramasi bu parcadan baslar; null = listenin basi. */
    @Nullable
    private static Identifier nextFrom;
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

    public static boolean isLooping(Identifier id) {
        return LOOP.contains(id);
    }

    public static boolean isFavorite(Identifier id) {
        return FAVORITES.contains(id);
    }

    public static List<Identifier> favorites() {
        return List.copyOf(FAVORITES);
    }

    /** Ana paneldeki ☆/★ dugmesi: favorilere ve siraya ekler ya da ikisinden de cikarir. @return artik favori mi */
    public static boolean toggleFavorite(Identifier id) {
        if (FAVORITES.remove(id)) {
            remove(id);
            changed();
            return false;
        }
        FAVORITES.add(id);
        add(id);
        changed();
        return true;
    }

    /** Daha once listeye eklenmis parcalar, en yenisi basta. */
    public static List<Identifier> history() {
        return Collections.unmodifiableList(HISTORY);
    }

    /** Calan (bitince listeden cikacak) parca; yoksa null. */
    @Nullable
    public static Identifier currentTrack() {
        return currentId;
    }

    public static int version() {
        return version;
    }

    /** Calma listesi su an oyunun arka plan muzigini yonetiyor mu? */
    public static boolean isActive() {
        return ModOptions.playlistMode && !QUEUE.isEmpty();
    }

    /**
     * Sirada calinacak parcalar, calinma sirasiyla (calan parca haric) - listedeki gercek sira numaralari.
     * Calan parcanin ardindan baslar, listenin sonuna gelince basa doner.
     */
    public static List<Integer> upcoming() {
        int n = QUEUE.size();
        int playing = currentId == null ? -1 : QUEUE.indexOf(currentId);
        int start = playing >= 0 ? playing + 1 : startIndex();
        List<Integer> order = new ArrayList<>(n);
        for (int k = 0; k < n; k++) {
            int i = (start + k) % n;
            if (i != playing) order.add(i);
        }
        return order;
    }

    /** Hicbir parca calmiyorken siradaki aramasinin baslayacagi sira numarasi. */
    private static int startIndex() {
        return nextFrom == null ? 0 : Math.max(0, QUEUE.indexOf(nextFrom));
    }

    // ---------------------------------------------------------------- degistirme

    public static void add(Identifier id) {
        if (QUEUE.contains(id)) return;
        QUEUE.add(id);
        HISTORY.remove(id);
        HISTORY.add(0, id);
        while (HISTORY.size() > HISTORY_MAX) HISTORY.remove(HISTORY.size() - 1);
        changed();
    }

    public static void remove(Identifier id) {
        int i = QUEUE.indexOf(id);
        if (i >= 0) removeAt(i);
    }

    /** "Sarki ekle" sekmesindeki dugme. @return artik listede mi */
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
        Identifier id = QUEUE.remove(index);
        LOOP.remove(id);
        if (id.equals(currentId) || id.equals(nextFrom)) {
            // calan (ya da siradaki) parca cikarilinca sira, ardindaki parcadan devam eder
            if (id.equals(currentId)) currentId = null;
            nextFrom = QUEUE.isEmpty() ? null : QUEUE.get(index % QUEUE.size());
        }
        changed();
    }

    /** Dongu dugmesi. @return artik dongude mi */
    public static boolean toggleLoop(Identifier id) {
        if (!QUEUE.contains(id)) return false;
        boolean on = !LOOP.remove(id);
        if (on) LOOP.add(id);
        changed();
        return on;
    }

    /** Iki sirayi yer degistirir (▲/▼). */
    public static void swap(int a, int b) {
        if (a < 0 || b < 0 || a >= QUEUE.size() || b >= QUEUE.size() || a == b) return;
        Collections.swap(QUEUE, a, b);
        changed();
    }

    /** Karistirir. Calan parca calmaya devam eder; calmiyorsa yeni sira bastan baslar. */
    public static void shuffle() {
        Collections.shuffle(QUEUE, new java.util.Random(RANDOM.nextLong()));
        if (currentId == null) nextFrom = null;
        changed();
    }

    public static void clear() {
        QUEUE.clear();
        LOOP.clear();
        currentId = null;
        nextFrom = null;
        changed();
    }

    /**
     * Siradaki parca bu olsun; liste sirasi degismez. Calan parca biter (silinir ya da donguyse yerinde kalir),
     * secilen parca calinca sira onun ardindan devam eder. Calan parcanin kendisiyse bastan tekrar calar.
     */
    public static void setNext(int index) {
        if (index < 0 || index >= QUEUE.size()) return;
        Identifier id = QUEUE.get(index);
        if (id.equals(currentId)) currentId = null;
        else finishCurrent();
        nextFrom = id;
        changed();
    }

    private static void changed() {
        version++;
    }

    // ---------------------------------------------------------------- secim

    /**
     * Calan parca bitti: listeden cikar, donguyse yerinde kalir. Siradaki aramasi ardindaki parcadan baslar.
     *
     * @return bir parca bitirildi mi
     */
    static boolean finishCurrent() {
        Identifier done = currentId;
        if (done == null) return false;
        currentId = null;
        int i = QUEUE.indexOf(done);
        if (i >= 0) {
            if (LOOP.contains(done)) {
                i++;
            } else {
                QUEUE.remove(i);
                LOOP.remove(done);
            }
            nextFrom = QUEUE.isEmpty() ? null : QUEUE.get(i % QUEUE.size());
        }
        version++;
        return true;
    }

    /**
     * Calan parcayi bitirir, sonra siradaki ilk calinabilir parcayi secer (ardindan baslayip basa donerek).
     * Calinamayanlari (kapali, yuklu degil) atlar ama listede birakir.
     *
     * @return sira numarasi; -1 = calinacak parca yok (oyunun kendi muzigi calsin)
     */
    static int advance(Predicate<Identifier> playable) {
        finishCurrent();
        int n = QUEUE.size();
        int start = startIndex();
        for (int k = 0; k < n; k++) {
            int i = (start + k) % n;
            if (playable.test(QUEUE.get(i))) {
                currentId = QUEUE.get(i);
                nextFrom = currentId; // oyun kapanirsa bir dahaki sefere bu parcadan devam
                version++;
                return i;
            }
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
        return (ModOptions.playlistMode || oneShot) && !QUEUE.isEmpty() && ModOptions.isBackground(music);
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

    /** Her istemci tick'inde: oyun duraklatilmamissa gecen sureyi say; parca bittiyse listeden cikar. */
    public static void clientTick(Minecraft mc) {
        if (current == null) return;
        boolean active = mc.getSoundManager().isActive(current);
        if (active && !mc.isPaused()) elapsedTicks++;
        if (!active && current.trackId.equals(currentId) && finishCurrent()) MusicSettings.save();
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
        currentId = null;
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
        LOOP.clear();
        HISTORY.clear();
        FAVORITES.clear();
        currentId = null;
        nextFrom = null;
        if (o != null) {
            List<Identifier> favorites = new ArrayList<>();
            // eski dosyalarda favori yok: yildiz o zamanlar siraya ekliyordu, siradakiler favori sayilir
            readIds(o, o.has("favorites") ? "favorites" : "tracks", favorites);
            FAVORITES.addAll(favorites);
            readIds(o, "tracks", QUEUE);
            List<Identifier> loop = new ArrayList<>();
            readIds(o, "loop", loop);
            loop.stream().filter(QUEUE::contains).forEach(LOOP::add);
            readIds(o, "history", HISTORY);
            for (Identifier id : QUEUE) if (!HISTORY.contains(id)) HISTORY.add(id);
            while (HISTORY.size() > HISTORY_MAX) HISTORY.remove(HISTORY.size() - 1);
            if (o.has("next") && o.get("next").isJsonPrimitive()) {
                Identifier next = Identifier.tryParse(o.get("next").getAsString());
                if (next != null && QUEUE.contains(next)) nextFrom = next;
            }
        }
        version++;
    }

    private static void readIds(JsonObject o, String key, List<Identifier> into) {
        if (!o.has(key) || !o.get(key).isJsonArray()) return;
        for (JsonElement e : o.getAsJsonArray(key)) {
            Identifier id = Identifier.tryParse(e.getAsString());
            if (id != null && !into.contains(id)) into.add(id);
        }
    }

    private static JsonArray ids(List<Identifier> list) {
        JsonArray a = new JsonArray();
        list.forEach(id -> a.add(id.toString()));
        return a;
    }

    static JsonObject write() {
        JsonObject o = new JsonObject();
        o.add("tracks", ids(QUEUE));
        o.add("loop", ids(QUEUE.stream().filter(LOOP::contains).toList()));
        o.add("history", ids(HISTORY));
        o.add("favorites", ids(List.copyOf(FAVORITES)));
        Identifier next = currentId != null ? currentId : nextFrom;
        if (next != null && QUEUE.contains(next)) o.addProperty("next", next.toString());
        return o;
    }
}
