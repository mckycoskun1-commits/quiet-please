package com.quietplease;

import com.quietplease.music.ModOptions;
import com.quietplease.music.MusicMixer;
import com.quietplease.music.MusicSettings;
import com.quietplease.music.Playlist;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Sadece gelistirme sirasinda (QUIETPLEASE_AUTODUMP ayarliyken) calisan kucuk bir oz-test.
 * Ayarlari gecici olarak degistirir, kontrol eder ve geri alir; dosyaya hicbir sey kaydetmez.
 */
final class DevSelfTest {

    private DevSelfTest() {}

    static void run() {
        List<String> failures = new ArrayList<>();
        SoundManager sm = Minecraft.getInstance().getSoundManager();
        RandomSource random = RandomSource.create(42);

        // 1) Parca atlama: music.menu icinde sonuncu haric hepsini kapat, 300 kez sec.
        WeighedSoundEvents menu = sm.getSoundEvent(Identifier.withDefaultNamespace("music.menu"));
        List<Identifier> files = new ArrayList<>();
        for (Weighted<Sound> w : ((com.quietplease.mixin.WeighedSoundEventsAccessor) menu).quietplease$getList()) if (w instanceof Sound s) files.add(s.getLocation());
        Identifier keep = files.get(files.size() - 1);
        files.stream().filter(f -> !f.equals(keep)).forEach(f -> MusicSettings.setEnabled(f, false));
        Set<Identifier> picked = new HashSet<>();
        for (int i = 0; i < 300; i++) picked.add(menu.getSound(random).getLocation());
        if (!picked.equals(Set.of(keep))) failures.add("atlama: beklenen sadece " + keep + ", gelen " + picked);

        // 2) Hepsi kapaliysa "bilerek sessiz" ses donmeli.
        MusicSettings.setEnabled(keep, false);
        Sound none = menu.getSound(random);
        if (none != SoundManager.INTENTIONALLY_EMPTY_SOUND) failures.add("hepsi kapali: sessiz ses beklenirdi, gelen " + none.getLocation());
        files.forEach(MusicSettings::reset);

        // 3) Ses seviyesi: ses motorunun hesabi %40 ile carpilmali.
        try {
            int fadeBackup = ModOptions.fadeSeconds;
            ModOptions.fadeSeconds = 0;
            SimpleSoundInstance music = SimpleSoundInstance.forMusic(SoundEvents.MUSIC_MENU.value());
            music.resolve(sm);
            Identifier file = music.getSound().getLocation();
            Field engineField = SoundManager.class.getDeclaredField("soundEngine");
            engineField.setAccessible(true);
            SoundEngine engine = (SoundEngine) engineField.get(sm);
            Method calc = SoundEngine.class.getDeclaredMethod("calculateVolume", net.minecraft.client.resources.sounds.SoundInstance.class);
            calc.setAccessible(true);

            float base = Mth.clamp(music.getVolume() * Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MUSIC), 0, 1);
            MusicSettings.setVolume(file, 40);
            float withSetting = (float) calc.invoke(engine, music);
            MusicSettings.setEnabled(file, false);
            float disabled = (float) calc.invoke(engine, music);
            MusicSettings.reset(file);
            float after = (float) calc.invoke(engine, music);
            MusicSettings.setModVolume(file.getNamespace(), 50);
            float modHalf = (float) calc.invoke(engine, music);
            MusicSettings.setVolume(file, 40);
            float modAndTrack = (float) calc.invoke(engine, music);
            MusicSettings.reset(file);
            MusicSettings.setModVolume(file.getNamespace(), 100);
            ModOptions.fadeSeconds = fadeBackup;
            if (Math.abs(modHalf - base * 0.5F) > 0.001F) failures.add("mod sesi: %50 bekleniyordu, " + base + " -> " + modHalf);
            if (Math.abs(modAndTrack - base * 0.2F) > 0.001F) failures.add("mod sesi: %50 x %40 = %20 bekleniyordu, gelen " + modAndTrack);
            if (MusicSettings.modVolume(file.getNamespace()) != 100) failures.add("mod sesi: %100'e donmedi");

            if (Math.abs(withSetting - base * 0.4F) > 0.001F) failures.add("ses: %40 bekleniyordu, " + base + " -> " + withSetting);
            if (disabled != 0F) failures.add("ses: kapali parca 0 olmali, gelen " + disabled);
            if (Math.abs(after - base) > 0.001F) failures.add("ses: sifirlaninca " + base + " beklenirdi, gelen " + after);
            QuietPlease.LOGGER.info("SELFTEST ses: taban={} %40={} kapali={} sifir={}", base, withSetting, disabled, after);
        } catch (ReflectiveOperationException e) {
            failures.add("ses testi calistirilamadi: " + e);
        }

