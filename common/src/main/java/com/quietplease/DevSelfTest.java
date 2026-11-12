package com.quietplease;

import com.quietplease.music.ModOptions;
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
import net.minecraft.resources.ResourceLocation;
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
        WeighedSoundEvents menu = sm.getSoundEvent(new ResourceLocation("music.menu"));
        List<ResourceLocation> files = new ArrayList<>();
        for (Weighted<Sound> w : ((com.quietplease.mixin.WeighedSoundEventsAccessor) menu).quietplease$getList()) if (w instanceof Sound s) files.add(s.getLocation());
        ResourceLocation keep = files.get(files.size() - 1);
        files.stream().filter(f -> !f.equals(keep)).forEach(f -> MusicSettings.setEnabled(f, false));
        Set<ResourceLocation> picked = new HashSet<>();
        for (int i = 0; i < 300; i++) picked.add(menu.getSound(random).getLocation());
        if (!picked.equals(Set.of(keep))) failures.add("atlama: beklenen sadece " + keep + ", gelen " + picked);

        // 2) Hepsi kapaliysa "bilerek sessiz" ses donmeli.
        MusicSettings.setEnabled(keep, false);
        Sound none = menu.getSound(random);
        if (none != SoundManager.INTENTIONALLY_EMPTY_SOUND) failures.add("hepsi kapali: sessiz ses beklenirdi, gelen " + none.getLocation());
        files.forEach(MusicSettings::reset);

        // 3) Ses seviyesi: ses motorunun hesabi %40 ile carpilmali.
        try {
            SimpleSoundInstance music = SimpleSoundInstance.forMusic(SoundEvents.MUSIC_MENU.value());
            music.resolve(sm);
            ResourceLocation file = music.getSound().getLocation();
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

            if (Math.abs(withSetting - base * 0.4F) > 0.001F) failures.add("ses: %40 bekleniyordu, " + base + " -> " + withSetting);
            if (disabled != 0F) failures.add("ses: kapali parca 0 olmali, gelen " + disabled);
            if (Math.abs(after - base) > 0.001F) failures.add("ses: sifirlaninca " + base + " beklenirdi, gelen " + after);
            QuietPlease.LOGGER.info("SELFTEST ses: taban={} %40={} kapali={} sifir={}", base, withSetting, disabled, after);
        } catch (ReflectiveOperationException e) {
            failures.add("ses testi calistirilamadi: " + e);
        }

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

        if (failures.isEmpty()) QuietPlease.LOGGER.info("SELFTEST BASARILI ({} menu parcasi ile)", files.size());
        else failures.forEach(f -> QuietPlease.LOGGER.error("SELFTEST HATA: {}", f));
    }

    private static void check(List<String> failures, boolean ok, String what) {
        if (!ok) failures.add(what);
    }

    private static void testPlaylist(List<ResourceLocation> menuFiles, List<String> failures) {
        if (menuFiles.size() < 4) {
            failures.add("calma listesi: en az 4 menu parcasi gerekiyordu, " + menuFiles.size() + " var");
            return;
        }
        com.google.gson.JsonObject backup = Playlist.snapshot();
        boolean modeBackup = ModOptions.playlistMode;
        ModOptions.Gap gapBackup = ModOptions.gap;
        ResourceLocation a = menuFiles.get(0), b = menuFiles.get(1), c = menuFiles.get(2), d = menuFiles.get(3);
        java.util.function.Predicate<ResourceLocation> all = id -> true;
        try {
            // Sirayla, sonra basa sar
            Playlist.clear();
            Playlist.add(a);
            Playlist.add(b);
            Playlist.add(c);
            Playlist.add(a); // ayni parca iki kez eklenmez
            Playlist.setRepeat(Playlist.Repeat.REPEAT_LIST);
            check(failures, Playlist.size() == 3, "calma listesi: tekrar eklenen parca listede iki kez");
            check(failures, Playlist.upcoming().equals(List.of(0, 1, 2)), "calma listesi: baslangic sirasi " + Playlist.upcoming());
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < 4; i++) order.add(Playlist.advanceForTest(all));
            check(failures, order.equals(List.of(0, 1, 2, 0)), "calma listesi: tekrar sirasi " + order);
            Playlist.advanceForTest(all); // simdi b caliyor (sira 1)
            check(failures, Playlist.upcoming().equals(List.of(2, 0)) && Playlist.isNextRound(0) && !Playlist.isNextRound(2),
                    "calma listesi: siradakiler " + Playlist.upcoming());

            // Calinamayan parca atlanir
            Playlist.setNext(0);
            List<Integer> skipped = new ArrayList<>();
            for (int i = 0; i < 3; i++) skipped.add(Playlist.advanceForTest(id -> !id.equals(b)));
            check(failures, skipped.equals(List.of(0, 2, 0)), "calma listesi: kapali parcayi atlama " + skipped);

            // Calandan once silinince siradaki yine dogru
            Playlist.setNext(1);
            Playlist.advanceForTest(all); // b caliyor
            Playlist.removeAt(0);         // a silindi
            int next = Playlist.advanceForTest(all);
            check(failures, next >= 0 && Playlist.tracks().get(next).equals(c), "calma listesi: silmeden sonra siradaki c olmaliydi");

            // Sonda dur -> -1, liste degisince yeniden baslar
            Playlist.clear();
            Playlist.add(a);
            Playlist.add(b);
            Playlist.setRepeat(Playlist.Repeat.STOP);
            List<Integer> stop = new ArrayList<>();
            for (int i = 0; i < 3; i++) stop.add(Playlist.advanceForTest(all));
            check(failures, stop.equals(List.of(0, 1, -1)) && Playlist.hasEnded(), "calma listesi: sonda dur " + stop);
            check(failures, Playlist.advanceForTest(all) == -1, "calma listesi: bittikten sonra -1 kalmali");
            Playlist.setNext(0);
            check(failures, !Playlist.hasEnded() && Playlist.advanceForTest(all) == 0, "calma listesi: ▶ sonrasi yeniden baslamali");

            // Tasima ve karistirma: calan parca basta kalir
            Playlist.clear();
            for (ResourceLocation id : List.of(a, b, c, d)) Playlist.add(id);
            Playlist.setRepeat(Playlist.Repeat.REPEAT_LIST);
            Playlist.advanceForTest(all);
            Playlist.advanceForTest(all); // b caliyor (sira 1)
            Playlist.swap(2, 3);
            check(failures, Playlist.tracks().equals(List.of(a, b, d, c)), "calma listesi: yer degistirme " + Playlist.tracks());
            Playlist.shuffle();
            check(failures, Playlist.tracks().get(0).equals(b) && Playlist.position() == 0
                    && new HashSet<>(Playlist.tracks()).equals(Set.of(a, b, c, d)), "calma listesi: karistirma " + Playlist.tracks());

            // Surekli karistir: tur bitince liste karisir, ayni parca arka arkaya gelmez
            Playlist.setRepeat(Playlist.Repeat.SHUFFLE);
            for (int round = 0; round < 20; round++) {
                ResourceLocation last = null;
                for (int i = 0; i < 4; i++) {
                    int idx = Playlist.advanceForTest(all);
                    ResourceLocation now = Playlist.tracks().get(idx);
                    if (now.equals(last)) failures.add("calma listesi: surekli karistirmada ayni parca arka arkaya");
                    last = now;
                }
            }
            check(failures, Playlist.size() == 4, "calma listesi: karistirmada parca kayboldu");

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
            check(failures, none.getMinDelay() == 20 && none.getMaxDelay() == 20 && ModOptions.isBackground(none)
                    && none.getEvent() == Musics.GAME.getEvent(), "sarki arasi: Yok -> 20 tick");
            ModOptions.gap = ModOptions.Gap.SHORT;
            Music shortGap = ModOptions.adjustGap(Musics.MENU);
            check(failures, shortGap.getMinDelay() == 600 && shortGap.getMaxDelay() == 1200 && shortGap.replaceCurrentMusic(),
                    "sarki arasi: Kisa -> 600-1200 tick");
            check(failures, ModOptions.adjustGap(Musics.END_BOSS) == Musics.END_BOSS, "sarki arasi: ejderha muzigi degismemeli");

            // Devralma: mod kapaliyken hic, aciksa sadece arka plan muziginde
            Playlist.clear();
            Playlist.add(c);
            Playlist.setRepeat(Playlist.Repeat.REPEAT_LIST);
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
