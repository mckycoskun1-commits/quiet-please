package com.quietplease.music;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.Util;
import net.minecraft.world.entity.monster.Enemy;

public final class MusicMixer {

    private static final float MIN_FACTOR = 0.001F;
    private static final float DUCK_STEP = 0.05F;
    private static final double JUKEBOX_RANGE = 64;
    private static final double ENEMY_RANGE = 12;
    private static final long HURT_MEMORY_MS = 6000;

    private static final Map<SoundInstance, Long> STARTED = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Deque<ResourceLocation> RECENT = new ArrayDeque<>();
    private static volatile Set<ResourceLocation> recentSet = Set.of();

    private static float duck = 1.0F;
    private static boolean jukeboxNear;
    private static boolean enemyNear;
    private static long lastHurt = -HURT_MEMORY_MS;
    private static int ticks;

    private MusicMixer() {}

    private static boolean applies(SoundInstance sound) {
        return sound.getSource() == SoundSource.MUSIC && !(sound instanceof MusicPreview.PreviewSound);
    }

    public static float factor(SoundInstance sound) {
        if (!applies(sound)) return 1.0F;
        float f = duck * fade(sound);
        return f >= 1.0F ? 1.0F : Math.max(MIN_FACTOR, f);
    }

    private static float fade(SoundInstance sound) {
        int seconds = ModOptions.fadeSeconds;
        if (seconds <= 0) return 1.0F;
        long now = Util.getMillis();
        Long start = STARTED.putIfAbsent(sound, now);
        long elapsed = start == null ? 0 : now - start;
        long fadeMs = seconds * 1000L;
        float in = Math.min(1.0F, elapsed / (float) fadeMs);
        float out = 1.0F;
        Sound resolved = resolved(sound);
        int length = resolved == null ? -1 : Playlist.durationSeconds(resolved);
        if (length > seconds * 2) {
            long remaining = length * 1000L - elapsed;
            out = Math.max(0.0F, Math.min(1.0F, remaining / (float) fadeMs));
        }
        return in * in * out * out;
    }

    private static Sound resolved(SoundInstance sound) {
        try {
            return sound.getSound();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static void clientTick(Minecraft mc) {
        ticks++;
        SoundEngine engine = ((com.quietplease.mixin.SoundManagerAccessor) mc.getSoundManager()).quietplease$getSoundEngine();
        Map<SoundInstance, ChannelAccess.ChannelHandle> channels =
                ((com.quietplease.mixin.SoundEngineAccessor) engine).quietplease$getInstanceToChannel();

        boolean jukebox = false;
        boolean fading = false;
        LocalPlayer player = mc.player;
        synchronized (STARTED) {
            STARTED.keySet().retainAll(channels.keySet());
        }
        for (SoundInstance instance : channels.keySet()) {
            if (instance instanceof MusicPreview.PreviewSound) continue;
            if (instance.getSource() == SoundSource.RECORDS && player != null && !instance.isRelative()
                    && player.distanceToSqr(instance.getX(), instance.getY(), instance.getZ()) <= JUKEBOX_RANGE * JUKEBOX_RANGE) {
                jukebox = true;
            }
            if (!applies(instance)) continue;
            if (!STARTED.containsKey(instance)) {
                STARTED.put(instance, Util.getMillis());
                remember(resolved(instance));
            }
            if (ModOptions.fadeSeconds > 0 && fade(instance) < 1.0F) fading = true;
        }
        jukeboxNear = jukebox;

        if (player != null) {
            if (player.hurtTime > 0) lastHurt = Util.getMillis();
            if (ticks % 10 == 0) {
                enemyNear = ModOptions.combatVolume < 100 && !mc.level.getEntities(player,
                        player.getBoundingBox().inflate(ENEMY_RANGE), e -> e instanceof Enemy && e.isAlive()).isEmpty();
            }
        } else {
            enemyNear = false;
        }

        float target = targetDuck(mc);
        boolean changed = false;
        if (duck != target) {
            duck = duck < target ? Math.min(target, duck + DUCK_STEP) : Math.max(target, duck - DUCK_STEP);
            changed = true;
        }
        if (changed || fading) MusicSettings.applyToPlayingSounds();
    }

    private static float targetDuck(Minecraft mc) {
        float t = 1.0F;
        if (jukeboxNear) t *= ModOptions.jukeboxVolume / 100.0F;
        if (!mc.isWindowActive()) t *= ModOptions.unfocusedVolume / 100.0F;
        if (inCombat()) t *= ModOptions.combatVolume / 100.0F;
        return t;
    }

    public static boolean inCombat() {
        if (ModOptions.combatVolume >= 100) return false;
        return enemyNear || Util.getMillis() - lastHurt < HURT_MEMORY_MS;
    }

    public static boolean jukeboxNear() {
        return jukeboxNear;
    }

    public static float duck() {
        return duck;
    }

    private static void remember(Sound sound) {
        if (sound == null) return;
        ResourceLocation file = sound.getLocation();
        if (file.equals(SoundManager.EMPTY_SOUND.getLocation()) || file.equals(SoundManager.INTENTIONALLY_EMPTY_SOUND_LOCATION)) return;
        remember(file);
    }

    private static void refreshRecent() {
        int keep = ModOptions.noRepeat;
        Set<ResourceLocation> set = new HashSet<>();
        if (keep > 0) {
            int skip = Math.max(0, RECENT.size() - keep);
            for (ResourceLocation id : RECENT) {
                if (skip-- > 0) continue;
                set.add(id);
            }
        }
        recentSet = set.isEmpty() ? Set.of() : set;
    }

    public static void optionsChanged() {
        synchronized (RECENT) {
            refreshRecent();
        }
    }

    public static boolean isRecent(ResourceLocation file) {
        return recentSet.contains(file);
    }

    public static boolean affectsSelection() {
        return !recentSet.isEmpty() || (ModOptions.favoriteWeight > 1 && Playlist.hasFavorites());
    }

    public static int weight(ResourceLocation file, int base) {
        return ModOptions.favoriteWeight > 1 && Playlist.isFavorite(file) ? base * ModOptions.favoriteWeight : base;
    }

    public static void remember(ResourceLocation file) {
        synchronized (RECENT) {
            RECENT.remove(file);
            RECENT.addLast(file);
            while (RECENT.size() > 10) RECENT.removeFirst();
            refreshRecent();
        }
    }

    public static void clearRecentForTest() {
        synchronized (RECENT) {
            RECENT.clear();
            refreshRecent();
        }
    }
}