        testSelection(menu, files, random, failures);
        testDurations(menu, failures);
        testLoudness(menu, failures);

        // 4) Calma listesi ve sarki arasi (ayarlar test sonunda geri alinir)
        testPlaylist(files, failures);

        // 5) Hazir ayar: disa aktar, degistir, ice aktar -> eski hal; gecici dosya silinir
        String preset = "selftest-tmp";
        try {
            MusicSettings.exportPreset(preset);
            boolean mode = ModOptions.playlistMode;
            MusicSettings.setVolume(keep, 25);
            ModOptions.playlistMode = !mode;
            MusicSettings.importPreset(preset);
            if (MusicSettings.get(keep).volume() != 100 || ModOptions.playlistMode != mode) {
                failures.add("hazir ayar: ice aktarinca eski hal gelmedi");
            }
            if (!MusicSettings.presetNames().contains(preset)) failures.add("hazir ayar: oneri listesinde yok");
            if (MusicSettings.isValidPresetName("../x") || !MusicSettings.isValidPresetName("my_pack-2")) {
                failures.add("hazir ayar: ad kontrolu yanlis");
            }
        } catch (Exception e) {
            failures.add("hazir ayar testi calistirilamadi: " + e);
        } finally {
            try {
                java.nio.file.Files.deleteIfExists(MusicSettings.presetFile(preset));
            } catch (java.io.IOException ignored) {}
        }

        testNewFeatures(files, failures);

