package com.quietplease.mixin;

import javax.annotation.Nullable;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** MusicManager'in gizli alanlarina erisim (Atla ve "simdi cal" icin). */
@Mixin(MusicManager.class)
public interface MusicManagerAccessor {

    @Accessor("nextSongDelay")
    int quietplease$getNextSongDelay();

    @Accessor("nextSongDelay")
    void quietplease$setNextSongDelay(int ticks);

    @Nullable
    @Accessor("currentMusic")
    SoundInstance quietplease$getCurrentMusic();
}
