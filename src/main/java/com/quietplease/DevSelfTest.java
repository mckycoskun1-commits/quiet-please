package com.quietplease;

import com.quietplease.music.MusicSettings;
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
        WeighedSoundEvents menu = sm.getSoundEvent(ResourceLocation.withDefaultNamespace("music.menu"));
        List<ResourceLocation> files = new ArrayList<>();
        for (Weighted<Sound> w : menu.list) if (w instanceof Sound s) files.add(s.getLocation());
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

        if (failures.isEmpty()) QuietPlease.LOGGER.info("SELFTEST BASARILI ({} menu parcasi ile)", files.size());
        else failures.forEach(f -> QuietPlease.LOGGER.error("SELFTEST HATA: {}", f));
    }
}