        if (failures.isEmpty()) QuietPlease.LOGGER.info("SELFTEST BASARILI ({} menu parcasi ile)", files.size());
        else failures.forEach(f -> QuietPlease.LOGGER.error("SELFTEST HATA: {}", f));
    }

    private static void testSelection(WeighedSoundEvents menu, List<Identifier> files, RandomSource random, List<String> failures) {
        int repeatBackup = ModOptions.noRepeat;
        int favoriteBackup = ModOptions.favoriteWeight;
        Identifier favorite = files.get(0);
        boolean wasFavorite = Playlist.isFavorite(favorite);
        com.google.gson.JsonObject playlistBackup = Playlist.snapshot();
        try {
            ModOptions.favoriteWeight = 1;
            ModOptions.noRepeat = 5;
            MusicMixer.clearRecentForTest();
            List<Identifier> recent = files.subList(0, Math.min(5, files.size() - 1));
            recent.forEach(MusicMixer::remember);
            // Havuzun en fazla yarisi disarida kalir: 9 menu parcasinda son 4 calinan
            int excludedCount = Math.min(5, Math.max(1, files.size() / 2));
            List<Identifier> excluded = recent.subList(Math.max(0, recent.size() - excludedCount), recent.size());
            Set<Identifier> picked = new HashSet<>();
            for (int i = 0; i < 300; i++) picked.add(menu.getSound(random).getLocation());
            check(failures, picked.stream().noneMatch(excluded::contains), "tekrar etmeme: son calinan parca yine secildi " + picked);
            check(failures, picked.size() == files.size() - excluded.size(),
                    "tekrar etmeme: " + (files.size() - excluded.size()) + " aday beklenirdi, gelen " + picked.size());

            // Kucuk havuz (Nether, 5 parca): hepsi son calinanlardaysa bile en son calan 2 parca disarida, 3 aday kalir
            WeighedSoundEvents nether = Minecraft.getInstance().getSoundManager()
                    .getSoundEvent(Identifier.withDefaultNamespace("music.nether.basalt_deltas"));
            if (nether != null) {
                List<Identifier> netherFiles = new ArrayList<>();
                for (Weighted<Sound> w : ((com.quietplease.mixin.WeighedSoundEventsAccessor) nether).quietplease$getList()) {
                    if (w instanceof Sound s) netherFiles.add(s.getLocation());
                }
                MusicMixer.clearRecentForTest();
                netherFiles.forEach(MusicMixer::remember);
                int skip = Math.min(5, Math.max(1, netherFiles.size() / 2));
                Set<Identifier> lastPlayed = new HashSet<>(netherFiles.subList(netherFiles.size() - skip, netherFiles.size()));
                Set<Identifier> netherPicked = new HashSet<>();
                for (int i = 0; i < 300; i++) netherPicked.add(nether.getSound(random).getLocation());
                check(failures, netherPicked.stream().noneMatch(lastPlayed::contains)
                        && netherPicked.size() == netherFiles.size() - skip,
                        "kucuk havuz: " + (netherFiles.size() - skip) + " aday beklenirdi, gelen " + netherPicked);
                QuietPlease.LOGGER.info("SELFTEST kucuk havuz: {} parcadan {} aday", netherFiles.size(), netherPicked.size());
                MusicMixer.clearRecentForTest();
            }

            ModOptions.noRepeat = 0;
            MusicMixer.optionsChanged();
            ModOptions.favoriteWeight = 5;
            if (!wasFavorite) Playlist.toggleFavorite(favorite);
            int hits = 0;
            int rounds = 3000;
            for (int i = 0; i < rounds; i++) if (menu.getSound(random).getLocation().equals(favorite)) hits++;
            int sum = 0;
            int favoriteBase = 1;
            for (Weighted<Sound> w : ((com.quietplease.mixin.WeighedSoundEventsAccessor) menu).quietplease$getList()) {
                sum += w instanceof Sound s0 ? MusicMixer.weight(s0.getLocation(), w.getWeight()) : w.getWeight();
                if (w instanceof Sound s && s.getLocation().equals(favorite)) favoriteBase = MusicMixer.weight(favorite, w.getWeight());
            }
            double expected = favoriteBase / (double) sum;
            check(failures, Math.abs(hits / (double) rounds - expected) < 0.08,
                    "favori agirligi: beklenen ~" + Math.round(expected * 100) + "%, gelen " + Math.round(hits * 100.0 / rounds) + "%");
            QuietPlease.LOGGER.info("SELFTEST favori x5: {}/{} secim", hits, rounds);
        } finally {
            Playlist.restore(playlistBackup);
            ModOptions.noRepeat = repeatBackup;
            ModOptions.favoriteWeight = favoriteBackup;
            MusicMixer.clearRecentForTest();
        }
    }

    /** Sure okuma: dosyanin sadece basini ve sonunu tutan okuma, tum dosyayi okuyan basit yontemle ayni sonucu vermeli. */
    private static void testDurations(WeighedSoundEvents menu, List<String> failures) {
        StringBuilder log = new StringBuilder();
        for (Weighted<Sound> w : ((com.quietplease.mixin.WeighedSoundEventsAccessor) menu).quietplease$getList()) {
            if (!(w instanceof Sound s)) continue;
            int fast = Playlist.readOggSeconds(s.getPath());
            int full = fullReadSeconds(s.getPath());
            check(failures, fast > 0 && fast == full, "sure: " + s.getLocation() + " hizli=" + fast + " tam=" + full);
            log.append(' ').append(s.getLocation().getPath()).append('=').append(fast);
        }
        QuietPlease.LOGGER.info("SELFTEST sureler (sn):{}", log);
    }

    /** Ses esitleme: olcum makul bir LUFS degeri vermeli; sessiz bir sinyal NaN, 1 kHz tam sinus yaklasik -3 LUFS olmali. */
    private static void testLoudness(WeighedSoundEvents menu, List<String> failures) {
        StringBuilder log = new StringBuilder();
        long start = System.nanoTime();
        int n = 0;
        for (Weighted<Sound> w : ((com.quietplease.mixin.WeighedSoundEventsAccessor) menu).quietplease$getList()) {
            if (!(w instanceof Sound s) || n >= 3) continue;
            float level = com.quietplease.music.Loudness.measure(s.getLocation());
            check(failures, level > -45 && level < -5, "ses esitleme: " + s.getLocation() + " = " + level + " LUFS");
            log.append(' ').append(s.getLocation().getPath()).append('=').append(Math.round(level * 10) / 10.0);
            n++;
        }
        QuietPlease.LOGGER.info("SELFTEST yukseklik (LUFS):{} ({} ms)", log, (System.nanoTime() - start) / 1_000_000);

        com.quietplease.music.Loudness.Meter silent = new com.quietplease.music.Loudness.Meter(48000, 2);
        for (int i = 0; i < 48000 * 2 * 2; i++) silent.accept(0.0F);
        check(failures, Float.isNaN(silent.loudness()), "ses esitleme: sessizlik NaN olmali");
        com.quietplease.music.Loudness.Meter sine = new com.quietplease.music.Loudness.Meter(48000, 1);
        for (int i = 0; i < 48000 * 3; i++) sine.accept((float) Math.sin(2 * Math.PI * 1000 * i / 48000.0));
        float sineLevel = sine.loudness();
        // EBU: 1 kHz, 0 dBFS tepe sinus tek kanalda -3,01 LUFS
        check(failures, Math.abs(sineLevel + 3.0F) < 0.5F, "ses esitleme: 1 kHz sinus -3 LUFS beklenirdi, gelen " + sineLevel);
        QuietPlease.LOGGER.info("SELFTEST sinus 1 kHz: {} LUFS", sineLevel);
    }

    private static int fullReadSeconds(Identifier path) {
        try (java.io.InputStream in = Minecraft.getInstance().getResourceManager().open(path)) {
            byte[] b = in.readAllBytes();
            long rate = 0;
            for (int i = 0; i + 16 < b.length && i < 8192; i++) {
                if (b[i] == 1 && b[i + 1] == 'v' && b[i + 2] == 'o' && b[i + 3] == 'r' && b[i + 4] == 'b'
                        && b[i + 5] == 'i' && b[i + 6] == 's') {
                    rate = (b[i + 12] & 0xFFL) | (b[i + 13] & 0xFFL) << 8 | (b[i + 14] & 0xFFL) << 16 | (b[i + 15] & 0xFFL) << 24;
                    break;
                }
            }
            for (int i = b.length - 14; i >= 0; i--) {
                if (b[i] == 'O' && b[i + 1] == 'g' && b[i + 2] == 'g' && b[i + 3] == 'S') {
                    long granule = 0;
                    for (int k = 7; k >= 0; k--) granule = (granule << 8) | (b[i + 6 + k] & 0xFF);
                    return rate > 0 && granule > 0 ? (int) (granule / rate) : -1;
                }
            }
        } catch (Exception e) {
            return -2;
        }
        return -1;
    }

    static final String TEST_SONG = "selftest_song.ogg";

    /** Kendi muzik klasorune bir oyun sarkisinin kopyasini koyar; klasor yeniden taranir. */
    static Identifier installTestSong(Identifier sourceFile) throws java.io.IOException {
        java.nio.file.Path dir = com.quietplease.music.CustomMusic.folder();
        java.nio.file.Files.createDirectories(dir);
        Identifier path = Identifier.fromNamespaceAndPath(sourceFile.getNamespace(), "sounds/" + sourceFile.getPath() + ".ogg");
        try (var in = Minecraft.getInstance().getResourceManager().open(path)) {
            java.nio.file.Files.copy(in, dir.resolve(TEST_SONG), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        com.quietplease.music.CustomMusic.refresh();
        return Identifier.fromNamespaceAndPath(com.quietplease.music.CustomMusic.NAMESPACE, "selftest_song");
    }

    static void removeTestSong() {
        try {
            java.nio.file.Files.deleteIfExists(com.quietplease.music.CustomMusic.folder().resolve(TEST_SONG));
        } catch (java.io.IOException ignored) {}
        com.quietplease.music.CustomMusic.refresh();
    }

    /** 1.5.0: nota bloklari, kendi muzigin, dinleme gecmisi, bir kerelik calma. */
    private static void testNewFeatures(List<Identifier> menuFiles, List<String> failures) {
        // Dosya adi -> kimlik
        check(failures, com.quietplease.music.CustomMusic.slugForTest("Benim Sarkim (v2).ogg").equals("benim_sarkim_v2"),
                "kendi muzigin: ad " + com.quietplease.music.CustomMusic.slugForTest("Benim Sarkim (v2).ogg"));
        check(failures, com.quietplease.music.CustomMusic.slugForTest("Albüm\\01 - Giriş.OGG").equals("alb_m/01_-_giri"),
                "kendi muzigin: alt klasor adi " + com.quietplease.music.CustomMusic.slugForTest("Albüm\\01 - Giriş.OGG"));
        check(failures, com.quietplease.music.CustomMusic.slugForTest("Benim Şarkım.MP3").equals("benim_ar_m"),
                "kendi muzigin: mp3 adi " + com.quietplease.music.CustomMusic.slugForTest("Benim Şarkım.MP3"));
        check(failures, com.quietplease.music.MusicTrack.title(Identifier.fromNamespaceAndPath("somemod", "music/boss__theme_2")).equals("boss theme 2"),
                "sarki adi: alt cizgiler bosluk olmali " + com.quietplease.music.MusicTrack.title(Identifier.fromNamespaceAndPath("somemod", "music/boss__theme_2")));

        // Nota bloklari listede, kendi turleriyle
        var library = com.quietplease.music.MusicLibrary.scan();
        var harp = library.getOrDefault("minecraft", List.of()).stream()
                .filter(t -> t.id().equals(Identifier.withDefaultNamespace("note/bell"))).findFirst();
        check(failures, harp.isPresent() && harp.get().kind() == com.quietplease.music.MusicTrack.Kind.NOTE,
                "nota blogu: minecraft:note/bell NOTE olmali " + harp.map(t -> t.kind().toString()).orElse("yok"));

        // Kendi muzigin: dosya listede, kaynak yoneticisinden okunuyor, calinabilir ses olusuyor
        try {
            Identifier custom = installTestSong(menuFiles.get(0));
            var scanned = com.quietplease.music.MusicLibrary.scan();
            check(failures, scanned.containsKey(com.quietplease.music.CustomMusic.NAMESPACE)
                            && scanned.keySet().iterator().next().equals(com.quietplease.music.CustomMusic.NAMESPACE),
                    "kendi muzigin: grup en ustte olmali " + scanned.keySet());
            int seconds = Playlist.readOggSeconds(com.quietplease.music.CustomMusic.sound(custom).getPath());
            int original = Playlist.readOggSeconds(Identifier.fromNamespaceAndPath(menuFiles.get(0).getNamespace(),
                    "sounds/" + menuFiles.get(0).getPath() + ".ogg"));
            check(failures, seconds > 0 && seconds == original, "kendi muzigin: sure " + seconds + " / " + original);
            check(failures, Playlist.createSoundFor(custom) != null, "kendi muzigin: ses olusturulamadi");
            check(failures, com.quietplease.music.MusicTrack.title(custom).equals("selftest song"),
                    "kendi muzigin: ekrandaki ad " + com.quietplease.music.MusicTrack.title(custom));

            boolean bg = ModOptions.customInBackground;
            ModOptions.customInBackground = false;
            check(failures, com.quietplease.music.CustomMusic.startInstead(Musics.MENU) == null, "kendi muzigin: ayar kapaliyken calmamali");
            ModOptions.customInBackground = true;
            int hits = 0;
            for (int i = 0; i < 400; i++) {
                if (com.quietplease.music.CustomMusic.startInstead(Musics.MENU) instanceof Playlist.PlaylistSound ps && ps.trackId.equals(custom)) hits++;
            }
            // 1 kendi sarkin + menunun havuzu: yaklasik 1/(1+n)
            double expected = 1.0 / (1 + menuFiles.size());
            check(failures, Math.abs(hits / 400.0 - expected) < 0.06, "kendi muzigin: karisma orani " + hits + "/400, beklenen ~" + expected);
            ModOptions.customInBackground = bg;
        } catch (Exception e) {
            failures.add("kendi muzigin testi calistirilamadi: " + e);
        } finally {
            removeTestSong();
        }

        // Bir kerelik calma (Onceki tusu): siradaki arka plan sarkisi o parca, gecmise yeniden eklenmez
        boolean modeBackup = ModOptions.playlistMode;
        ModOptions.playlistMode = false;
        Identifier target = menuFiles.get(1);
        Playlist.playOnce(target, true);
        var once = Playlist.startInstead(Musics.MENU);
        check(failures, once instanceof Playlist.PlaylistSound ps && ps.trackId.equals(target) && ps.replay && !Playlist.hasForced(),
                "bir kerelik calma: " + once);
        check(failures, Playlist.startInstead(Musics.MENU) == null, "bir kerelik calma: ikinci kez calmamali");
        ModOptions.playlistMode = modeBackup;

        // Gecmis ve sayac
        Identifier fake = Identifier.fromNamespaceAndPath("selftest", "song");
        com.quietplease.music.ListenStats.played(fake, true);
        com.quietplease.music.ListenStats.played(fake, false);
        check(failures, com.quietplease.music.ListenStats.plays(fake).count() == 2
                        && fake.equals(com.quietplease.music.ListenStats.historyIds().get(0)),
                "gecmis: sayac " + com.quietplease.music.ListenStats.plays(fake).count());
        com.quietplease.music.ListenStats.forget(fake);
        check(failures, com.quietplease.music.ListenStats.plays(fake).count() == 0
                && !com.quietplease.music.ListenStats.historyIds().contains(fake), "gecmis: silinemedi");
        QuietPlease.LOGGER.info("SELFTEST yeni ozellikler: {} hata", failures.size());
    }

    private static void check(List<String> failures, boolean ok, String what) {
        if (!ok) failures.add(what);
    }

    private static void testPlaylist(List<Identifier> menuFiles, List<String> failures) {
        if (menuFiles.size() < 4) {
            failures.add("calma listesi: en az 4 menu parcasi gerekiyordu, " + menuFiles.size() + " var");
            return;
        }
        com.google.gson.JsonObject backup = Playlist.snapshot();
        boolean modeBackup = ModOptions.playlistMode;
        ModOptions.Gap gapBackup = ModOptions.gap;
        Identifier a = menuFiles.get(0), b = menuFiles.get(1), c = menuFiles.get(2), d = menuFiles.get(3);
        java.util.function.Predicate<Identifier> all = id -> true;
        try {
            // Sirayla calar, calan parca bitince listeden cikar (oyuncunun favorileri/gecmisi karismasin diye bos baslar)
            Playlist.restore(new com.google.gson.JsonObject());
            Playlist.add(a);
            Playlist.add(b);
            Playlist.add(c);
            Playlist.add(a); // ayni parca iki kez eklenmez
            check(failures, Playlist.size() == 3, "calma listesi: tekrar eklenen parca listede iki kez");
            check(failures, Playlist.upcoming().equals(List.of(0, 1, 2)), "calma listesi: baslangic sirasi " + Playlist.upcoming());
            List<Identifier> played = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                int idx = Playlist.advanceForTest(all);
                played.add(idx < 0 ? null : Playlist.tracks().get(idx));
            }
            check(failures, java.util.Arrays.asList(a, b, c, null).equals(played) && Playlist.isEmpty(),
                    "calma listesi: calinanlar cikmali " + played + " kalan " + Playlist.tracks());

            // Dongudeki parca silinmez, yerinde kalir; sira ardindan devam eder, sonda basa doner
            Playlist.clear();
            for (Identifier id : List.of(a, b, c)) Playlist.add(id);
            Playlist.toggleLoop(a);
            Playlist.advanceForTest(all); // a caliyor
            check(failures, Playlist.upcoming().equals(List.of(1, 2)), "calma listesi: calan parca siradakilerde olmamali " + Playlist.upcoming());
            Playlist.advanceForTest(all); // a bitti -> yerinde, b caliyor
            check(failures, Playlist.tracks().equals(List.of(a, b, c)) && b.equals(Playlist.currentTrack()),
                    "calma listesi: dongu yerinde kalmali " + Playlist.tracks());
            check(failures, Playlist.upcoming().equals(List.of(2, 0)), "calma listesi: siradakiler basa donmeli " + Playlist.upcoming());
            Playlist.advanceForTest(all); // b bitti -> silindi, c caliyor
            check(failures, c.equals(Playlist.currentTrack()), "calma listesi: b'den sonra c calmali " + Playlist.currentTrack());
            Playlist.advanceForTest(all); // c bitti -> silindi, basa donup a caliyor
            check(failures, Playlist.tracks().equals(List.of(a)) && a.equals(Playlist.currentTrack()),
                    "calma listesi: dongusuzler silinmeli " + Playlist.tracks());
            int again = Playlist.advanceForTest(all);
            check(failures, again == 0 && Playlist.tracks().equals(List.of(a)), "calma listesi: tek dongulu parca tekrar calmali");

            // Calinamayan parca atlanir ama listede kalir
            Playlist.clear();
            for (Identifier id : List.of(a, b, c)) Playlist.add(id);
            int first = Playlist.advanceForTest(id -> !id.equals(a));
            check(failures, first == 1 && Playlist.contains(a), "calma listesi: kapali parcayi atlama " + first);

            // ▶: secilen parca yerinden oynamaz; calan parca (b) biter, sira secilenin ardindan devam eder
            Playlist.add(d); // a b c d, b caliyor
            Playlist.setNext(2); // c
            check(failures, Playlist.tracks().equals(List.of(a, c, d)) && Playlist.currentTrack() == null,
                    "calma listesi: siradaki yapma " + Playlist.tracks());
            int chosen = Playlist.advanceForTest(all);
            check(failures, chosen == 1 && c.equals(Playlist.currentTrack()), "calma listesi: secilen calmali " + chosen);
            check(failures, Playlist.upcoming().equals(List.of(2, 0)), "calma listesi: secilenin ardindan devam " + Playlist.upcoming());
            // calan parca listeden cikarilinca ardindaki calar
            Playlist.remove(c);
            int afterRemove = Playlist.advanceForTest(all);
            check(failures, afterRemove == 1 && Playlist.tracks().equals(List.of(a, d)) && d.equals(Playlist.currentTrack()),
                    "calma listesi: calan silinince " + Playlist.tracks() + " " + Playlist.currentTrack());

            // Tasima ve karistirma
            Playlist.clear();
            for (Identifier id : List.of(a, b, c, d)) Playlist.add(id);
            Playlist.swap(2, 3);
            check(failures, Playlist.tracks().equals(List.of(a, b, d, c)), "calma listesi: yer degistirme " + Playlist.tracks());
            Playlist.shuffle();
            check(failures, new HashSet<>(Playlist.tracks()).equals(Set.of(a, b, c, d)), "calma listesi: karistirma " + Playlist.tracks());

            // Favoriler: siraya da eklenir, calinca favori kalir, yildiz kaldirilinca siradan da cikar
            Playlist.clear();
            Playlist.toggleFavorite(d);
            check(failures, Playlist.isFavorite(d) && Playlist.contains(d), "favori: siraya eklenmeli");
            Playlist.advanceForTest(all);
            Playlist.advanceForTest(all);
            check(failures, Playlist.isFavorite(d) && !Playlist.contains(d), "favori: calinca favori kalmali, siradan cikmali");
            Playlist.toggleFavorite(d);
            Playlist.toggleFavorite(d);
            Playlist.toggleFavorite(d);
            check(failures, !Playlist.isFavorite(d) && !Playlist.contains(d) && Playlist.history().get(0).equals(d),
                    "favori: kaldirilinca siradan da cikmali, gecmiste kalmali");

            // Dosyaya yazip okuyunca dongu, favori ve gecmis korunur
            Playlist.clear();
            Playlist.add(b);
            Playlist.toggleLoop(b);
            Playlist.toggleFavorite(c);
            com.google.gson.JsonObject saved = Playlist.snapshot();
            Playlist.restore(saved);
            check(failures, Playlist.tracks().equals(List.of(b, c)) && Playlist.isLooping(b) && Playlist.isFavorite(c)
                    && !Playlist.isFavorite(b) && Playlist.history().contains(b), "calma listesi: kaydet/yukle " + saved);

            // Arka plan muzigi tanimi
            check(failures, ModOptions.isBackground(Musics.MENU) && ModOptions.isBackground(Musics.GAME)
                    && ModOptions.isBackground(Musics.END) && ModOptions.isBackground(Musics.UNDER_WATER)
                    && ModOptions.isBackground(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_BASALT_DELTAS)),
                    "arka plan: menu/oyun/End/su alti/Nether arka plan sayilmali");
            check(failures, !ModOptions.isBackground(Musics.END_BOSS) && !ModOptions.isBackground(Musics.CREDITS)
                    && !ModOptions.isBackground(new Music(SoundEvents.MUSIC_GAME, 0, 0, true)),
                    "arka plan: ejderha/jenerik/mod boss muzigi arka plan sayilmamali");

            // Sarki arasi
            ModOptions.gap = ModOptions.Gap.VANILLA;
            check(failures, ModOptions.adjustGap(Musics.GAME) == Musics.GAME, "sarki arasi: Vanilla'da ayni nesne donmeli");
            ModOptions.gap = ModOptions.Gap.NONE;
            Music none = ModOptions.adjustGap(Musics.GAME);
            check(failures, none.minDelay() == 20 && none.maxDelay() == 20 && ModOptions.isBackground(none)
                    && none.sound() == Musics.GAME.sound(), "sarki arasi: Yok -> 20 tick");
            ModOptions.gap = ModOptions.Gap.SHORT;
            Music shortGap = ModOptions.adjustGap(Musics.MENU);
            check(failures, shortGap.minDelay() == 600 && shortGap.maxDelay() == 1200 && shortGap.replaceCurrentMusic(),
                    "sarki arasi: Kisa -> 600-1200 tick");
            check(failures, ModOptions.adjustGap(Musics.END_BOSS) == Musics.END_BOSS, "sarki arasi: ejderha muzigi degismemeli");

            // Devralma: mod kapaliyken hic, aciksa sadece arka plan muziginde
            Playlist.clear();
            Playlist.add(c);
            Playlist.toggleLoop(c); // ikinci denemede de listede kalsin
            ModOptions.playlistMode = false;
            check(failures, Playlist.startInstead(Musics.MENU) == null, "devralma: mod kapaliyken devralmamali");
            ModOptions.playlistMode = true;
            check(failures, Playlist.startInstead(Musics.END_BOSS) == null, "devralma: ejderha muzigini devralmamali");
            var sound = Playlist.startInstead(Musics.MENU);
            if (sound != null) sound.resolve(Minecraft.getInstance().getSoundManager()); // getSound() resolve ile dolar
            check(failures, sound instanceof Playlist.PlaylistSound ps && ps.trackId.equals(c)
                    && sound.getSound() != null && sound.getSound().getLocation().equals(c), "devralma: menu muziginde listedeki parca calmali");
            MusicSettings.setEnabled(c, false);
            check(failures, Playlist.startInstead(Musics.MENU) == null, "devralma: tek parca kapaliysa oyunun muzigi calmali");
            MusicSettings.reset(c);
            QuietPlease.LOGGER.info("SELFTEST calma listesi: {} hata", failures.size());
        } catch (RuntimeException e) {
            failures.add("calma listesi testi coktu: " + e);
            QuietPlease.LOGGER.error("SELFTEST calma listesi", e);
        } finally {
            Playlist.restore(backup);
            ModOptions.playlistMode = modeBackup;
            ModOptions.gap = gapBackup;
            MusicSettings.save(); // startInstead sirayi kaydetmisti: eski hali geri yaz
        }
    }
}
