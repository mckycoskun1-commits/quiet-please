package com.quietplease.mixin;

import com.quietplease.music.CustomMusic;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Kendi muzik klasorundeki MP3'ler: oyunun OGG cozucusu yerine modun MP3 akisi. OGG dosyalari bu surumde ses motoruna
 * oyunun kaynak yoneticisinden (ReloadableResourceManagerMixin) gelir.
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {

    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    private void quietplease$mp3(ResourceLocation path, boolean looping, CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        CompletableFuture<AudioStream> mp3 = CustomMusic.mp3Stream(path);
        if (mp3 != null) cir.setReturnValue(mp3);
    }
}
