package com.quietplease.music;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.quietplease.QuietPlease;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Ses seviyesi esitleme. Modlarin muzikleri cogu zaman Minecraft'in kendi muziklerinden cok daha yuksek ya da alcak.
 *
 * Her muzik dosyasinin ortalama yuksekligi bir kez olculur (arka planda, dusuk oncelikli tek bir thread'de) ve
 * config/quietplease-loudness.json dosyasinda saklanir; boylece olcum sadece ilk seferde yapilir. Olcum, yayincilikta
 * kullanilan yontemin (EBU R128 / LUFS) sade bir hali: insan kulagina gore agirliklandirma (K-weighting), 0,4 sn'lik
 * bloklar, sessiz kisimlari disarida birakan esikler.
 *
 * Hedef seviye, Minecraft muziklerinin (minecraft:music/...) ortanca yuksekligi. Her sarki bu seviyeye cekilir;
 * en fazla 18 dB kisilir, en fazla 6 dB yukseltilir (yukseltme, oyunun ses ayari zaten tam aciksa duyulmaz).
 */
public final class Loudness {

    private static final float MAX_CUT_DB = 18.0F;
    private static final float MAX_BOOST_DB = 6.0F;
    private static final int CACHE_VERSION = 1;
    private static final int SAVE_EVERY = 20;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** Dosya kimligi -> yukseklik (LUFS). NaN = olculemedi (dosyaya yazilmaz, bir sonraki acilista tekrar denenir). */
    private static final Map<ResourceLocation, Float> MEASURED = new ConcurrentHashMap<>();
    private static final LinkedBlockingDeque<ResourceLocation> QUEUE = new LinkedBlockingDeque<>();
    private static final Set<ResourceLocation> PENDING = ConcurrentHashMap.newKeySet();

    private static volatile float target = Float.NaN;
    /** Yeni olcum geldi: calan seslerin seviyesi oyunun thread'inde yenilensin. */
    private static volatile boolean changed;
    private static boolean cacheLoaded;
    private static int knownSoundCount = -1;
    private static Thread worker;

    private Loudness() {}

    // ---------------------------------------------------------------- carpan (oyun icinden cagrilir)

    /** Bu dosya icin ses seviyesi carpani. Esitleme kapaliysa ya da dosya henuz olculmediyse 1. */
    public static float factor(ResourceLocation file) {
        if (!ModOptions.normalize) return 1.0F;
        float t = target;
        Float level = MEASURED.get(file);
        if (level == null || level.isNaN() || Float.isNaN(t)) return 1.0F;
        float db = Mth.clamp(t - level, -MAX_CUT_DB, MAX_BOOST_DB);
        return (float) Math.pow(10.0, db / 20.0);
    }

    /** Esitleme acik ve hedef seviye belli mi? */
    public static boolean active() {
        return ModOptions.normalize && !Float.isNaN(target);
    }

    /** Olculmeyi bekleyen dosya sayisi (ayar ekraninda gosterilir). */
    public static int pending() {
        return PENDING.size();
    }

    /** Hedef seviye (LUFS); henuz yoksa NaN. */
    public static float target() {
        return target;
    }

    /** Olculmus yukseklik (LUFS); olculmediyse null. */
    public static Float level(ResourceLocation file) {
        return MEASURED.get(file);
    }

    // ---------------------------------------------------------------- olcum sirasi

    /** Her istemci tick'inde: esitleme aciksa olculmemis muzikleri siraya koyar, yeni olcumleri calan seslere uygular. */
    public static void clientTick(Minecraft mc) {
        if (!ModOptions.normalize) {
            if (!PENDING.isEmpty()) {
                QUEUE.clear();
                PENDING.clear();
            }
            knownSoundCount = -1; // tekrar acilinca bastan siraya koy
            return;
        }
        if (!cacheLoaded) {
            cacheLoaded = true;
            loadCache();
        }
        if (mc.getOverlay() != null) return; // kaynaklar yukleniyor
        int count = mc.getSoundManager().getAvailableSounds().size();
        if (count != knownSoundCount) {
            knownSoundCount = count; // ilk acilis ya da kaynak paketi degisti (F3+T)
            queueAll();
        }
        if (changed) {
            changed = false;
            MusicSettings.applyToPlayingSounds();
        }
    }

    /** Tum muzik ve plak dosyalarini siraya koyar; once Minecraft'in muzikleri (hedef seviye onlardan gelir). */
    private static void queueAll() {
        List<ResourceLocation> reference = new ArrayList<>();
        List<ResourceLocation> others = new ArrayList<>();
        for (List<MusicTrack> tracks : MusicLibrary.scan().values()) {
            for (MusicTrack track : tracks) {
                if (!track.kind().isSong()) continue;
                ResourceLocation id = track.id();
                if (MEASURED.containsKey(id) || !PENDING.add(id)) continue;
                (isReference(id) ? reference : others).add(id);
            }
        }
        reference.forEach(QUEUE::addLast);
        others.forEach(QUEUE::addLast);
        if (!reference.isEmpty() || !others.isEmpty()) {
            QuietPlease.LOGGER.info("Ses esitleme: {} parca olculecek", reference.size() + others.size());
            ensureWorker();
        }
    }

    /** Yeni baslayan bir sarki henuz olculmediyse sirada one alinir. */
    public static void prioritize(ResourceLocation file) {
        if (!ModOptions.normalize || !cacheLoaded || MEASURED.containsKey(file)) return;
        PENDING.add(file);
        QUEUE.remove(file);
        QUEUE.addFirst(file);
        ensureWorker();
    }

    private static synchronized void ensureWorker() {
        if (worker != null && worker.isAlive()) return;
        worker = new Thread(Loudness::work, "QuietPlease-Loudness");
        worker.setDaemon(true);
        worker.setPriority(Thread.MIN_PRIORITY);
        worker.start();
    }

    private static void work() {
        int sinceSave = 0;
        while (true) {
            ResourceLocation id;
            try {
                id = QUEUE.take();
            } catch (InterruptedException e) {
                return;
            }
            if (!PENDING.contains(id)) continue; // esitleme kapatildi
            float level = measure(id);
            MEASURED.put(id, level);
            PENDING.remove(id);
            if (isReference(id)) recomputeTarget();
            changed = true;
            if (++sinceSave >= SAVE_EVERY || QUEUE.isEmpty()) {
                sinceSave = 0;
                saveCache();
                if (QUEUE.isEmpty()) QuietPlease.LOGGER.info("Ses esitleme: olcum bitti, hedef {} LUFS", target);
            }
        }
    }

    private static boolean isReference(ResourceLocation file) {
        return file.getNamespace().equals("minecraft") && file.getPath().startsWith("music/");
    }

    /** Hedef: Minecraft muziklerinin ortanca yuksekligi. */
    private static void recomputeTarget() {
        List<Float> levels = new ArrayList<>();
        MEASURED.forEach((id, level) -> {
            if (isReference(id) && !level.isNaN()) levels.add(level);
        });
        if (levels.isEmpty()) return;
        levels.sort(null);
        int n = levels.size();
        target = n % 2 == 1 ? levels.get(n / 2) : (levels.get(n / 2 - 1) + levels.get(n / 2)) / 2.0F;
    }

    // ---------------------------------------------------------------- olcum

    /** Bir muzik dosyasinin yuksekligi (LUFS); okunamazsa NaN. */
    public static float measure(ResourceLocation file) {
        ResourceLocation path = ResourceLocation.fromNamespaceAndPath(file.getNamespace(), "sounds/" + file.getPath() + ".ogg");
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(path);
             JOrbisAudioStream stream = new JOrbisAudioStream(new BufferedInputStream(in))) {
            AudioFormat format = stream.getFormat();
            Meter meter = new Meter(format.getSampleRate(), format.getChannels());
            while (stream.readChunk(meter)) {
                // okumaya devam
            }
            return meter.loudness();
        } catch (Exception e) {
            QuietPlease.LOGGER.debug("Ses esitleme: {} olculemedi", file, e);
            return Float.NaN;
        }
    }

    /** Ornekleri (kanallar sirayla, -1..1) alir, 0,4 sn'lik bloklarin ortalama gucunu toplar. */
    public static final class Meter implements FloatConsumer {
        private final int channels;
        private final int blockFrames;
        private final Biquad[] shelf;
        private final Biquad[] highPass;
        private final DoubleArrayList blocks = new DoubleArrayList();
        private int channel;
        private int frames;
        private double blockSum;

        public Meter(float sampleRate, int channels) {
            this.channels = Math.max(1, channels);
            this.blockFrames = Math.max(1, Math.round(sampleRate * 0.4F));
            this.shelf = new Biquad[this.channels];
            this.highPass = new Biquad[this.channels];
            for (int c = 0; c < this.channels; c++) {
                shelf[c] = Biquad.highShelf(sampleRate, 1500.0, 4.0, 1.0 / Math.sqrt(2.0));
                highPass[c] = Biquad.highPass(sampleRate, 38.0, 0.5);
            }
        }

        @Override
        public void accept(float sample) {
            double y = highPass[channel].process(shelf[channel].process(sample));
            blockSum += y * y;
            if (++channel == channels) {
                channel = 0;
                if (++frames == blockFrames) {
                    blocks.add(blockSum / blockFrames);
                    blockSum = 0;
                    frames = 0;
                }
            }
        }

        /** Esikli ortalama: -70 LUFS alti bloklar (sessizlik) ve ortalamanin 10 LU altindakiler sayilmaz. */
        public float loudness() {
            double absolute = Math.pow(10.0, (-70.0 + 0.691) / 10.0);
            double sum = 0;
            int n = 0;
            for (int i = 0; i < blocks.size(); i++) {
                double b = blocks.getDouble(i);
                if (b > absolute) {
                    sum += b;
                    n++;
                }
            }
            if (n == 0) return Float.NaN;
            double relative = sum / n * 0.1;
            sum = 0;
            n = 0;
            for (int i = 0; i < blocks.size(); i++) {
                double b = blocks.getDouble(i);
                if (b > absolute && b > relative) {
                    sum += b;
                    n++;
                }
            }
            return n == 0 ? Float.NaN : (float) (-0.691 + 10.0 * Math.log10(sum / n));
        }
    }

    /** Ikinci dereceden suzgec (K-weighting'in iki asamasi icin). */
    static final class Biquad {
        private final double b0;
        private final double b1;
        private final double b2;
        private final double a1;
        private final double a2;
        private double z1;
        private double z2;

        private Biquad(double b0, double b1, double b2, double a0, double a1, double a2) {
            this.b0 = b0 / a0;
            this.b1 = b1 / a0;
            this.b2 = b2 / a0;
            this.a1 = a1 / a0;
            this.a2 = a2 / a0;
        }

        static Biquad highShelf(double rate, double freq, double gainDb, double q) {
            double a = Math.pow(10.0, gainDb / 40.0);
            double w0 = 2.0 * Math.PI * freq / rate;
            double cos = Math.cos(w0);
            double alpha = Math.sin(w0) / (2.0 * q);
            double s = 2.0 * Math.sqrt(a) * alpha;
            return new Biquad(
                    a * ((a + 1) + (a - 1) * cos + s),
                    -2.0 * a * ((a - 1) + (a + 1) * cos),
                    a * ((a + 1) + (a - 1) * cos - s),
                    (a + 1) - (a - 1) * cos + s,
                    2.0 * ((a - 1) - (a + 1) * cos),
                    (a + 1) - (a - 1) * cos - s);
        }

        static Biquad highPass(double rate, double freq, double q) {
            double w0 = 2.0 * Math.PI * freq / rate;
            double cos = Math.cos(w0);
            double alpha = Math.sin(w0) / (2.0 * q);
            return new Biquad((1 + cos) / 2.0, -(1 + cos), (1 + cos) / 2.0, 1 + alpha, -2.0 * cos, 1 - alpha);
        }

        double process(double x) {
            double y = b0 * x + z1;
            z1 = b1 * x - a1 * y + z2;
            z2 = b2 * x - a2 * y;
            return y;
        }
    }

    // ---------------------------------------------------------------- dosya

    private static Path cacheFile() {
        return com.quietplease.platform.Services.PLATFORM.configDir().resolve(QuietPlease.MODID + "-loudness.json");
    }

    private static void loadCache() {
        Path path = cacheFile();
        if (!Files.exists(path)) return;
        try {
            JsonObject root = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null || !root.has("version") || root.get("version").getAsInt() != CACHE_VERSION) return;
            JsonObject tracks = root.getAsJsonObject("tracks");
            for (String key : tracks.keySet()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id != null) MEASURED.put(id, tracks.get(key).getAsFloat());
            }
            recomputeTarget();
            QuietPlease.LOGGER.info("Ses esitleme: {} olcum yuklendi, hedef {} LUFS", MEASURED.size(), target);
        } catch (Exception e) {
            QuietPlease.LOGGER.warn("{} okunamadi, sarkilar yeniden olculecek", path, e);
        }
    }

    private static synchronized void saveCache() {
        JsonObject tracks = new JsonObject();
        new TreeMap<>(Map.copyOf(MEASURED)).forEach((id, level) -> {
            if (!level.isNaN()) tracks.addProperty(id.toString(), Math.round(level * 100.0F) / 100.0F);
        });
        JsonObject root = new JsonObject();
        root.addProperty("version", CACHE_VERSION);
        root.add("tracks", tracks);
        Path target = cacheFile();
        try {
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            QuietPlease.LOGGER.error("{} yazilamadi", target, e);
        }
    }
}
