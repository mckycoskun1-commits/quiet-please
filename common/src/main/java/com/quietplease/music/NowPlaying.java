package com.quietplease.music;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;

/**
 * Oyunda su an calan (ya da oyun duraklatildigi icin bekleyen) muzik parcalarini bulur.
 * Onizleme sesleri dahil edilmez.
 */
public final class NowPlaying {

    private NowPlaying() {}

    /** @param tracksById panelin bildigi parcalar (dosya kimligi -> parca) */
    public static List<MusicTrack> find(Map<Identifier, MusicTrack> tracksById) {
        List<MusicTrack> playing = new ArrayList<>();
        SoundEngine engine = ((com.quietplease.mixin.SoundManagerAccessor) Minecraft.getInstance().getSoundManager()).quietplease$getSoundEngine();
        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : ((com.quietplease.mixin.SoundEngineAccessor) engine).quietplease$getInstanceToChannel().entrySet()) {
            SoundInstance instance = entry.getKey();
            if (instance instanceof MusicPreview.PreviewSound || entry.getValue().isStopped()) continue;
            Sound sound;
            try {
                sound = instance.getSound();
            } catch (RuntimeException e) {
                continue; // bazi modlarin ozel ses siniflari
            }
            if (sound == null) continue;
            MusicTrack track = tracksById.get(sound.getLocation());
            if (track != null && !playing.contains(track)) playing.add(track);
        }
        return playing;
    }
}
