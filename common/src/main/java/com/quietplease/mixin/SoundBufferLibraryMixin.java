package com.quietplease.mixin;

import com.quietplease.music.CustomMusic;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ses motoru dosyalari oyunun kaynak yoneticisinden degil, yuklemede toplanan bir haritadan okur
 * (ResourceProvider.fromMap). Kendi muzik klasorundeki dosyalar o haritada yoktur; saglayiciyi sarip onlari da ekler.
 */
@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {

    @Shadow
    @Final
    @Mutable
    private ResourceProvider resourceManager;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void quietplease$customMusic(ResourceProvider provider, CallbackInfo ci) {
        this.resourceManager = CustomMusic.provider(this.resourceManager);
    }

    /** Kendi muzik klasorundeki MP3'ler: oyunun OGG cozucusu yerine modun MP3 akisi. */
    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    private void quietplease$mp3(Identifier path, boolean looping, CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        CompletableFuture<AudioStream> mp3 = CustomMusic.mp3Stream(path);
        if (mp3 != null) cir.setReturnValue(mp3);
    }
}
