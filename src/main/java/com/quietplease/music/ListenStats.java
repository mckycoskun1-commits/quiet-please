package com.quietplease.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.quietplease.QuietPlease;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/**
 * Dinleme gecmisi ve istatistikleri: son calan 20 arka plan sarkisi, her sarkinin kac kez caldigi ve en son ne zaman
 * caldigi. config/quietplease-stats.json dosyasinda saklanir (ayarlardan ayri; hazir ayarlara girmez).
 */
public final class ListenStats {

    public static final int HISTORY_MAX = 20;

    public record Entry(Identifier id, long time) {}

    public record Plays(int count, long last) {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Entry> HISTORY = new ArrayList<>();
    private static final Map<Identifier, Plays> PLAYS = new HashMap<>();
    private static boolean dirty;
    private static long lastSave;
    private static int version;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ListenStats::saveNow, "QuietPlease-StatsFlush"));
    }

    private ListenStats() {}

    private static Path file() {
        return com.quietplease.platform.Services.PLATFORM.configDir().resolve(QuietPlease.MODID + "-stats.json");
    }

    /**
     * Bir arka plan sarkisi calmaya basladi.
     *
     * @param intoHistory gecmise eklensin mi (Onceki tusuyla tekrar calinan sarki gecmisteki yerinde kalir)
     */
    public static synchronized void played(Identifier id, boolean intoHistory) {
        Plays p = PLAYS.get(id);
        long now = System.currentTimeMillis();
        PLAYS.put(id, new Plays(p == null ? 1 : p.count() + 1, now));
        if (intoHistory) {
            HISTORY.removeIf(e -> e.id().equals(id));
            HISTORY.add(0, new Entry(id, now));
            while (HISTORY.size() > HISTORY_MAX) HISTORY.remove(HISTORY.size() - 1);
        }
        dirty = true;
        version++;
    }

    /** Son calanlar, en yenisi basta. */
    public static synchronized List<Entry> history() {
        return List.copyOf(HISTORY);
    }

    public static synchronized List<Identifier> historyIds() {
        return HISTORY.stream().map(Entry::id).toList();
    }

    public static synchronized Plays plays(Identifier id) {
        return PLAYS.getOrDefault(id, new Plays(0, 0));
    }

    /** En cok calanlar, cok calandan aza. */
    public static synchronized List<Map.Entry<Identifier, Plays>> mostPlayed() {
        List<Map.Entry<Identifier, Plays>> list = new ArrayList<>(PLAYS.entrySet());
        list.sort(Comparator.<Map.Entry<Identifier, Plays>>comparingInt(e -> -e.getValue().count())
                .thenComparing(e -> -e.getValue().last()));
        return list;
    }

    public static synchronized int version() {
        return version;
    }

    /** Bir parcanin gecmisini ve sayacini siler. */
    public static synchronized void forget(Identifier id) {
        PLAYS.remove(id);
        HISTORY.removeIf(e -> e.id().equals(id));
        dirty = true;
        version++;
    }

    public static synchronized void clear() {
        HISTORY.clear();
        PLAYS.clear();
        dirty = true;
        version++;
        saveNow();
    }

    // ---------------------------------------------------------------- dosya

    public static synchronized void load() {
        HISTORY.clear();
        PLAYS.clear();
        Path path = file();
        if (!Files.exists(path)) return;
        try {
            JsonObject root = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) return;
            if (root.has("plays")) {
                JsonObject plays = root.getAsJsonObject("plays");
                for (String key : plays.keySet()) {
                    Identifier id = Identifier.tryParse(key);
                    if (id == null) continue;
                    JsonObject o = plays.getAsJsonObject(key);
                    PLAYS.put(id, new Plays(o.has("count") ? o.get("count").getAsInt() : 0,
                            o.has("last") ? o.get("last").getAsLong() : 0));
                }
            }
            if (root.has("history")) {
                for (JsonElement e : root.getAsJsonArray("history")) {
                    JsonObject o = e.getAsJsonObject();
                    Identifier id = o.has("id") ? Identifier.tryParse(o.get("id").getAsString()) : null;
                    if (id != null && HISTORY.size() < HISTORY_MAX) {
                        HISTORY.add(new Entry(id, o.has("time") ? o.get("time").getAsLong() : 0));
                    }
                }
            }
        } catch (Exception e) {
            QuietPlease.LOGGER.warn("{} okunamadi, istatistikler sifirdan basliyor", path, e);
        }
        version++;
    }

    /** Her istemci tick'inde: degisiklik varsa en fazla 30 saniyede bir yazar. */
    public static void clientTick() {
        if (dirty && System.currentTimeMillis() - lastSave > 30_000) saveNow();
    }

    public static synchronized void saveNow() {
        if (!dirty) return;
        dirty = false;
        lastSave = System.currentTimeMillis();
        JsonObject plays = new JsonObject();
        PLAYS.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            JsonObject o = new JsonObject();
            o.addProperty("count", e.getValue().count());
            o.addProperty("last", e.getValue().last());
            plays.add(e.getKey().toString(), o);
        });
        JsonArray history = new JsonArray();
        for (Entry e : HISTORY) {
            JsonObject o = new JsonObject();
            o.addProperty("id", e.id().toString());
            o.addProperty("time", e.time());
            history.add(o);
        }
        JsonObject root = new JsonObject();
        root.add("history", history);
        root.add("plays", plays);
        Path target = file();
        try {
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            QuietPlease.LOGGER.warn("{} yazilamadi", target, e);
        }
    }
}
